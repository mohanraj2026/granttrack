package com.granttrack.progress.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.progress.dto.request.ProgressReportRequest;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.entity.ProgressReport;
import com.granttrack.progress.entity.ProgressStatus;
import com.granttrack.progress.mapper.ProgressMapper;
import com.granttrack.progress.repository.ProgressReportRepository;
import com.granttrack.progress.service.ProgressReportService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressReportServiceImpl implements ProgressReportService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final ProgressReportRepository reportRepository;
    private final ProgressMapper mapper;
    private final GrantAwardRepository awardRepository;
    private final GrantApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final DocumentStorageService documentStorageService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ProgressReportResponse create(ProgressReportRequest request) {
        // A progress report may only be filed by the award's principal investigator, against an ACTIVE award.
        GrantAward award = awardRepository.findById(request.awardId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", request.awardId()));
        if (award.getStatus() != AwardStatus.ACTIVE) {
            throw new BusinessException("Progress reports can only be filed against an ACTIVE award (current: " + award.getStatus() + ")");
        }
        assertOwningPrincipalInvestigator(award);
        ProgressReport report = ProgressReport.builder()
                .awardId(request.awardId())
                .milestoneId(request.milestoneId())
                .period(request.period())
                .summary(request.summary())
                .keyAchievements(request.keyAchievements())
                .challenges(request.challenges())
                .budgetUtilisationPercent(request.budgetUtilisationPercent())
                .status(ProgressStatus.DRAFT)
                .build();
        return mapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public ProgressReportResponse update(Long id, ProgressReportRequest request) {
        ProgressReport report = find(id);
        assertOwningPrincipalInvestigator(awardOf(report.getAwardId()));
        // A report can be edited while it is a DRAFT, or after a reviewer has requested a revision.
        if (report.getStatus() != ProgressStatus.DRAFT && report.getStatus() != ProgressStatus.REVISION_REQUESTED) {
            throw new BusinessException("Only a DRAFT or REVISION_REQUESTED report can be edited (current: " + report.getStatus() + ")");
        }
        report.setAwardId(request.awardId());
        if (request.milestoneId() != null) {
            report.setMilestoneId(request.milestoneId());
        }
        report.setPeriod(request.period());
        report.setSummary(request.summary());
        report.setKeyAchievements(request.keyAchievements());
        report.setChallenges(request.challenges());
        report.setBudgetUtilisationPercent(request.budgetUtilisationPercent());
        return mapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public ProgressReportResponse submit(Long id) {
        ProgressReport report = find(id);
        assertOwningPrincipalInvestigator(awardOf(report.getAwardId()));
        // A fresh DRAFT or a revision-requested report can be (re)submitted for review.
        if (report.getStatus() != ProgressStatus.DRAFT && report.getStatus() != ProgressStatus.REVISION_REQUESTED) {
            throw new BusinessException("Only a DRAFT or REVISION_REQUESTED report can be submitted (current: " + report.getStatus() + ")");
        }
        report.setStatus(ProgressStatus.SUBMITTED);
        report.setSubmittedDate(Instant.now());
        report.setSubmittedById(SecurityUtils.getCurrentUserId().orElse(null));
        ProgressReport saved = reportRepository.save(report);
        // The Compliance Officer(s) review submitted reports — notify them there is one waiting.
        notifyComplianceOfficers("A progress report"
                + (report.getPeriod() != null ? " for " + report.getPeriod() : "")
                + " has been submitted and is awaiting your review.");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "REVIEW_PROGRESS", entityType = "ProgressReport")
    public ProgressReportResponse review(Long id, String decision, String comment) {
        ProgressReport report = find(id);
        if (report.getStatus() != ProgressStatus.SUBMITTED) {
            throw new BusinessException("Only a SUBMITTED report can be reviewed (current: " + report.getStatus() + ")");
        }
        String outcome;
        switch (decision == null ? "" : decision.toUpperCase()) {
            case "APPROVE" -> {
                report.setStatus(ProgressStatus.APPROVED);
                outcome = "approved";
            }
            case "REQUEST_REVISION" -> {
                report.setStatus(ProgressStatus.REVISION_REQUESTED);
                outcome = "sent back for revision";
            }
            default -> throw new BusinessException("Invalid decision: " + decision);
        }
        report.setReviewComment(comment);
        ProgressReport saved = reportRepository.save(report);
        String periodText = report.getPeriod() != null ? " for " + report.getPeriod() : "";
        String commentText = StringUtils.hasText(comment) ? " Reviewer comment: " + comment : "";
        // Notify the researcher (owning PI) of the compliance outcome.
        notifyOwningPrincipalInvestigator(report.getAwardId(),
                "Your progress report" + periodText + " has been " + outcome + "." + commentText);
        // Notify the assigned Finance Officer so they can verify the milestone and proceed to disbursement.
        notifyAssignedFinanceOfficer(report.getAwardId(),
                "The Compliance Officer has " + outcome + " a progress report" + periodText
                        + ". Please review the outcome and verify the milestone." + commentText);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProgressReportResponse uploadDocument(Long id, MultipartFile document) {
        ProgressReport report = find(id);
        assertOwningPrincipalInvestigator(awardOf(report.getAwardId()));
        if (report.getStatus() != ProgressStatus.DRAFT && report.getStatus() != ProgressStatus.REVISION_REQUESTED) {
            throw new BusinessException("A report document can only be attached to a DRAFT or REVISION_REQUESTED report (current: " + report.getStatus() + ")");
        }
        if (document == null || document.isEmpty()) {
            throw new BusinessException("No document was provided");
        }
        report.setReportDocPath(documentStorageService.storeProgressReport(id, document));
        report.setReportDocName(StringUtils.cleanPath(document.getOriginalFilename() == null ? "report" : document.getOriginalFilename()));
        return mapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDocument downloadDocument(Long id) {
        ProgressReport report = find(id);
        assertCanReadAward(report.getAwardId());
        if (report.getReportDocPath() == null) {
            throw new ResourceNotFoundException("Progress report document", id);
        }
        Resource resource = documentStorageService.load(report.getReportDocPath());
        String filename = report.getReportDocName() != null ? report.getReportDocName() : "progress-report";
        return new ReportDocument(resource, filename);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressReportResponse getById(Long id) {
        ProgressReport report = find(id);
        assertCanReadAward(report.getAwardId());
        return mapper.toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgressReportResponse> search(Long awardId, String status, Pageable pageable) {
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<ProgressReport> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!staff) {
                if (ownedAwardIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(root.get("awardId").in(ownedAwardIds));
            }
            if (awardId != null) {
                predicates.add(cb.equal(root.get("awardId"), awardId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return reportRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    private ProgressReport find(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgressReport", id));
    }

    private GrantAward awardOf(Long awardId) {
        return awardRepository.findById(awardId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", awardId));
    }

    private ProgressStatus parseStatus(String raw) {
        try {
            return ProgressStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid progress report status: " + raw);
        }
    }

    /** Award ids belonging to the current researcher's applications (for read scoping). */
    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }

    /** Staff roles may read any award's reports; a researcher only their own. */
    private void assertCanReadAward(Long awardId) {
        if (SecurityUtils.hasAnyRole(STAFF_ROLES)) {
            return;
        }
        assertOwningPrincipalInvestigator(awardOf(awardId));
    }

    /** The current caller must be the principal investigator of the award's application. */
    private void assertOwningPrincipalInvestigator(GrantAward award) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Long piId = applicationRepository.findById(award.getApplicationId())
                .map(GrantApplication::getPrincipalInvestigatorId)
                .orElse(null);
        if (currentUserId == null || !currentUserId.equals(piId)) {
            throw new AccessDeniedException("You do not have access to this award's progress reports");
        }
    }

    private void notifyOwningPrincipalInvestigator(Long awardId, String message) {
        try {
            awardRepository.findById(awardId).ifPresent(award ->
                    applicationRepository.findById(award.getApplicationId()).ifPresent(app ->
                            notificationService.notify(app.getPrincipalInvestigatorId(), message, NotificationCategory.PROGRESS)));
        } catch (Exception e) {
            log.warn("Failed to send progress report notification", e);
        }
    }

    /** Notify the finance officer assigned to the award (best-effort; never breaks the review). */
    private void notifyAssignedFinanceOfficer(Long awardId, String message) {
        try {
            awardRepository.findById(awardId).ifPresent(award -> {
                Long financeOfficerId = award.getFinanceOfficerId();
                if (financeOfficerId != null) {
                    notificationService.notify(financeOfficerId, message, NotificationCategory.PROGRESS);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to notify finance officer of a progress report review", e);
        }
    }

    /** Notify every ACTIVE Compliance Officer that a progress report is awaiting review (best-effort). */
    private void notifyComplianceOfficers(String message) {
        try {
            Specification<User> spec = (root, cq, cb) -> {
                cq.distinct(true);
                return cb.and(
                        cb.equal(root.get("status"), UserStatus.ACTIVE),
                        root.join("roles").get("name").in(RoleName.ROLE_COMPLIANCE_OFFICER.name()));
            };
            for (User officer : userRepository.findAll(spec)) {
                notificationService.notify(officer.getId(), message, NotificationCategory.PROGRESS);
            }
        } catch (Exception e) {
            log.warn("Failed to notify compliance officers of a submitted progress report", e);
        }
    }
}
