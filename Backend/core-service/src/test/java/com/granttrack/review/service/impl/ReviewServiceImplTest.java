package com.granttrack.review.service.impl;

import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.repository.GrantCallRepository;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.review.dto.request.PanelDecisionRequest;
import com.granttrack.review.dto.request.ReviewScoreRequest;
import com.granttrack.review.dto.request.ReviewerAssignmentRequest;
import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.entity.AssignmentStatus;
import com.granttrack.review.entity.ConflictScreeningStatus;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.entity.ReviewCriterion;
import com.granttrack.review.entity.ReviewScore;
import com.granttrack.review.entity.ReviewerAssignment;
import com.granttrack.review.mapper.ReviewMapper;
import com.granttrack.review.repository.PanelDecisionRepository;
import com.granttrack.review.repository.ReviewScoreRepository;
import com.granttrack.review.repository.ReviewerAssignmentRepository;
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
import org.springframework.security.access.AccessDeniedException;

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
class ReviewServiceImplTest {

    private static final long REVIEWER = 2L;

    @Mock
    private ReviewerAssignmentRepository assignmentRepository;
    @Mock
    private ReviewScoreRepository scoreRepository;
    @Mock
    private PanelDecisionRepository panelDecisionRepository;
    @Mock
    private com.granttrack.application.repository.GrantApplicationRepository applicationRepository;
    @Mock
    private GrantCallRepository callRepository;
    @Mock
    private com.granttrack.auth.repository.UserRepository userRepository;
    @Mock
    private ReviewMapper mapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

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

    private ReviewerAssignment assignment(Long id, AssignmentStatus status, ConflictScreeningStatus coi) {
        ReviewerAssignment a = ReviewerAssignment.builder()
                .applicationId(1L).reviewerId(REVIEWER).status(status).conflictScreeningStatus(coi).build();
        a.setId(id);
        return a;
    }

    private GrantApplication underReviewApp(Long id) {
        GrantApplication app = GrantApplication.builder()
                .projectTitle("Test App").principalInvestigatorId(10L).status(ApplicationStatus.UNDER_REVIEW).build();
        app.setId(id);
        return app;
    }

    private com.granttrack.auth.entity.User financeOfficer(Long id) {
        var role = com.granttrack.auth.entity.Role.builder()
                .name(com.granttrack.auth.entity.RoleName.ROLE_FINANCE_OFFICER.name()).build();
        var user = com.granttrack.auth.entity.User.builder()
                .roles(java.util.Set.of(role)).build();
        user.setId(id);
        return user;
    }

    // ---- assignReviewer ----

    @Test
    void assignReviewer_Success() {
        ReviewerAssignmentRequest request = new ReviewerAssignmentRequest(1L, REVIEWER, LocalDate.now());
        GrantApplication app = GrantApplication.builder().status(ApplicationStatus.SUBMITTED).build();
        app.setId(1L);
        ReviewerAssignment assignment = assignment(1L, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("ASSIGNED").build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(assignmentRepository.existsByApplicationIdAndReviewerId(1L, REVIEWER)).thenReturn(false);
        when(assignmentRepository.save(any(ReviewerAssignment.class))).thenReturn(assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        ReviewerAssignmentResponse result = reviewService.assignReviewer(request);

        assertNotNull(result);
        verify(notificationService, times(1)).notify(eq(REVIEWER), anyString(), any());
    }

    @Test
    void assignReviewer_ApplicationNotInReviewableStatus_ThrowsException() {
        ReviewerAssignmentRequest request = new ReviewerAssignmentRequest(1L, REVIEWER, LocalDate.now());
        GrantApplication app = GrantApplication.builder().status(ApplicationStatus.DRAFT).build();
        app.setId(1L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> reviewService.assignReviewer(request));
        verify(assignmentRepository, never()).save(any(ReviewerAssignment.class));
    }

    @Test
    void assignReviewer_Duplicate_ThrowsException() {
        ReviewerAssignmentRequest request = new ReviewerAssignmentRequest(1L, REVIEWER, LocalDate.now());
        GrantApplication app = GrantApplication.builder().status(ApplicationStatus.SUBMITTED).build();
        app.setId(1L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(assignmentRepository.existsByApplicationIdAndReviewerId(1L, REVIEWER)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> reviewService.assignReviewer(request));
    }

    // ---- conflict / respond ----

    @Test
    void recordConflictCheck_Success() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewerAssignment a = assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).conflictScreeningStatus("COI_DECLARED").build();

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any(ReviewerAssignment.class))).thenReturn(a);
        when(mapper.toResponse(a)).thenReturn(response);

