package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.ApplicationBudgetRequest;
import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.entity.ApplicationBudget;
import com.granttrack.application.entity.BudgetHead;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.ApplicationBudgetRepository;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.service.ApplicationBudgetService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationBudgetServiceImpl implements ApplicationBudgetService {

    private final ApplicationBudgetRepository budgetRepository;
    private final GrantApplicationRepository applicationRepository;
    private final ApplicationMapper mapper;

    @Override
    @Transactional
    public ApplicationBudgetResponse add(Long applicationId, ApplicationBudgetRequest request) {
        GrantApplication application = findApplication(applicationId);
        assertOwnerAndDraft(application);
        // The itemised budget total must never exceed the amount requested on the application.
        BigDecimal existingTotal = budgetRepository.findByApplicationId(applicationId).stream()
                .map(ApplicationBudget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingTotal.add(request.amount()).compareTo(application.getRequestedAmount()) > 0) {
            throw new BusinessException("Total budget would exceed the requested amount ("
                    + application.getRequestedAmount() + ")");
        }
        ApplicationBudget budget = ApplicationBudget.builder()
                .application(application)
                .budgetHead(parseHead(request.budgetHead()))
                .amount(request.amount())
                .justification(request.justification())
                .build();
        ApplicationBudget saved = budgetRepository.save(budget);
        log.info("Added budget line {} to application {}", saved.getId(), applicationId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationBudgetResponse> listByApplication(Long applicationId) {
        assertCanRead(findApplication(applicationId));
        return budgetRepository.findByApplicationId(applicationId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void remove(Long applicationId, Long id) {
        GrantApplication application = findApplication(applicationId);
        assertOwnerAndDraft(application);
        ApplicationBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationBudget", id));
        if (!budget.getApplication().getId().equals(applicationId)) {
            throw new BusinessException("Budget does not belong to this application");
        }
        budgetRepository.delete(budget);
        log.info("Removed budget line {} from application {}", id, applicationId);
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

    /** The budget may only be edited by the owning PI (or an admin) while the application is a DRAFT. */
    private void assertOwnerAndDraft(GrantApplication application) {
        assertOwner(application);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("The budget can only be changed while the application is a DRAFT");
        }
    }

    private BudgetHead parseHead(String raw) {
        try {
            return BudgetHead.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid budget head: " + raw);
        }
    }
}
