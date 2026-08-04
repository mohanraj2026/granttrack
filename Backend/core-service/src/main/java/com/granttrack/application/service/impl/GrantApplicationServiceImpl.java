package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.GrantApplicationRequest;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.dto.response.BlindApplicationResponse;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.application.service.GrantApplicationService;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.repository.GrantCallRepository;
import com.granttrack.review.repository.ReviewerAssignmentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrantApplicationServiceImpl implements GrantApplicationService {

    private final GrantApplicationRepository applicationRepository;
    private final GrantCallRepository callRepository;
    private final ReviewerAssignmentRepository assignmentRepository;
    private final ApplicationMapper mapper;
    private final DocumentStorageService documentStorage;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public GrantApplicationResponse create(GrantApplicationRequest request) {
        // The principal investigator is ALWAYS the authenticated caller; a client-supplied
        // principalInvestigatorId is ignored so an application cannot be created on another user's behalf.
        Long principalInvestigatorId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException("Unable to resolve current user as principal investigator"));
        // Applications may only be created against a call that is OPEN for submissions.
        requireOpenCall(request.callId());
        GrantApplication application = GrantApplication.builder()
                .callId(request.callId())
                .principalInvestigatorId(principalInvestigatorId)
                .projectTitle(request.projectTitle())
                .researchAbstract(request.researchAbstract())
                .discipline(request.discipline())
                .requestedAmount(request.requestedAmount())
                .projectDurationMonths(request.projectDurationMonths())
                .institutionId(request.institutionId())
                .status(ApplicationStatus.DRAFT)
                .build();
        GrantApplication saved = applicationRepository.save(application);
        log.info("Created grant application {} for call {}", saved.getId(), saved.getCallId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GrantApplicationResponse update(Long id, GrantApplicationRequest request) {
        GrantApplication application = find(id);
        assertOwnerOrAdmin(application);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("Only a DRAFT application can be edited (current: " + application.getStatus() + ")");
        }
        application.setCallId(request.callId());
        // The principal investigator is immutable after creation.
        application.setProjectTitle(request.projectTitle());
        application.setResearchAbstract(request.researchAbstract());
        application.setDiscipline(request.discipline());
        application.setRequestedAmount(request.requestedAmount());
        application.setProjectDurationMonths(request.projectDurationMonths());
        application.setInstitutionId(request.institutionId());
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public GrantApplicationResponse getById(Long id) {
        GrantApplication application = find(id);
        assertCanRead(application);
        return mapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public BlindApplicationResponse getBlindById(Long id) {
        GrantApplication application = find(id);
        // Grant Admins / Admins may view any blind application; a reviewer only those assigned to them.
        if (!SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")) {
            Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            if (currentUserId == null || !assignmentRepository.existsByApplicationIdAndReviewerId(id, currentUserId)) {
                throw new AccessDeniedException("You are not assigned to review this application");
            }
        }
        return BlindApplicationResponse.builder()
                .id(application.getId())
                .projectTitle(application.getProjectTitle())
                .researchAbstract(application.getResearchAbstract())
                .discipline(application.getDiscipline())
                .requestedAmount(application.getRequestedAmount())
                .projectDurationMonths(application.getProjectDurationMonths())
                .abstractDocName(application.getAbstractDocName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GrantApplicationResponse> search(String q, String status, Long callId, Pageable pageable) {
        // Researchers may only see their own applications; Grant Admins / Admins see the full pipeline.
        boolean privileged = SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN");
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Specification<GrantApplication> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!privileged) {
                // Non-privileged callers are scoped to applications they own (as PI).
                predicates.add(cb.equal(root.get("principalInvestigatorId"),
                        currentUserId == null ? -1L : currentUserId));
            }
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("projectTitle")), "%" + q.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (callId != null) {
                predicates.add(cb.equal(root.get("callId"), callId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return applicationRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    @Auditable(action = "SUBMIT_APPLICATION", entityType = "GrantApplication")
    public GrantApplicationResponse submit(Long id) {
        GrantApplication application = find(id);
        assertOwnerOrAdmin(application);
        validateTransition(application.getStatus(), ApplicationStatus.SUBMITTED);
        // An application may only be submitted while its call is OPEN and inside the submission window.
        GrantCall call = requireOpenCall(application.getCallId());
        LocalDate today = LocalDate.now();
        if (today.isBefore(call.getOpenDate()) || today.isAfter(call.getCloseDate())) {
            throw new BusinessException("The submission window for this call is closed");
        }
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmissionDate(Instant.now());
        log.info("Submitted grant application {}", id);
        
        notificationService.notify(
                application.getPrincipalInvestigatorId(),
                "Your application " + application.getProjectTitle() + " status is now " + ApplicationStatus.SUBMITTED.name(),
                NotificationCategory.APPLICATION
        ); return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public GrantApplicationResponse withdraw(Long id) {
        GrantApplication application = find(id);
        assertOwnerOrAdmin(application);
        validateTransition(application.getStatus(), ApplicationStatus.WITHDRAWN);
        application.setStatus(ApplicationStatus.WITHDRAWN);
        log.info("Withdrew grant application {}", id);
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_APPLICATION_STATUS", entityType = "GrantApplication")
    public GrantApplicationResponse changeStatus(Long id, String status) {
        GrantApplication application = find(id);
        ApplicationStatus target = parseStatus(status);
        validateTransition(application.getStatus(), target);
        application.setStatus(target);
        // When the first application enters review, advance its call OPEN -> UNDER_REVIEW.
        if (target == ApplicationStatus.UNDER_REVIEW) {
            advanceCallToUnderReview(application.getCallId());
        }
        log.info("Changed grant application {} status to {}", id, target);
        
        notificationService.notify(application.getPrincipalInvestigatorId(), 
            "The status of your application '" + application.getProjectTitle() + "' has changed to " + target.name() + ".", 
            NotificationCategory.APPLICATION);
            
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public GrantApplicationResponse uploadAbstract(Long id, MultipartFile file) {
        GrantApplication application = find(id);
        assertOwnerOrAdmin(application);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("Documents can only be uploaded while the application is a DRAFT");
        }
        String storedPath = documentStorage.storeAbstract(id, file);
        application.setAbstractDocPath(storedPath);
        application.setAbstractDocName(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "abstract" : file.getOriginalFilename()));
        log.info("Attached abstract document to application {}", id);
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractDocument downloadAbstract(Long id) {
        GrantApplication application = find(id);
        assertCanReadAbstractDocument(application);
        if (!StringUtils.hasText(application.getAbstractDocPath())) {
            throw new ResourceNotFoundException("No abstract document uploaded for application " + id);
        }
        Resource resource = documentStorage.load(application.getAbstractDocPath());
        String name = StringUtils.hasText(application.getAbstractDocName())
                ? application.getAbstractDocName() : resource.getFilename();
        return new AbstractDocument(resource, name);
    }

    private GrantApplication find(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", id));
    }

    /** Grant Admins / Admins have full access; otherwise the caller must be the application's PI. */
    private void assertOwnerOrAdmin(GrantApplication application) {
        if (SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(application.getPrincipalInvestigatorId())) {
            throw new AccessDeniedException("You do not have access to this application");
        }
    }

    /**
     * Read access: the owning PI, or any full-pipeline staff role. Finance and Compliance officers are
     * trusted downstream staff (they already see all awards/disbursements/progress) and legitimately need
     * to read an application's details and documents — e.g. a Finance Officer reviewing an award to
     * accept/reject it. Blind reviewers use the separate {@code /blind} projection, not this path.
     */
    private void assertCanRead(GrantApplication application) {
        if (SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN",
                "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(application.getPrincipalInvestigatorId())) {
            throw new AccessDeniedException("You do not have access to this application");
        }
    }

    /**
     * Read access to the abstract <em>document</em>. Same as {@link #assertCanRead} (staff + owning PI),
     * but additionally allows a reviewer who is assigned to this application — a blind reviewer needs the
     * abstract document to score it. Access stays assignment-scoped (identical to the {@code /blind}
     * projection), so a reviewer can only download documents for applications actually assigned to them.
     */
    private void assertCanReadAbstractDocument(GrantApplication application) {
        if (SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN",
                "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId != null) {
            if (currentUserId.equals(application.getPrincipalInvestigatorId())) {
                return; // the owning principal investigator
            }
            if (assignmentRepository.existsByApplicationIdAndReviewerId(application.getId(), currentUserId)) {
                return; // a reviewer assigned to this application (blind review)
            }
        }
        throw new AccessDeniedException("You do not have access to this application's document");
    }

    /** Loads a call and ensures it exists and is OPEN for submissions; returns it for further checks. */
    private GrantCall requireOpenCall(Long callId) {
        GrantCall call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantCall", callId));
        if (call.getStatus() != CallStatus.OPEN) {
            throw new BusinessException("Grant call is not open for submissions (status: " + call.getStatus() + ")");
        }
        return call;
    }

    /** Advances the parent call OPEN -> UNDER_REVIEW when its applications move into review. */
    private void advanceCallToUnderReview(Long callId) {
        if (callId == null) {
            return;
        }
        callRepository.findById(callId).ifPresent(call -> {
            if (call.getStatus() == CallStatus.OPEN) {
                call.setStatus(CallStatus.UNDER_REVIEW);
                callRepository.save(call);
            }
        });
    }

    private ApplicationStatus parseStatus(String raw) {
        try {
            return ApplicationStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid application status: " + raw);
        }
    }

    /** Enforces the application state machine. */
    private void validateTransition(ApplicationStatus current, ApplicationStatus target) {
        boolean allowed = switch (target) {
            case SUBMITTED -> current == ApplicationStatus.DRAFT;
            case WITHDRAWN -> current == ApplicationStatus.DRAFT || current == ApplicationStatus.SUBMITTED;
            case UNDER_REVIEW -> current == ApplicationStatus.SUBMITTED;
            case AWARDED, DECLINED -> current == ApplicationStatus.UNDER_REVIEW;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException("Illegal transition from " + current + " to " + target);
        }
    }
}
