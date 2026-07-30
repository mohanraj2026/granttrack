package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.CoInvestigatorRequest;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.entity.CoInvestigator;
import com.granttrack.application.entity.CoInvestigatorRole;
import com.granttrack.application.entity.CoInvestigatorStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.CoInvestigatorRepository;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.service.CoInvestigatorService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoInvestigatorServiceImpl implements CoInvestigatorService {

    private final CoInvestigatorRepository coInvestigatorRepository;
    private final GrantApplicationRepository applicationRepository;
    private final ApplicationMapper mapper;

    @Override
    @Transactional
    public CoInvestigatorResponse add(Long applicationId, CoInvestigatorRequest request) {
        GrantApplication application = findApplication(applicationId);
        assertOwnerAndDraft(application);
        if (request.userId() == null && request.institutionId() == null) {
            throw new BusinessException("A co-investigator must reference a user or an institution");
        }
        CoInvestigator coInvestigator = CoInvestigator.builder()
                .application(application)
                .userId(request.userId())
                .institutionId(request.institutionId())
                .role(parseRole(request.role()))
                .contribution(request.contribution())
                // Newly added team members start as INVITED and must accept before they are CONFIRMED.
                .status(CoInvestigatorStatus.INVITED)
                .build();
        CoInvestigator saved = coInvestigatorRepository.save(coInvestigator);
        log.info("Invited co-investigator {} to application {}", saved.getId(), applicationId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CoInvestigatorResponse respond(Long applicationId, Long coiId, String decision) {
        findApplication(applicationId);
        CoInvestigator coInvestigator = coInvestigatorRepository.findById(coiId)
                .orElseThrow(() -> new ResourceNotFoundException("CoInvestigator", coiId));
        if (!coInvestigator.getApplication().getId().equals(applicationId)) {
            throw new BusinessException("CoInvestigator does not belong to this application");
        }
        // Only the invited user may accept or decline their own invitation.
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(coInvestigator.getUserId())) {
            throw new AccessDeniedException("Only the invited user can respond to this invitation");
        }
        if (coInvestigator.getStatus() != CoInvestigatorStatus.INVITED) {
            throw new BusinessException("This invitation has already been " + coInvestigator.getStatus());
        }
        coInvestigator.setStatus(parseDecision(decision));
        CoInvestigator saved = coInvestigatorRepository.save(coInvestigator);
        log.info("Co-investigator {} responded {} on application {}", coiId, saved.getStatus(), applicationId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoInvestigatorResponse> listByApplication(Long applicationId) {
        assertCanRead(findApplication(applicationId));
        return coInvestigatorRepository.findByApplicationId(applicationId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void remove(Long applicationId, Long id) {
        GrantApplication application = findApplication(applicationId);
        assertOwnerAndDraft(application);
        CoInvestigator coInvestigator = coInvestigatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CoInvestigator", id));
        if (!coInvestigator.getApplication().getId().equals(applicationId)) {
            throw new BusinessException("CoInvestigator does not belong to this application");
        }
        coInvestigatorRepository.delete(coInvestigator);
        log.info("Removed co-investigator {} from application {}", id, applicationId);
    }

    private GrantApplication findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantApplication", applicationId));
    }

    /** The application must be owned by the caller (PI) or the caller is a Grant Admin / Admin. */
    private void assertOwner(GrantApplication application) {
        if (SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(application.getPrincipalInvestigatorId())) {
            throw new AccessDeniedException("You do not have access to this application");
        }
    }

    /** Read access: the owning PI or any full-pipeline staff role (Grant Admin/Admin/Finance/Compliance). */
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

    /** The team may only be edited by the owning PI (or an admin) while the application is a DRAFT. */
    private void assertOwnerAndDraft(GrantApplication application) {
        assertOwner(application);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("The team can only be changed while the application is a DRAFT");
        }
    }

    private CoInvestigatorRole parseRole(String raw) {
        try {
            return CoInvestigatorRole.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid co-investigator role: " + raw);
        }
    }

    private CoInvestigatorStatus parseDecision(String decision) {
        String value = decision == null ? "" : decision.trim().toUpperCase();
        return switch (value) {
            case "ACCEPT", "CONFIRMED", "CONFIRM" -> CoInvestigatorStatus.CONFIRMED;
            case "DECLINE", "DECLINED" -> CoInvestigatorStatus.DECLINED;
            default -> throw new BusinessException("Invalid decision: " + decision + " (expected ACCEPT or DECLINE)");
        };
    }
}
