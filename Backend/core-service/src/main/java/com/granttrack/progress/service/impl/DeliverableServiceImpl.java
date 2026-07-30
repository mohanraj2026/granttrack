package com.granttrack.progress.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.progress.dto.request.DeliverableRequest;
import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.entity.Deliverable;
import com.granttrack.progress.entity.DeliverableStatus;
import com.granttrack.progress.entity.DeliverableType;
import com.granttrack.progress.mapper.ProgressMapper;
import com.granttrack.progress.repository.DeliverableRepository;
import com.granttrack.progress.service.DeliverableService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverableServiceImpl implements DeliverableService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final DeliverableRepository deliverableRepository;
    private final ProgressMapper mapper;
    private final GrantAwardRepository awardRepository;
    private final GrantApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final DocumentStorageService documentStorageService;

    @Override
    @Transactional
    public DeliverableResponse create(DeliverableRequest request) {
        // A deliverable may only be registered by the award's principal investigator, against an ACTIVE award.
        GrantAward award = awardRepository.findById(request.awardId())
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", request.awardId()));
        if (award.getStatus() != AwardStatus.ACTIVE) {
            throw new BusinessException("Deliverables can only be registered against an ACTIVE award (current: " + award.getStatus() + ")");
        }
        assertOwningPrincipalInvestigator(award);
        Deliverable deliverable = Deliverable.builder()
                .awardId(request.awardId())
                .title(request.title())
                .type(parseType(request.type()))
                .dueDate(request.dueDate())
                .status(DeliverableStatus.PENDING)
                .build();
        return mapper.toResponse(deliverableRepository.save(deliverable));
    }

    @Override
    @Transactional
    public DeliverableResponse upload(Long id, MultipartFile document) {
        Deliverable deliverable = find(id);
        assertOwningPrincipalInvestigator(awardOf(deliverable.getAwardId()));
        // A deliverable can be uploaded while PENDING, or re-uploaded after a reviewer has rejected it.
        if (deliverable.getStatus() != DeliverableStatus.PENDING && deliverable.getStatus() != DeliverableStatus.REJECTED) {
            throw new BusinessException("Only a PENDING or REJECTED deliverable can be uploaded (current: " + deliverable.getStatus() + ")");
        }
        if (document == null || document.isEmpty()) {
            throw new BusinessException("No deliverable document was provided");
        }
        deliverable.setFilePath(documentStorageService.storeDeliverable(id, document));
        deliverable.setFileName(StringUtils.cleanPath(document.getOriginalFilename() == null ? "deliverable" : document.getOriginalFilename()));
        deliverable.setSubmittedDate(LocalDate.now());
        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        return mapper.toResponse(deliverableRepository.save(deliverable));
    }

    @Override
    @Transactional
    @Auditable(action = "REVIEW_DELIVERABLE", entityType = "Deliverable")
    public DeliverableResponse review(Long id, String decision, String comment) {
        Deliverable deliverable = find(id);
        if (deliverable.getStatus() != DeliverableStatus.SUBMITTED) {
            throw new BusinessException("Only a SUBMITTED deliverable can be reviewed (current: " + deliverable.getStatus() + ")");
        }
        String outcome;
        switch (decision == null ? "" : decision.toUpperCase()) {
            case "ACCEPT" -> {
                deliverable.setStatus(DeliverableStatus.ACCEPTED);
                outcome = "accepted";
            }
            case "REJECT" -> {
                deliverable.setStatus(DeliverableStatus.REJECTED);
                outcome = "rejected";
            }
            default -> throw new BusinessException("Invalid decision: " + decision);
        }
        deliverable.setReviewComment(comment);
        DeliverableResponse response = mapper.toResponse(deliverableRepository.save(deliverable));
        notifyOwningPrincipalInvestigator(deliverable.getAwardId(),
                "Your deliverable \"" + deliverable.getTitle() + "\" has been " + outcome + "."
                        + (StringUtils.hasText(comment) ? " Reviewer comment: " + comment : ""));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliverableDocument downloadDocument(Long id) {
        Deliverable deliverable = find(id);
        assertCanReadAward(deliverable.getAwardId());
        if (deliverable.getFilePath() == null) {
            throw new ResourceNotFoundException("Deliverable document", id);
        }
        Resource resource = documentStorageService.load(deliverable.getFilePath());
        String filename = deliverable.getFileName() != null ? deliverable.getFileName() : "deliverable";
        return new DeliverableDocument(resource, filename);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliverableResponse getById(Long id) {
        Deliverable deliverable = find(id);
        assertCanReadAward(deliverable.getAwardId());
        return mapper.toResponse(deliverable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliverableResponse> search(Long awardId, String status, Pageable pageable) {
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<Deliverable> spec = (root, cq, cb) -> {
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
        return deliverableRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    private Deliverable find(Long id) {
        return deliverableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable", id));
    }

    private GrantAward awardOf(Long awardId) {
        return awardRepository.findById(awardId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", awardId));
    }

    private DeliverableType parseType(String raw) {
        try {
            return DeliverableType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid deliverable type: " + raw);
        }
    }

    private DeliverableStatus parseStatus(String raw) {
        try {
            return DeliverableStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid deliverable status: " + raw);
        }
    }

    /** Award ids belonging to the current researcher's applications (for read scoping). */
    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }

    /** Staff roles may read any award's deliverables; a researcher only their own. */
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
            throw new AccessDeniedException("You do not have access to this award's deliverables");
        }
    }

    private void notifyOwningPrincipalInvestigator(Long awardId, String message) {
        try {
            awardRepository.findById(awardId).ifPresent(award ->
                    applicationRepository.findById(award.getApplicationId()).ifPresent(app ->
                            notificationService.notify(app.getPrincipalInvestigatorId(), message, NotificationCategory.PROGRESS)));
        } catch (Exception e) {
            log.warn("Failed to send deliverable notification", e);
        }
    }
}
