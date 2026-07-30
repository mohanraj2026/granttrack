package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.ApplicationBudgetRequest;
import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.entity.ApplicationBudget;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.BudgetHead;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.ApplicationBudgetRepository;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationBudgetServiceImplTest {

    private static final long OWNER = 2L;

    @Mock
    private ApplicationBudgetRepository budgetRepository;

    @Mock
    private GrantApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper mapper;

    @InjectMocks
    private ApplicationBudgetServiceImpl budgetService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(OWNER));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private GrantApplication draftApp(Long id, String requested) {
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER)
                .status(ApplicationStatus.DRAFT)
                .requestedAmount(new BigDecimal(requested))
                .build();
        app.setId(id);
        return app;
    }

    @Test
    void add_Success() {
        Long appId = 1L;
        ApplicationBudgetRequest request = new ApplicationBudgetRequest("PERSONNEL", new BigDecimal("5000"), "Justification");
        ApplicationBudget budget = ApplicationBudget.builder().budgetHead(BudgetHead.PERSONNEL).build();
        budget.setId(1L);
        ApplicationBudgetResponse response = ApplicationBudgetResponse.builder().id(1L).budgetHead("PERSONNEL").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId, "100000")));
        when(budgetRepository.findByApplicationId(appId)).thenReturn(List.of());
        when(budgetRepository.save(any(ApplicationBudget.class))).thenReturn(budget);
        when(mapper.toResponse(budget)).thenReturn(response);

        ApplicationBudgetResponse result = budgetService.add(appId, request);

        assertNotNull(result);
        assertEquals("PERSONNEL", result.budgetHead());
    }

    @Test
    void add_ExceedsRequestedAmount_ThrowsException() {
        Long appId = 1L;
        ApplicationBudgetRequest request = new ApplicationBudgetRequest("PERSONNEL", new BigDecimal("500"), "j");
        ApplicationBudget existing = ApplicationBudget.builder().amount(new BigDecimal("800")).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId, "1000")));
        when(budgetRepository.findByApplicationId(appId)).thenReturn(List.of(existing));

        // 800 existing + 500 new = 1300 > 1000 requested
        assertThrows(BusinessException.class, () -> budgetService.add(appId, request));
        verify(budgetRepository, never()).save(any(ApplicationBudget.class));
    }

    @Test
    void add_NonDraft_ThrowsException() {
        Long appId = 1L;
        ApplicationBudgetRequest request = new ApplicationBudgetRequest("PERSONNEL", new BigDecimal("500"), "j");
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.SUBMITTED).requestedAmount(new BigDecimal("100000")).build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> budgetService.add(appId, request));
        verify(budgetRepository, never()).save(any(ApplicationBudget.class));
    }

    @Test
    void add_InvalidBudgetHead_ThrowsException() {
        Long appId = 1L;
        ApplicationBudgetRequest request = new ApplicationBudgetRequest("INVALID_HEAD", new BigDecimal("5000"), "Justification");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId, "100000")));
        when(budgetRepository.findByApplicationId(appId)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> budgetService.add(appId, request));
    }

    @Test
    void listByApplication_Success() {
        Long appId = 1L;
        ApplicationBudget budget = ApplicationBudget.builder().build();
        ApplicationBudgetResponse response = ApplicationBudgetResponse.builder().id(1L).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId, "100000")));
        when(budgetRepository.findByApplicationId(appId)).thenReturn(List.of(budget));
        when(mapper.toResponse(budget)).thenReturn(response);

        List<ApplicationBudgetResponse> result = budgetService.listByApplication(appId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