        ReviewerAssignmentResponse result = reviewService.recordConflictCheck(id, "COI_DECLARED");

        assertEquals("COI_DECLARED", result.conflictScreeningStatus());
    }

    @Test
    void respond_Accept_Success() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewerAssignment a = assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("ACCEPTED").build();

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any(ReviewerAssignment.class))).thenReturn(a);
        when(mapper.toResponse(a)).thenReturn(response);

        ReviewerAssignmentResponse result = reviewService.respond(id, "ACCEPT", null);

        assertEquals("ACCEPTED", result.status());
    }

    @Test
    void respond_Decline_RequiresReason() {
        loginAs(REVIEWER);
        Long id = 1L;
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR)));

        // Declining without a reason is a clean 400, and nothing is persisted.
        assertThrows(BusinessException.class, () -> reviewService.respond(id, "DECLINE", "  "));
        verify(assignmentRepository, never()).save(any(ReviewerAssignment.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void respond_Decline_StoresReasonAndNotifiesGrantAdmins() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewerAssignment a = assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR);
        a.setApplicationId(77L);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("DECLINED").build();

        var admin = com.granttrack.auth.entity.User.builder().build();
        admin.setId(9L);

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any(ReviewerAssignment.class))).thenReturn(a);
        when(mapper.toResponse(a)).thenReturn(response);
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(admin));

        ReviewerAssignmentResponse result = reviewService.respond(id, "DECLINE", "Conflict of interest with a co-author");

        assertEquals("DECLINED", result.status());
        assertEquals(AssignmentStatus.DECLINED, a.getStatus());
        assertEquals("Conflict of interest with a co-author", a.getResponseComment());
        // The Grant Admin is notified with the reason so they can reassign.
        verify(notificationService, times(1)).notify(eq(9L), anyString(), any());
    }

    @Test
    void respond_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        Long id = 1L;
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR)));

        assertThrows(AccessDeniedException.class, () -> reviewService.respond(id, "ACCEPT", null));
    }

    @Test
    void respond_InvalidStatus_ThrowsException() {
        loginAs(REVIEWER);
        Long id = 1L;
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR)));

        assertThrows(BusinessException.class, () -> reviewService.respond(id, "ACCEPT", null));
    }

    // ---- submitScore ----

    @Test
    void submitScore_Success() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", "FUND_AT_FULL_AMOUNT");
        ReviewScore score = ReviewScore.builder().score(9).build();
        ReviewScoreResponse response = ReviewScoreResponse.builder().id(1L).score(9).build();

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR)));
        when(scoreRepository.existsByAssignmentIdAndCriterion(id, ReviewCriterion.SCIENTIFIC_MERIT)).thenReturn(false);
        when(scoreRepository.save(any(ReviewScore.class))).thenReturn(score);
        when(mapper.toResponse(score)).thenReturn(response);

        ReviewScoreResponse result = reviewService.submitScore(id, request);

        assertEquals(9, result.score());
    }

    @Test
    void submitScore_NotAccepted_ThrowsException() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", null);
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR)));

        assertThrows(BusinessException.class, () -> reviewService.submitScore(id, request));
        verify(scoreRepository, never()).save(any(ReviewScore.class));
    }

    @Test
    void submitScore_ConflictDeclared_ThrowsException() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", null);
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.COI_DECLARED)));

        assertThrows(BusinessException.class, () -> reviewService.submitScore(id, request));
        verify(scoreRepository, never()).save(any(ReviewScore.class));
    }

    @Test
    void submitScore_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        Long id = 1L;
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", null);
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR)));

        assertThrows(AccessDeniedException.class, () -> reviewService.submitScore(id, request));
    }

    @Test
    void submitScore_DuplicateCriterion_ThrowsException() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", null);
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR)));
        when(scoreRepository.existsByAssignmentIdAndCriterion(id, ReviewCriterion.SCIENTIFIC_MERIT)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> reviewService.submitScore(id, request));
    }

    // ---- submitAssignment / search / scores ----

    @Test
    void submitAssignment_Success() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewerAssignment a = assignment(id, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("SUBMITTED").build();

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any(ReviewerAssignment.class))).thenReturn(a);
        when(mapper.toResponse(a)).thenReturn(response);

        ReviewerAssignmentResponse result = reviewService.submitAssignment(id);

        assertEquals("SUBMITTED", result.status());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchAssignments_Success() {
        loginAs(REVIEWER);
        Pageable pageable = PageRequest.of(0, 10);
        ReviewerAssignment a = assignment(1L, AssignmentStatus.ASSIGNED, ConflictScreeningStatus.CLEAR);
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).build();
        Page<ReviewerAssignment> page = new PageImpl<>(List.of(a));

        when(assignmentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(a)).thenReturn(response);

        Page<ReviewerAssignmentResponse> result = reviewService.searchAssignments(1L, REVIEWER, "ASSIGNED", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getScores_Success() {
        loginAs(REVIEWER);
        Long id = 1L;
        ReviewScore score = ReviewScore.builder().build();
        ReviewScoreResponse response = ReviewScoreResponse.builder().id(1L).build();

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(assignment(id, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR)));
        when(scoreRepository.findByAssignmentId(id)).thenReturn(List.of(score));
        when(mapper.toResponse(score)).thenReturn(response);

        List<ReviewScoreResponse> result = reviewService.getScores(id);

        assertEquals(1, result.size());
    }

    // ---- panel decision ----

    @Test
    void createPanelDecision_Success() {
        Long appId = 1L;
        loginAs(3L);
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), new BigDecimal("8.5"), "FULL_AWARD", new BigDecimal("10000"), "Cond", 99L);
        PanelDecision decision = PanelDecision.builder().awardDecision(com.granttrack.review.entity.AwardDecision.FULL_AWARD).build();
        decision.setId(1L);
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("FULL_AWARD").build();

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(userRepository.findById(99L)).thenReturn(Optional.of(financeOfficer(99L)));
        when(assignmentRepository.findByApplicationId(appId))
                .thenReturn(List.of(assignment(5L, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR)));
        when(panelDecisionRepository.save(any(PanelDecision.class))).thenReturn(decision);
        when(mapper.toResponse(decision)).thenReturn(response);

        PanelDecisionResponse result = reviewService.createPanelDecision(appId, request);

        assertEquals("FULL_AWARD", result.awardDecision());
    }

    @Test
    void createPanelDecision_AwardWithoutFinanceOfficer_ThrowsException() {
        Long appId = 1L;
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), null, "FULL_AWARD", new BigDecimal("5000"), null, null);

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(assignmentRepository.findByApplicationId(appId))
                .thenReturn(List.of(assignment(5L, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR)));

        // An award decision must name a finance officer.
        assertThrows(BusinessException.class, () -> reviewService.createPanelDecision(appId, request));
    }

    @Test
    void createPanelDecision_Award_AdvancesCallToAwarded() {
        Long appId = 1L;
        loginAs(3L);
        GrantApplication app = GrantApplication.builder()
                .projectTitle("A").principalInvestigatorId(10L).callId(7L).status(ApplicationStatus.UNDER_REVIEW).build();
        app.setId(appId);
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), new BigDecimal("9.0"), "FULL_AWARD", new BigDecimal("5000"), null, 99L);
        PanelDecision decision = PanelDecision.builder().awardDecision(com.granttrack.review.entity.AwardDecision.FULL_AWARD).build();
        decision.setId(1L);
        GrantCall call = GrantCall.builder().status(CallStatus.UNDER_REVIEW).build();
        call.setId(7L);

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(userRepository.findById(99L)).thenReturn(Optional.of(financeOfficer(99L)));
        when(assignmentRepository.findByApplicationId(appId))
                .thenReturn(List.of(assignment(5L, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR)));
        when(panelDecisionRepository.save(any(PanelDecision.class))).thenReturn(decision);
        when(callRepository.findById(7L)).thenReturn(Optional.of(call));
        when(mapper.toResponse(decision)).thenReturn(PanelDecisionResponse.builder().id(1L).awardDecision("FULL_AWARD").build());

        reviewService.createPanelDecision(appId, request);

        assertEquals(CallStatus.AWARDED, call.getStatus());
        verify(callRepository, times(1)).save(call);
    }

    @Test
    void createPanelDecision_NotUnderReview_ThrowsException() {
        Long appId = 1L;
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), null, "REJECTED", null, null, null);
        GrantApplication app = GrantApplication.builder().status(ApplicationStatus.SUBMITTED).build();
        app.setId(appId);

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> reviewService.createPanelDecision(appId, request));
    }

    @Test
    void createPanelDecision_NoSubmittedReview_ThrowsException() {
        Long appId = 1L;
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), null, "REJECTED", null, null, null);

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(assignmentRepository.findByApplicationId(appId))
                .thenReturn(List.of(assignment(5L, AssignmentStatus.ACCEPTED, ConflictScreeningStatus.CLEAR)));

        assertThrows(BusinessException.class, () -> reviewService.createPanelDecision(appId, request));
    }

    @Test
    void createPanelDecision_AwardWithoutAmount_ThrowsException() {
        Long appId = 1L;
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), null, "FULL_AWARD", null, null, null);

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(assignmentRepository.findByApplicationId(appId))
                .thenReturn(List.of(assignment(5L, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR)));

        assertThrows(BusinessException.class, () -> reviewService.createPanelDecision(appId, request));
    }

    @Test
    void createPanelDecision_AutoComputesConsensus_WhenOmitted() {
        Long appId = 1L;
        loginAs(3L);
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), null, "REJECTED", null, null, null);
        ReviewerAssignment submitted = assignment(5L, AssignmentStatus.SUBMITTED, ConflictScreeningStatus.CLEAR);
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("REJECTED").build();

        when(panelDecisionRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(assignmentRepository.findByApplicationId(appId)).thenReturn(List.of(submitted));
        when(scoreRepository.findByAssignmentId(5L)).thenReturn(List.of(
                ReviewScore.builder().score(8).build(), ReviewScore.builder().score(6).build()));
        when(panelDecisionRepository.save(any(PanelDecision.class))).thenAnswer(inv -> {
            PanelDecision d = inv.getArgument(0);
            d.setId(1L);
            // consensus should be the mean of 8 and 6 = 7.00
            assertEquals(0, d.getConsensusScore().compareTo(new BigDecimal("7.00")));
            return d;
        });
        when(mapper.toResponse(any(PanelDecision.class))).thenReturn(response);

        PanelDecisionResponse result = reviewService.createPanelDecision(appId, request);

        assertEquals("REJECTED", result.awardDecision());
    }

    @Test
    void getPanelDecision_Success() {
        Long appId = 1L;
        // A Grant Admin (full-pipeline staff) may read any panel decision without the ownership check.
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN",
                "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER")).thenReturn(true);
        PanelDecision decision = PanelDecision.builder().build();
        decision.setId(1L);
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("FULL_AWARD").build();

        when(panelDecisionRepository.findByApplicationId(appId)).thenReturn(Optional.of(decision));
        when(mapper.toResponse(decision)).thenReturn(response);

        PanelDecisionResponse result = reviewService.getPanelDecision(appId);

        assertEquals("FULL_AWARD", result.awardDecision());
    }

    @Test
    void getPanelDecision_Researcher_OwnApplication_Success() {
        Long appId = 1L;
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")).thenReturn(false);
        loginAs(10L); // matches underReviewApp PI id
        PanelDecision decision = PanelDecision.builder().build();
        decision.setId(1L);
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("FULL_AWARD").build();

        when(panelDecisionRepository.findByApplicationId(appId)).thenReturn(Optional.of(decision));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));
        when(mapper.toResponse(decision)).thenReturn(response);

        assertEquals("FULL_AWARD", reviewService.getPanelDecision(appId).awardDecision());
    }

    @Test
    void getPanelDecision_Researcher_OtherApplication_ThrowsAccessDenied() {
        Long appId = 1L;
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ROLE_GRANT_ADMIN", "ROLE_ADMIN")).thenReturn(false);
        loginAs(999L); // not the PI (underReviewApp PI id = 10)
        when(panelDecisionRepository.findByApplicationId(appId)).thenReturn(Optional.of(PanelDecision.builder().build()));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(underReviewApp(appId)));

        assertThrows(AccessDeniedException.class, () -> reviewService.getPanelDecision(appId));
    }

    @Test
    void getPanelDecision_NotFound_ReturnsNull() {
        // "No decision yet" is a normal state (not an error) so the UI shows an empty state, not a toast.
        when(panelDecisionRepository.findByApplicationId(1L)).thenReturn(Optional.empty());

        assertNull(reviewService.getPanelDecision(1L));
    }
}
