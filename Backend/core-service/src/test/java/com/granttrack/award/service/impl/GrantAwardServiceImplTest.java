package com.granttrack.award.service.impl;

import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.dto.request.AwardTermsRequest;
import com.granttrack.award.dto.request.GrantAwardRequest;
import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.mapper.AwardMapper;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.review.entity.AwardDecision;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.repository.PanelDecisionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrantAwardServiceImplTest {

    private static final long PI = 2L;

    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private AwardMapper mapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GrantApplicationRepository applicationRepository;
    @Mock
    private PanelDecisionRepository panelDecisionRepository;
    @Mock
    private com.granttrack.auth.repository.UserRepository userRepository;

    @InjectMocks
    private GrantAwardServiceImpl grantAwardService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private void loginAs(long id) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(id));
    }

    private GrantApplication awardedApp() {
        return GrantApplication.builder().principalInvestigatorId(PI).status(ApplicationStatus.AWARDED).build();
    }

    private PanelDecision favourablePanel(String amount) {
        return PanelDecision.builder().awardDecision(AwardDecision.FULL_AWARD).awardedAmount(new BigDecimal(amount)).build();
    }

    private GrantAwardRequest request(String amount) {
        return new GrantAwardRequest(1L, new BigDecimal(amount), LocalDate.now(), LocalDate.now().plusYears(1), "conditions.pdf", LocalDate.now());
    }

    @Test
    void create_Success() {
        GrantAward award = GrantAward.builder().status(AwardStatus.ACTIVE).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardRepository.existsByApplicationId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));
        when(panelDecisionRepository.findByApplicationId(1L)).thenReturn(Optional.of(favourablePanel("50000")));
        when(awardRepository.save(any(GrantAward.class))).thenReturn(award);
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.create(request("50000"));

        assertNotNull(result);
        verify(awardRepository, times(1)).save(any(GrantAward.class));
        verify(notificationService, times(1)).notify(eq(PI), anyString(), eq(NotificationCategory.AWARD));
    }

    @Test
    void create_Duplicate_ThrowsException() {
        when(awardRepository.existsByApplicationId(1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> grantAwardService.create(request("50000")));
        verify(awardRepository, never()).save(any(GrantAward.class));
    }

    @Test
    void create_ApplicationNotAwarded_ThrowsException() {
        when(awardRepository.existsByApplicationId(1L)).thenReturn(false);
        GrantApplication app = GrantApplication.builder().principalInvestigatorId(PI).status(ApplicationStatus.UNDER_REVIEW).build();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> grantAwardService.create(request("50000")));
        verify(awardRepository, never()).save(any(GrantAward.class));
    }

    @Test
    void create_NoPanelDecision_ThrowsException() {
        when(awardRepository.existsByApplicationId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));
        when(panelDecisionRepository.findByApplicationId(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> grantAwardService.create(request("50000")));
    }

    @Test
    void create_AmountExceedsPanel_ThrowsException() {
        when(awardRepository.existsByApplicationId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));
        when(panelDecisionRepository.findByApplicationId(1L)).thenReturn(Optional.of(favourablePanel("40000")));

        assertThrows(BusinessException.class, () -> grantAwardService.create(request("50000")));
        verify(awardRepository, never()).save(any(GrantAward.class));
    }

    @Test
    void update_Success() {
        AwardTermsRequest request = new AwardTermsRequest(
                new BigDecimal("60000"), LocalDate.now(), LocalDate.now().plusYears(2), "new-conditions.pdf");
        GrantAward award = GrantAward.builder().status(AwardStatus.ACTIVE).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(awardRepository.save(any(GrantAward.class))).thenReturn(award);
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.update(1L, request);

        assertNotNull(result);
        verify(awardRepository, times(1)).save(any(GrantAward.class));
    }

    @Test
    void update_CompletedAward_ThrowsException() {
        AwardTermsRequest request = new AwardTermsRequest(new BigDecimal("60000"), LocalDate.now(), LocalDate.now(), "c");
        GrantAward award = GrantAward.builder().status(AwardStatus.COMPLETED).build();
        award.setId(1L);
        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> grantAwardService.update(1L, request));
        verify(awardRepository, never()).save(any(GrantAward.class));
    }

    @Test
    void approve_Success() {
        GrantAward award = GrantAward.builder().status(AwardStatus.SUSPENDED).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).status("ACTIVE").build();

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(awardRepository.save(any(GrantAward.class))).thenReturn(award);
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.approve(1L);

        assertNotNull(result);
        verify(awardRepository, times(1)).save(any(GrantAward.class));
    }

    @Test
    void changeStatus_Success() {
        GrantAward award = GrantAward.builder().applicationId(1L).status(AwardStatus.ACTIVE).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).status("SUSPENDED").build();

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(awardRepository.save(any(GrantAward.class))).thenReturn(award);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.changeStatus(1L, "SUSPENDED");

        assertEquals("SUSPENDED", result.status());
        verify(notificationService, times(1)).notify(eq(PI), anyString(), eq(NotificationCategory.AWARD));
    }

    @Test
    void changeStatus_InvalidTransition_ThrowsException() {
        GrantAward award = GrantAward.builder().status(AwardStatus.TERMINATED).build();
        award.setId(1L);
        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> grantAwardService.changeStatus(1L, "ACTIVE"));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        GrantAward award = GrantAward.builder().applicationId(1L).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));
        when(mapper.toResponse(award)).thenReturn(response);

        assertNotNull(grantAwardService.getById(1L));
    }

    @Test
    void getById_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        GrantAward award = GrantAward.builder().applicationId(1L).build();
        award.setId(1L);

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(awardedApp()));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> grantAwardService.getById(1L));
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(awardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> grantAwardService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        GrantAward award = GrantAward.builder().build();
        award.setId(1L);
        Page<GrantAward> page = new PageImpl<>(List.of(award));
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(1L));
        when(awardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(GrantAward.class))).thenReturn(response);

        Page<GrantAwardResponse> result = grantAwardService.search("ACTIVE", 1L, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void financeReview_Accept_Success() {
        loginAs(50L);
        GrantAward award = GrantAward.builder()
                .applicationId(9L).status(AwardStatus.ACTIVE)
                .financeOfficerId(50L)
                .financeReviewStatus(com.granttrack.award.entity.FinanceReviewStatus.PENDING).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).financeReviewStatus("ACCEPTED").build();

        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(awardRepository.save(award)).thenReturn(award);
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<com.granttrack.auth.entity.User>>any())).thenReturn(List.of());
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.financeReview(1L, "ACCEPT", null);

        assertEquals(com.granttrack.award.entity.FinanceReviewStatus.ACCEPTED, award.getFinanceReviewStatus());
        assertEquals("ACCEPTED", result.financeReviewStatus());
    }

    @Test
    void financeReview_Reject_RequiresReason() {
        loginAs(50L);
        GrantAward award = GrantAward.builder()
                .applicationId(9L).status(AwardStatus.ACTIVE)
                .financeOfficerId(50L)
                .financeReviewStatus(com.granttrack.award.entity.FinanceReviewStatus.PENDING).build();
        award.setId(1L);
        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> grantAwardService.financeReview(1L, "REJECT", "  "));
        verify(awardRepository, never()).save(any(GrantAward.class));
    }

    @Test
    void financeReview_AnyFinanceOfficer_Success() {
        // Finance staff are interchangeable: a finance officer other than the one named on the
        // panel decision may still action a PENDING finance review (role-gated at the controller).
        loginAs(999L); // a different finance officer than the assigned one (50)
        GrantAward award = GrantAward.builder()
                .applicationId(9L).status(AwardStatus.ACTIVE).financeOfficerId(50L)
                .financeReviewStatus(com.granttrack.award.entity.FinanceReviewStatus.PENDING).build();
        award.setId(1L);
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).financeReviewStatus("ACCEPTED").build();
        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));
        when(awardRepository.save(award)).thenReturn(award);
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<com.granttrack.auth.entity.User>>any())).thenReturn(List.of());
        when(mapper.toResponse(award)).thenReturn(response);

        GrantAwardResponse result = grantAwardService.financeReview(1L, "ACCEPT", null);

        assertEquals(com.granttrack.award.entity.FinanceReviewStatus.ACCEPTED, award.getFinanceReviewStatus());
        assertEquals("ACCEPTED", result.financeReviewStatus());
    }

    @Test
    void delete_Success() {
        GrantAward award = GrantAward.builder().build();
        award.setId(1L);
        when(awardRepository.findById(1L)).thenReturn(Optional.of(award));

        grantAwardService.delete(1L);

        assertTrue(award.isDeleted());
        verify(awardRepository, times(1)).save(award);
    }
}
