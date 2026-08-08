package com.granttrack.review.service.impl;

import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.review.dto.request.PanelDecisionRequest;
import com.granttrack.review.dto.request.ReviewScoreRequest;
import com.granttrack.review.dto.request.ReviewerAssignmentRequest;
import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.entity.AssignmentStatus;
import com.granttrack.review.entity.AwardDecision;
import com.granttrack.review.entity.ConflictScreeningStatus;
import com.granttrack.review.entity.OverallRecommendation;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.entity.ReviewCriterion;
import com.granttrack.review.entity.ReviewScore;
import com.granttrack.review.entity.ReviewerAssignment;
import com.granttrack.review.mapper.ReviewMapper;
import com.granttrack.review.repository.PanelDecisionRepository;
import com.granttrack.review.repository.ReviewScoreRepository;
import com.granttrack.review.repository.ReviewerAssignmentRepository;
import com.granttrack.review.service.ReviewService;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.repository.GrantCallRepository;
import jakarta.persistence.criteria.Predicate;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewerAssignmentRepository assignmentRepository;
    private final ReviewScoreRepository scoreRepository;
    private final PanelDecisionRepository panelDecisionRepository;
    private final GrantApplicationRepository applicationRepository;
    private final GrantCallRepository callRepository;
    private final UserRepository userRepository;
    private final ReviewMapper mapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReviewerAssignmentResponse assignReviewer(ReviewerAssignmentRequest request) {
        GrantApplication application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", request.applicationId()));
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException("Reviewers can only be assigned to a SUBMITTED or UNDER_REVIEW application (current: "
                    + application.getStatus() + ")");
        }
        if (assignmentRepository.existsByApplicationIdAndReviewerId(request.applicationId(), request.reviewerId())) {
            throw new DuplicateResourceException(
                    "Reviewer %s is already assigned to application %s"
                            .formatted(request.reviewerId(), request.applicationId()));
        }
        ReviewerAssignment assignment = ReviewerAssignment.builder()
                .applicationId(request.applicationId())
                .reviewerId(request.reviewerId())
                .assignedDate(LocalDate.now())
                .reviewDeadline(request.reviewDeadline())
                .conflictScreeningStatus(ConflictScreeningStatus.CLEAR)
                .status(AssignmentStatus.ASSIGNED)
                .build();
        log.info("Assigning reviewer {} to application {}", request.reviewerId(), request.applicationId());
        
        notificationService.notify(request.reviewerId(), 
            "You have been assigned to review application " + request.applicationId() + ". Deadline: " + request.reviewDeadline(), 
            NotificationCategory.REVIEW);
            
        return mapper.toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public ReviewerAssignmentResponse recordConflictCheck(Long assignmentId, String status) {
        ReviewerAssignment assignment = findAssignment(assignmentId);
        assertAssignmentOwner(assignment);
        assignment.setConflictScreeningStatus(parseConflictStatus(status));
        return mapper.toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public ReviewerAssignmentResponse respond(Long assignmentId, String decision, String reason) {
        ReviewerAssignment assignment = findAssignment(assignmentId);
        assertAssignmentOwner(assignment);
        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new BusinessException("Only an ASSIGNED assignment can be responded to (current: "
                    + assignment.getStatus() + ")");
        }
        switch (decision == null ? "" : decision.trim().toUpperCase()) {
            case "ACCEPT" -> assignment.setStatus(AssignmentStatus.ACCEPTED);
            case "DECLINE" -> {
                // Declining requires a reason so the Grant Admin can reassign with context.
                if (!StringUtils.hasText(reason)) {
                    throw new BusinessException("A reason is required to decline a review assignment");
                }
                assignment.setStatus(AssignmentStatus.DECLINED);
                assignment.setResponseComment(reason.trim());
            }
            default -> throw new BusinessException("Invalid response decision: " + decision);
        }
        ReviewerAssignmentResponse response = mapper.toResponse(assignmentRepository.save(assignment));
        log.info("Reviewer response on assignment {}: {}", assignmentId, assignment.getStatus());

        if (assignment.getStatus() == AssignmentStatus.DECLINED) {
            notifyGrantAdmins("A reviewer has DECLINED the review of application "
                    + assignment.getApplicationId() + ". Reason: " + reason.trim()
                    + ". Please assign another reviewer.");
        }
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "SUBMIT_REVIEW_SCORE", entityType = "ReviewScore")
    public ReviewScoreResponse submitScore(Long assignmentId, ReviewScoreRequest request) {
        ReviewerAssignment assignment = findAssignment(assignmentId);
        assertAssignmentOwner(assignment);
        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new BusinessException("Scores can only be submitted on an ACCEPTED assignment (current: "
                    + assignment.getStatus() + ")");
        }
        if (assignment.getConflictScreeningStatus() == ConflictScreeningStatus.COI_DECLARED) {
            throw new BusinessException("A reviewer who has declared a conflict of interest cannot score this application");
        }
        ReviewCriterion criterion = parseCriterion(request.criterion());
        if (scoreRepository.existsByAssignmentIdAndCriterion(assignmentId, criterion)) {
            throw new DuplicateResourceException(
                    "A score for criterion %s already exists on assignment %s".formatted(criterion, assignmentId));
        }
        ReviewScore score = ReviewScore.builder()
                .assignment(assignment)
                .criterion(criterion)
                .score(request.score())
                .comments(request.comments())
                .overallRecommendation(parseRecommendation(request.overallRecommendation()))
                .submittedDate(Instant.now())
                .build();
        log.info("Submitting score for criterion {} on assignment {}", criterion, assignmentId);
        return mapper.toResponse(scoreRepository.save(score));
    }

    @Override
    @Transactional
    public ReviewerAssignmentResponse submitAssignment(Long assignmentId) {
        ReviewerAssignment assignment = findAssignment(assignmentId);
        assertAssignmentOwner(assignment);
        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new BusinessException("Only an ACCEPTED assignment can be submitted (current: "
                    + assignment.getStatus() + ")");
        }
        assignment.setStatus(AssignmentStatus.SUBMITTED);
        log.info("Submitting assignment {}", assignmentId);
        return mapper.toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewerAssignmentResponse> searchAssignments(Long applicationId, Long reviewerId, String status,
                                                              Pageable pageable) {
        // A plain reviewer may only see their own assignments; Grant Admins / Admins may query any.
        final Long effectiveReviewerId;
        if (!SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")) {
            effectiveReviewerId = SecurityUtils.getCurrentUserId().orElse(-1L);
        } else {
            effectiveReviewerId = reviewerId;
        }
        Specification<ReviewerAssignment> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            if (effectiveReviewerId != null) {
                predicates.add(cb.equal(root.get("reviewerId"), effectiveReviewerId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseAssignmentStatus(status)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return assignmentRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewScoreResponse> getScores(Long assignmentId) {
        assertAssignmentOwner(findAssignment(assignmentId));
        return scoreRepository.findByAssignmentId(assignmentId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewScoreResponse> getApplicationReviews(Long applicationId) {
        // Grant Admin / Admin only (controller-gated): every score from the SUBMITTED reviews of this
        // application, so the panel can read completed reviews before recording the decision.
        List<ReviewScoreResponse> reviews = new ArrayList<>();
        for (ReviewerAssignment a : assignmentRepository.findByApplicationId(applicationId)) {
            if (a.getStatus() == AssignmentStatus.SUBMITTED) {
                scoreRepository.findByAssignmentId(a.getId())
                        .forEach(s -> reviews.add(mapper.toResponse(s)));
            }
        }
        return reviews;
    }

    @Override
    @Transactional
    @Auditable(action = "PANEL_DECISION", entityType = "PanelDecision")
    public PanelDecisionResponse createPanelDecision(Long applicationId, PanelDecisionRequest request) {
        if (panelDecisionRepository.existsByApplicationId(applicationId)) {
            throw new DuplicateResourceException("A panel decision already exists for application " + applicationId);
        }
        GrantApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", applicationId));
        // A panel decision may only be recorded once the application is under review and reviews are in.
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException("A panel decision requires the application to be UNDER_REVIEW (current: "
                    + app.getStatus() + ")");
        }
        List<ReviewerAssignment> assignments = assignmentRepository.findByApplicationId(applicationId);
        boolean anyReviewSubmitted = assignments.stream()
                .anyMatch(a -> a.getStatus() == AssignmentStatus.SUBMITTED);
        if (!anyReviewSubmitted) {
            throw new BusinessException("At least one review must be submitted before a panel decision can be recorded");
        }

        AwardDecision awardDecision = parseAwardDecision(request.awardDecision());
        boolean isAward = awardDecision == AwardDecision.FULL_AWARD || awardDecision == AwardDecision.REDUCED_AWARD;
        if (isAward && (request.awardedAmount() == null || request.awardedAmount().signum() <= 0)) {
            throw new BusinessException("A positive awarded amount is required for a " + awardDecision + " decision");
        }
        // An award must name the Finance Officer who will handle its disbursement.
        if (isAward) {
            if (request.financeOfficerId() == null) {
                throw new BusinessException("A finance officer must be assigned for an award decision");
            }
            assertFinanceOfficer(request.financeOfficerId());
        }

        // Consensus: use the panel-supplied value if given, otherwise the mean of all submitted scores.
        BigDecimal consensusScore = request.consensusScore() != null
                ? request.consensusScore()
                : computeConsensus(assignments);

        PanelDecision decision = PanelDecision.builder()
                .applicationId(applicationId)
                .panelDate(request.panelDate())
                .consensusScore(consensusScore)
                .awardDecision(awardDecision)
                .awardedAmount(request.awardedAmount())
                .conditionsAttached(request.conditionsAttached())
                .decidedById(SecurityUtils.getCurrentUserId().orElse(null))
                .financeOfficerId(isAward ? request.financeOfficerId() : null)
                .build();
        log.info("Recording panel decision for application {}", applicationId);
        PanelDecision saved = panelDecisionRepository.save(decision);

        if (isAward) {
            app.setStatus(ApplicationStatus.AWARDED);
            advanceCallToAwarded(app.getCallId());
        } else if (saved.getAwardDecision() == AwardDecision.REJECTED) {
            app.setStatus(ApplicationStatus.DECLINED);
        }
        applicationRepository.save(app);

        try {
            notificationService.notify(app.getPrincipalInvestigatorId(),
                "Your application " + app.getProjectTitle() + " has been " + saved.getAwardDecision() + ".",
                NotificationCategory.AWARD);
        } catch (Exception e) {
            log.warn("Failed to send panel decision notification", e);
        }

        // Hand off to the assigned finance officer for disbursement setup.
        if (isAward && saved.getFinanceOfficerId() != null) {
            try {
                notificationService.notify(saved.getFinanceOfficerId(),
                    "You have been assigned to handle disbursement for the awarded application \""
                        + app.getProjectTitle() + "\" (awarded amount " + saved.getAwardedAmount()
                        + "). Please review and set up milestones.",
                    NotificationCategory.DISBURSEMENT);
            } catch (Exception e) {
                log.warn("Failed to notify assigned finance officer", e);
            }
        }

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_PANEL_DECISION", entityType = "PanelDecision")
    public PanelDecisionResponse updatePanelDecision(Long applicationId, PanelDecisionRequest request) {
        PanelDecision decision = panelDecisionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("PanelDecision", applicationId));

        // The award outcome is final; only the supporting details may be edited.
        boolean isAward = decision.getAwardDecision() == AwardDecision.FULL_AWARD
                || decision.getAwardDecision() == AwardDecision.REDUCED_AWARD;
        if (isAward) {
            if (request.awardedAmount() == null || request.awardedAmount().signum() <= 0) {
                throw new BusinessException("A positive awarded amount is required for a "
                        + decision.getAwardDecision() + " decision");
            }
            if (request.financeOfficerId() == null) {
                throw new BusinessException("A finance officer must be assigned for an award decision");
            }
            assertFinanceOfficer(request.financeOfficerId());
        }

        Long previousFinanceOfficer = decision.getFinanceOfficerId();
        decision.setPanelDate(request.panelDate());
        if (request.consensusScore() != null) {
            decision.setConsensusScore(request.consensusScore());
        }
        decision.setAwardedAmount(request.awardedAmount());
        decision.setConditionsAttached(request.conditionsAttached());
        decision.setFinanceOfficerId(isAward ? request.financeOfficerId() : null);
        PanelDecision saved = panelDecisionRepository.save(decision);

        // Notify only when the finance officer actually changed.
        if (isAward && saved.getFinanceOfficerId() != null
                && !saved.getFinanceOfficerId().equals(previousFinanceOfficer)) {
            applicationRepository.findById(applicationId).ifPresent(app -> {
                try {
                    notificationService.notify(saved.getFinanceOfficerId(),
                        "You have been assigned to handle disbursement for application \""
                            + app.getProjectTitle() + "\".",
                        NotificationCategory.DISBURSEMENT);
                } catch (Exception e) {
                    log.warn("Failed to notify reassigned finance officer", e);
                }
            });
        }
        log.info("Updated panel decision for application {}", applicationId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PanelDecisionResponse getPanelDecision(Long applicationId) {
        // "No decision recorded yet" is a normal state (e.g. an application not yet before the panel),
        // not an error — return null so the UI can show an empty state instead of an error toast.
        PanelDecision decision = panelDecisionRepository.findByApplicationId(applicationId).orElse(null);
        if (decision == null) {
            return null;
        }
        // Full-pipeline staff (Grant Admin/Admin/Finance/Compliance) may read any decision;
        // a researcher may only read the panel decision for their own application.
        if (!SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN",
                "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER")) {
            GrantApplication app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", applicationId));
            Long uid = SecurityUtils.getCurrentUserId().orElse(null);
            if (uid == null || !uid.equals(app.getPrincipalInvestigatorId())) {
                throw new AccessDeniedException("This panel decision does not belong to you");
            }
        }
        return mapper.toResponse(decision);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PanelDecisionResponse> searchPanelDecisions(Pageable pageable) {
        return panelDecisionRepository.findAll(pageable).map(mapper::toResponse);
    }

    /** Verify the id refers to an existing user who actually holds the Finance Officer role. */
    private void assertFinanceOfficer(Long financeOfficerId) {
        User fo = userRepository.findById(financeOfficerId)
                .orElseThrow(() -> new BusinessException("Assigned finance officer not found: " + financeOfficerId));
        boolean isFinanceOfficer = fo.getRoles().stream()
                .anyMatch(r -> r.getName().equals(RoleName.ROLE_FINANCE_OFFICER.name()));
        if (!isFinanceOfficer) {
            throw new BusinessException("The assigned user is not a Finance Officer");
        }
    }

    private ReviewerAssignment findAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewerAssignment", id));
    }

    /** Notify every ACTIVE Grant Admin / Admin (e.g. when a reviewer declines and reassignment is needed). */
    private void notifyGrantAdmins(String message) {
        Specification<User> spec = (root, cq, cb) -> {
            cq.distinct(true);
            return cb.and(
                    cb.equal(root.get("status"), UserStatus.ACTIVE),
                    root.join("roles").get("name").in(
                            RoleName.ROLE_GRANT_ADMIN.name(), RoleName.ROLE_ADMIN.name()));
        };
        for (User admin : userRepository.findAll(spec)) {
            try {
                notificationService.notify(admin.getId(), message, NotificationCategory.REVIEW);
            } catch (Exception e) {
                log.warn("Failed to notify grant admin {} of a review decline", admin.getId(), e);
            }
        }
    }

    /** Grant Admins / Admins may act on any assignment; a reviewer only on their own. */
    private void assertAssignmentOwner(ReviewerAssignment assignment) {
        if (SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(assignment.getReviewerId())) {
            throw new AccessDeniedException("This review assignment does not belong to you");
        }
    }

    /** Mean of every submitted criterion score across all of an application's assignments (0 if none). */
    private BigDecimal computeConsensus(List<ReviewerAssignment> assignments) {
        List<Integer> scores = assignments.stream()
                .flatMap(a -> scoreRepository.findByAssignmentId(a.getId()).stream())
                .map(ReviewScore::getScore)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int sum = scores.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    /** Advances the parent call to AWARDED once an award is issued (only from OPEN / UNDER_REVIEW). */
    private void advanceCallToAwarded(Long callId) {
        if (callId == null) {
            return;
        }
        callRepository.findById(callId).ifPresent(call -> {
            if (call.getStatus() == CallStatus.OPEN || call.getStatus() == CallStatus.UNDER_REVIEW) {
                call.setStatus(CallStatus.AWARDED);
                callRepository.save(call);
            }
        });
    }

    private ConflictScreeningStatus parseConflictStatus(String raw) {
        try {
            return ConflictScreeningStatus.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid conflict screening status: " + raw);
        }
    }

    private AssignmentStatus parseAssignmentStatus(String raw) {
        try {
            return AssignmentStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid assignment status: " + raw);
        }
    }

    private ReviewCriterion parseCriterion(String raw) {
        try {
            return ReviewCriterion.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid review criterion: " + raw);
        }
    }

    private OverallRecommendation parseRecommendation(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return OverallRecommendation.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid overall recommendation: " + raw);
        }
    }

    private AwardDecision parseAwardDecision(String raw) {
        try {
            return AwardDecision.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid award decision: " + raw);
        }
    }
}
