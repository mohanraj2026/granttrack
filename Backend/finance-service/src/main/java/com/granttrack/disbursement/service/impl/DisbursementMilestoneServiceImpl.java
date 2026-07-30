package com.granttrack.disbursement.service.impl;

import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.DisbursementStatus;
import com.granttrack.disbursement.entity.FundDisbursement;
import com.granttrack.disbursement.entity.MilestoneStatus;
import com.granttrack.disbursement.mapper.DisbursementMapper;
import com.granttrack.disbursement.repository.DisbursementMilestoneRepository;
import com.granttrack.disbursement.repository.FundDisbursementRepository;
import com.granttrack.disbursement.service.DisbursementMilestoneService;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementMilestoneServiceImpl implements DisbursementMilestoneService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final DisbursementMilestoneRepository milestoneRepository;
    private final FundDisbursementRepository disbursementRepository;
    private final DisbursementMapper mapper;
    private final com.granttrack.notification.service.NotificationService notificationService;
    private final com.granttrack.award.repository.GrantAwardRepository awardRepository;
    private final com.granttrack.application.repository.GrantApplicationRepository applicationRepository;
    private final com.granttrack.disbursement.service.DocumentStorageService documentStorageService;

    @Override
    @Transactional
    public MilestoneResponse create(MilestoneRequest request) {
        GrantAward award = awardRepository.findById(request.awardId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", request.awardId()));
        if (award.getStatus() != AwardStatus.ACTIVE) {
            throw new BusinessException("Milestones can only be added to an ACTIVE award (current: " + award.getStatus() + ")");
        }
        // When a finance officer is assigned, they must accept the award before milestones can be set up.
        if (award.getFinanceOfficerId() != null
                && award.getFinanceReviewStatus() != com.granttrack.award.entity.FinanceReviewStatus.ACCEPTED) {
            throw new BusinessException(
                    "The assigned finance officer must accept this award before milestones can be created (finance review: "
                            + award.getFinanceReviewStatus() + ")");
        }
        if (milestoneRepository.existsByAwardIdAndMilestoneNumber(request.awardId(), request.milestoneNumber())) {
            throw new DuplicateResourceException(
                    "Milestone number %d already exists for award %d".formatted(request.milestoneNumber(), request.awardId()));
        }
        // The sum of milestone amounts must never exceed the award amount.
        BigDecimal existingTotal = milestoneRepository.sumAmountByAwardId(request.awardId());
        if (existingTotal.add(request.amount()).compareTo(award.getAwardedAmount()) > 0) {
            throw new BusinessException("Total milestone amount would exceed the award amount (" + award.getAwardedAmount() + ")");
        }
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(request.awardId())
                .milestoneNumber(request.milestoneNumber())
                .description(request.description())
                .dueDate(request.dueDate())
                .amount(request.amount())
                .evidenceRequired(request.evidenceRequired() != null ? request.evidenceRequired() : Boolean.TRUE)
                .status(MilestoneStatus.UPCOMING)
                .build();
        return mapper.toResponse(milestoneRepository.save(milestone));
    }

    @Override
    @Transactional
    public MilestoneResponse update(Long id, MilestoneUpdateRequest request) {
        DisbursementMilestone milestone = find(id);
        if (milestone.getStatus() != MilestoneStatus.UPCOMING) {
            throw new BusinessException("Only an UPCOMING milestone can be edited (current: " + milestone.getStatus() + ")");
        }
        // Re-check the award cap: total of the other milestones + this milestone's new amount.
        GrantAward award = awardRepository.findById(milestone.getAwardId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", milestone.getAwardId()));
        BigDecimal othersTotal = milestoneRepository.sumAmountByAwardId(milestone.getAwardId())
                .subtract(milestone.getAmount());
        if (othersTotal.add(request.amount()).compareTo(award.getAwardedAmount()) > 0) {
            throw new BusinessException("Total milestone amount would exceed the award amount (" + award.getAwardedAmount() + ")");
        }
        milestone.setDescription(request.description());
        milestone.setDueDate(request.dueDate());
        milestone.setAmount(request.amount());
        if (request.evidenceRequired() != null) {
            milestone.setEvidenceRequired(request.evidenceRequired());
        }
        return mapper.toResponse(milestoneRepository.save(milestone));
    }

    @Override
    @Transactional(readOnly = true)
    public MilestoneResponse getById(Long id) {
        DisbursementMilestone milestone = find(id);
        assertCanReadAward(milestone.getAwardId());
        return mapper.toResponse(milestone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MilestoneResponse> search(Long awardId, String status, Pageable pageable) {
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<DisbursementMilestone> spec = (root, cq, cb) -> {
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
        return milestoneRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public MilestoneResponse submitEvidence(Long id, String note, MultipartFile document) {
        DisbursementMilestone milestone = find(id);
        // Only the award's principal investigator may submit evidence for its milestones.
        assertOwningPrincipalInvestigator(milestone.getAwardId());
        if (milestone.getStatus() != MilestoneStatus.UPCOMING) {
            throw new BusinessException("Only an UPCOMING milestone can submit evidence (current: " + milestone.getStatus() + ")");
        }
        boolean hasDocument = document != null && !document.isEmpty();
        if (Boolean.TRUE.equals(milestone.getEvidenceRequired()) && !hasDocument) {
            throw new BusinessException("This milestone requires a supporting evidence document");
        }
        if (hasDocument) {
            milestone.setEvidenceDocPath(documentStorageService.storeMilestoneEvidence(id, document));
            milestone.setEvidenceDocName(cleanName(document.getOriginalFilename()));
        }
        milestone.setEvidenceNote(note);
        milestone.setEvidenceSubmittedDate(LocalDate.now());
        milestone.setEvidenceReviewComment(null); // clear any prior rejection reason on resubmission
        milestone.setStatus(MilestoneStatus.EVIDENCE_SUBMITTED);
        return mapper.toResponse(milestoneRepository.save(milestone));
    }

    @Override
    @Transactional
    @Auditable(action = "REJECT_MILESTONE_EVIDENCE", entityType = "DisbursementMilestone")
    public MilestoneResponse rejectEvidence(Long id, String reason) {
        DisbursementMilestone milestone = find(id);
        if (milestone.getStatus() != MilestoneStatus.EVIDENCE_SUBMITTED) {
            throw new BusinessException("Only EVIDENCE_SUBMITTED evidence can be returned (current: " + milestone.getStatus() + ")");
        }
        milestone.setStatus(MilestoneStatus.UPCOMING);
        milestone.setEvidenceReviewComment(reason);
        DisbursementMilestone saved = milestoneRepository.save(milestone);
        notifyPrincipalInvestigator(milestone,
                "Evidence for milestone " + milestone.getMilestoneNumber() + " was returned for resubmission."
                        + (StringUtils.hasText(reason) ? " Reason: " + reason : ""));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceDocument downloadEvidence(Long id) {
        DisbursementMilestone milestone = find(id);
        assertCanReadAward(milestone.getAwardId());
        if (milestone.getEvidenceDocPath() == null) {
            throw new ResourceNotFoundException("Evidence document", id);
        }
        Resource resource = documentStorageService.load(milestone.getEvidenceDocPath());
        String filename = milestone.getEvidenceDocName() != null ? milestone.getEvidenceDocName() : "evidence";
        return new EvidenceDocument(resource, filename);
    }

    @Override
    @Transactional
    @Auditable(action = "APPROVE_MILESTONE", entityType = "DisbursementMilestone")
    public MilestoneResponse approve(Long id) {
        DisbursementMilestone milestone = find(id);
        if (milestone.getStatus() != MilestoneStatus.EVIDENCE_SUBMITTED) {
            throw new BusinessException("Only an EVIDENCE_SUBMITTED milestone can be approved (current: " + milestone.getStatus() + ")");
        }
        milestone.setStatus(MilestoneStatus.APPROVED);
        DisbursementMilestone saved = milestoneRepository.save(milestone);
        try {
            awardRepository.findById(milestone.getAwardId()).ifPresent(award -> {
                applicationRepository.findById(award.getApplicationId()).ifPresent(app -> {
                    notificationService.notify(app.getPrincipalInvestigatorId(),
                        "Your milestone " + milestone.getMilestoneNumber() + " has been approved.",
                        NotificationCategory.DISBURSEMENT);
                });
            });
        } catch (Exception e) {
            log.warn("Failed to send milestone approval notification", e);
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "RELEASE_FUNDS", entityType = "FundDisbursement")
    public FundDisbursementResponse release(Long id, ReleaseFundsRequest request) {
        DisbursementMilestone milestone = find(id);
        if (milestone.getStatus() != MilestoneStatus.APPROVED) {
            throw new BusinessException("Only an APPROVED milestone can be released (current: " + milestone.getStatus() + ")");
        }
        FundDisbursement disbursement = FundDisbursement.builder()
                .milestone(milestone)
                .awardId(milestone.getAwardId())
                .amount(milestone.getAmount())
                .disbursedDate(request != null && request.releaseDate() != null ? request.releaseDate() : LocalDate.now())
                .receivingAccountRef(request != null ? request.receivingAccountRef() : null)
                .paymentReference(request != null ? request.paymentReference() : null)
                .status(DisbursementStatus.RELEASED)
                .build();
        FundDisbursement saved = disbursementRepository.save(disbursement);
        milestone.setStatus(MilestoneStatus.DISBURSED);
        milestoneRepository.save(milestone);
        try {
            awardRepository.findById(milestone.getAwardId()).ifPresent(award -> {
                applicationRepository.findById(award.getApplicationId()).ifPresent(app -> {
                    notificationService.notify(app.getPrincipalInvestigatorId(), 
                        "Funds for milestone " + milestone.getMilestoneNumber() + " have been disbursed. Amount: " + milestone.getAmount(), 
                        com.granttrack.notification.entity.NotificationCategory.DISBURSEMENT);
                });
            });
        } catch (Exception e) {
            log.warn("Failed to send disbursement notification", e);
        }
        return mapper.toResponse(saved);
    }

    private DisbursementMilestone find(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisbursementMilestone", id));
    }

    private String cleanName(String raw) {
        return raw == null ? null : StringUtils.cleanPath(raw);
    }

    /** Notify the award's principal investigator (best-effort; never breaks the operation). */
    private void notifyPrincipalInvestigator(DisbursementMilestone milestone, String message) {
        try {
            awardRepository.findById(milestone.getAwardId()).ifPresent(award ->
                    applicationRepository.findById(award.getApplicationId()).ifPresent(app ->
                            notificationService.notify(app.getPrincipalInvestigatorId(), message, NotificationCategory.DISBURSEMENT)));
        } catch (Exception e) {
            log.warn("Failed to send milestone notification", e);
        }
    }

    private MilestoneStatus parseStatus(String raw) {
        try {
            return MilestoneStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid milestone status: " + raw);
        }
    }

    /** Award ids belonging to the current researcher's applications (for read scoping). */
    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }

    /** Staff roles may read any award's milestones; a researcher only their own. */
    private void assertCanReadAward(Long awardId) {
        if (SecurityUtils.hasAnyRole(STAFF_ROLES)) {
            return;
        }
        assertOwningPrincipalInvestigator(awardId);
    }

    /** The current caller must be the principal investigator of the award's application. */
    private void assertOwningPrincipalInvestigator(Long awardId) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Long piId = awardRepository.findById(awardId)
                .flatMap(award -> applicationRepository.findById(award.getApplicationId()))
                .map(GrantApplication::getPrincipalInvestigatorId)
                .orElse(null);
        if (currentUserId == null || !currentUserId.equals(piId)) {
            throw new AccessDeniedException("You do not have access to this award's milestones");
        }
    }
}
