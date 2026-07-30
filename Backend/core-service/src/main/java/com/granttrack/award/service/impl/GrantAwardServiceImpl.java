package com.granttrack.award.service.impl;

import com.granttrack.award.dto.request.AwardTermsRequest;
import com.granttrack.award.dto.request.GrantAwardRequest;
import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.FinanceReviewStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.award.mapper.AwardMapper;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.award.service.GrantAwardService;
import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.review.entity.AwardDecision;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.repository.PanelDecisionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrantAwardServiceImpl implements GrantAwardService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final GrantAwardRepository awardRepository;
    private final AwardMapper mapper;
    private final NotificationService notificationService;
    private final GrantApplicationRepository applicationRepository;
    private final PanelDecisionRepository panelDecisionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Auditable(action = "CREATE_AWARD", entityType = "GrantAward")
    public GrantAwardResponse create(GrantAwardRequest request) {
        if (awardRepository.existsByApplicationId(request.applicationId())) {
            throw new DuplicateResourceException("Award already exists for application " + request.applicationId());
        }
        // An award may only be issued from a favourable panel decision, for an AWARDED application,
        // and for no more than the panel-approved amount.
        GrantApplication application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", request.applicationId()));
        if (application.getStatus() != ApplicationStatus.AWARDED) {
            throw new BusinessException("An award can only be created for an AWARDED application (current: "
                    + application.getStatus() + ")");
        }
        PanelDecision panel = panelDecisionRepository.findByApplicationId(request.applicationId())
                .orElseThrow(() -> new BusinessException("No panel decision exists for application " + request.applicationId()));
        if (panel.getAwardDecision() != AwardDecision.FULL_AWARD && panel.getAwardDecision() != AwardDecision.REDUCED_AWARD) {
            throw new BusinessException("The panel decision (" + panel.getAwardDecision() + ") is not a favourable award");
        }
        if (panel.getAwardedAmount() != null
                && request.awardedAmount().compareTo(panel.getAwardedAmount()) > 0) {
            throw new BusinessException("Awarded amount cannot exceed the panel-approved amount ("
                    + panel.getAwardedAmount() + ")");
        }
        // Carry the finance officer assigned on the panel decision onto the award. If one is assigned,
        // they must accept the award before milestones can be set up; otherwise there is no finance gate.
        Long financeOfficerId = panel.getFinanceOfficerId();
        GrantAward award = GrantAward.builder()
                .applicationId(request.applicationId())
                .awardedAmount(request.awardedAmount())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .conditionsRef(request.conditionsRef())
                .awardLetterDate(request.awardLetterDate())
                .status(AwardStatus.ACTIVE)
                .financeOfficerId(financeOfficerId)
                .financeReviewStatus(financeOfficerId != null
                        ? FinanceReviewStatus.PENDING : FinanceReviewStatus.ACCEPTED)
                .build();
        award = awardRepository.save(award);

        Long piId = applicationRepository.findById(request.applicationId())
            .map(GrantApplication::getPrincipalInvestigatorId).orElse(null);
        if (piId != null) {
            notificationService.notify(piId,
                "An award has been created for your application " + request.applicationId() + ". Amount: " + request.awardedAmount(),
                NotificationCategory.AWARD);
        }

        // Ask the assigned finance officer to review and accept the award for disbursement.
        if (financeOfficerId != null) {
            try {
                notificationService.notify(financeOfficerId,
                    "An award for application " + request.applicationId()
                        + " is ready for your finance review. Accept it to set up disbursement milestones.",
                    NotificationCategory.DISBURSEMENT);
            } catch (Exception e) {
                log.warn("Failed to notify finance officer {} of a new award", financeOfficerId, e);
            }
        }

        return mapper.toResponse(award);
    }

    @Override
    @Transactional
    @Auditable(action = "FINANCE_REVIEW_AWARD", entityType = "GrantAward")
    public GrantAwardResponse financeReview(Long id, String decision, String reason) {
        GrantAward award = find(id);
        // Any Finance Officer may action a pending finance review — finance staff are interchangeable
        // here, consistent with milestone setup and fund release (which are role-gated, not
        // assigned-officer-gated). The officer named on the panel decision is simply the one notified.
        if (award.getStatus() != AwardStatus.ACTIVE) {
            throw new BusinessException("Only an ACTIVE award can be finance-reviewed (current: " + award.getStatus() + ")");
        }
        if (award.getFinanceReviewStatus() != FinanceReviewStatus.PENDING) {
            throw new BusinessException("This award has already been finance-reviewed ("
                    + award.getFinanceReviewStatus() + ")");
        }
        switch (decision == null ? "" : decision.trim().toUpperCase()) {
            case "ACCEPT" -> {
                award.setFinanceReviewStatus(FinanceReviewStatus.ACCEPTED);
                award.setFinanceReviewComment(StringUtils.hasText(reason) ? reason.trim() : null);
                notifyGrantAdmins("Finance officer ACCEPTED the award for application "
                        + award.getApplicationId() + ". Disbursement milestones can now be set up.");
            }
            case "REJECT" -> {
                if (!StringUtils.hasText(reason)) {
                    throw new BusinessException("A reason is required to reject an award");
                }
                award.setFinanceReviewStatus(FinanceReviewStatus.REJECTED);
                award.setFinanceReviewComment(reason.trim());
                notifyGrantAdmins("Finance officer REJECTED the award for application "
                        + award.getApplicationId() + ". Reason: " + reason.trim());
            }
            default -> throw new BusinessException("Invalid finance review decision: " + decision);
        }
        return mapper.toResponse(awardRepository.save(award));
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_AWARD", entityType = "GrantAward")
    public GrantAwardResponse update(Long id, AwardTermsRequest request) {
        GrantAward award = find(id);
        if (award.getStatus() == AwardStatus.COMPLETED || award.getStatus() == AwardStatus.TERMINATED) {
            throw new BusinessException("Cannot edit a " + award.getStatus() + " award");
        }
        award.setAwardedAmount(request.awardedAmount());
        award.setStartDate(request.startDate());
        award.setEndDate(request.endDate());
        award.setConditionsRef(request.conditionsRef());
        return mapper.toResponse(awardRepository.save(award));
    }

    @Override
    @Transactional
    @Auditable(action = "APPROVE_AWARD", entityType = "GrantAward")
    public GrantAwardResponse approve(Long id) {
        GrantAward award = find(id);
        award.setAwardLetterDate(LocalDate.now());
        award.setStatus(AwardStatus.ACTIVE);
        return mapper.toResponse(awardRepository.save(award));
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_AWARD_STATUS", entityType = "GrantAward")
    public GrantAwardResponse changeStatus(Long id, String status) {
        GrantAward award = find(id);
        AwardStatus target = parseStatus(status);
        validateTransition(award.getStatus(), target);
        award.setStatus(target);
        
        Long piId = applicationRepository.findById(award.getApplicationId())
            .map(GrantApplication::getPrincipalInvestigatorId).orElse(null);
        if (piId != null) {
            notificationService.notify(piId, 
                "The status of your award for application " + award.getApplicationId() + " has changed to " + target.name() + ".", 
                NotificationCategory.AWARD);
        }
        
        return mapper.toResponse(awardRepository.save(award));
    }

    @Override
    @Transactional(readOnly = true)
    public GrantAwardResponse getById(Long id) {
        GrantAward award = find(id);
        assertCanRead(award);
        return mapper.toResponse(award);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GrantAwardResponse> search(String status, Long applicationId, Long financeOfficerId,
                                           String financeReviewStatus, Pageable pageable) {
        // Finance/compliance/grant-admin/admin see all awards; a researcher sees only their own.
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAppIds = staff ? null : ownedApplicationIds();
        Specification<GrantAward> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!staff) {
                if (ownedAppIds.isEmpty()) {
                    return cb.disjunction(); // researcher owns nothing -> no rows
                }
                predicates.add(root.get("applicationId").in(ownedAppIds));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            if (financeOfficerId != null) {
                predicates.add(cb.equal(root.get("financeOfficerId"), financeOfficerId));
            }
            if (StringUtils.hasText(financeReviewStatus)) {
                predicates.add(cb.equal(root.get("financeReviewStatus"), parseFinanceReviewStatus(financeReviewStatus)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return awardRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    /** Notify every ACTIVE Grant Admin / Admin (finance accept/reject outcomes). */
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
                notificationService.notify(admin.getId(), message, NotificationCategory.DISBURSEMENT);
            } catch (Exception e) {
                log.warn("Failed to notify grant admin {} of a finance review", admin.getId(), e);
            }
        }
    }

    private FinanceReviewStatus parseFinanceReviewStatus(String raw) {
        try {
            return FinanceReviewStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid finance review status: " + raw);
        }
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_AWARD", entityType = "GrantAward")
    public void delete(Long id) {
        GrantAward award = find(id);
        award.setDeleted(true);
        awardRepository.save(award);
    }

    private List<Long> ownedApplicationIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        return applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
    }

    /** Staff roles see any award; a researcher only awards for their own applications. */
    private void assertCanRead(GrantAward award) {
        if (SecurityUtils.hasAnyRole(STAFF_ROLES)) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Long piId = applicationRepository.findById(award.getApplicationId())
                .map(GrantApplication::getPrincipalInvestigatorId).orElse(null);
        if (currentUserId == null || !currentUserId.equals(piId)) {
            throw new AccessDeniedException("You do not have access to this award");
        }
    }

    private GrantAward find(Long id) {
        return awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", id));
    }

    private AwardStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("Award status is required");
        }
        try {
            return AwardStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid award status: " + raw);
        }
    }

    private void validateTransition(AwardStatus current, AwardStatus target) {
        if (current == target) {
            return;
        }
        boolean allowed = switch (current) {
            case ACTIVE -> target == AwardStatus.SUSPENDED
                    || target == AwardStatus.COMPLETED
                    || target == AwardStatus.TERMINATED;
            case SUSPENDED -> target == AwardStatus.ACTIVE;
            case COMPLETED, TERMINATED -> false;
        };
        if (!allowed) {
            throw new BusinessException("Illegal award status transition: " + current + " -> " + target);
        }
    }
}
