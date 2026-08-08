package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.GrantApplicationRequest;
import com.granttrack.application.dto.response.BlindApplicationResponse;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.repository.GrantCallRepository;
import com.granttrack.review.repository.ReviewerAssignmentRepository;
import com.granttrack.notification.service.NotificationService;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrantApplicationServiceImplTest {

    private static final long OWNER = 2L;

    @Mock
    private GrantApplicationRepository applicationRepository;

    @Mock
    private GrantCallRepository callRepository;

    @Mock
    private ReviewerAssignmentRepository assignmentRepository;

    @Mock
    private com.granttrack.application.repository.CoInvestigatorRepository coInvestigatorRepository;

    @Mock
    private ApplicationMapper mapper;

    @Mock
    private DocumentStorageService documentStorage;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GrantApplicationServiceImpl applicationService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private void loginAs(long userId) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
    }

    private GrantCall openCall() {
        return GrantCall.builder()
                .status(CallStatus.OPEN)
                .openDate(LocalDate.now().minusDays(1))
                .closeDate(LocalDate.now().plusDays(30))
                .build();
    }

    @Test
    void create_ForcesCurrentUserAsPI_Success() {
        loginAs(OWNER);
        when(callRepository.findById(1L)).thenReturn(Optional.of(openCall()));
        // The explicit PI in the payload (999L) must be ignored in favour of the caller.
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 999L, "Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplication saved = GrantApplication.builder().status(ApplicationStatus.DRAFT).build();
        saved.setId(1L);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).build();

        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        GrantApplicationResponse result = applicationService.create(request);

        assertNotNull(result);
        verify(applicationRepository, times(1)).save(argThat(a -> OWNER == a.getPrincipalInvestigatorId()));
    }

    @Test
    void create_CallNotOpen_ThrowsException() {
        loginAs(OWNER);
        GrantCall upcoming = GrantCall.builder().status(CallStatus.UPCOMING).build();
        when(callRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, null, "Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);

        assertThrows(BusinessException.class, () -> applicationService.create(request));
        verify(applicationRepository, never()).save(any(GrantApplication.class));
    }

    @Test
    void update_Draft_Success() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 2L, "Upd Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.DRAFT).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).projectTitle("Upd Title").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.update(appId, request);

        assertNotNull(result);
        assertEquals("Upd Title", result.projectTitle());
    }

    @Test
    void update_NotOwner_ThrowsAccessDenied() {
        loginAs(3L); // a different researcher
        Long appId = 1L;
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 2L, "Upd Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.DRAFT).build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(AccessDeniedException.class, () -> applicationService.update(appId, request));
        verify(applicationRepository, never()).save(any(GrantApplication.class));
    }

    @Test
    void update_NonDraft_ThrowsException() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 2L, "Upd Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.SUBMITTED).build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> applicationService.update(appId, request));
    }

    @Test
    void getById_Success() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().principalInvestigatorId(OWNER).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.getById(appId);

        assertNotNull(result);
        assertEquals(appId, result.id());
    }

    @Test
    void getById_NotOwner_ThrowsAccessDenied() {
        loginAs(3L);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().principalInvestigatorId(OWNER).build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(AccessDeniedException.class, () -> applicationService.getById(appId));
    }

    @Test
    void getBlindById_AssignedReviewer_Success() {
        loginAs(OWNER); // acting as an assigned reviewer (id OWNER)
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().projectTitle("Blind Title").build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(assignmentRepository.existsByApplicationIdAndReviewerId(appId, OWNER)).thenReturn(true);

        BlindApplicationResponse result = applicationService.getBlindById(appId);

        assertNotNull(result);
        assertEquals("Blind Title", result.projectTitle());
    }

    @Test
    void getBlindById_NotAssigned_ThrowsAccessDenied() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().projectTitle("Blind Title").build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(assignmentRepository.existsByApplicationIdAndReviewerId(appId, OWNER)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> applicationService.getBlindById(appId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_Success() {
        loginAs(OWNER);
        Pageable pageable = PageRequest.of(0, 10);
        GrantApplication app = GrantApplication.builder().build();
        app.setId(1L);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).build();
        Page<GrantApplication> page = new PageImpl<>(List.of(app));

        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(app)).thenReturn(response);

        Page<GrantApplicationResponse> result = applicationService.search("Title", "DRAFT", 1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void submit_Draft_Success() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder()
                .callId(1L).principalInvestigatorId(OWNER).projectTitle("Title").status(ApplicationStatus.DRAFT).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).status("SUBMITTED").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(callRepository.findById(1L)).thenReturn(Optional.of(openCall()));
        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.submit(appId);

        assertNotNull(result);
        assertEquals("SUBMITTED", result.status());
        verify(notificationService, times(1)).notify(any(), any(), any());
    }

    @Test
    void submit_CallNotOpen_ThrowsException() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder()
                .callId(1L).principalInvestigatorId(OWNER).status(ApplicationStatus.DRAFT).build();
        app.setId(appId);
        GrantCall closed = GrantCall.builder().status(CallStatus.CLOSED).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(callRepository.findById(1L)).thenReturn(Optional.of(closed));

        assertThrows(BusinessException.class, () -> applicationService.submit(appId));
        verify(applicationRepository, never()).save(any(GrantApplication.class));
    }

    @Test
    void withdraw_Submitted_Success() {
        loginAs(OWNER);
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.SUBMITTED).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).status("WITHDRAWN").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.withdraw(appId);

        assertNotNull(result);
        assertEquals("WITHDRAWN", result.status());
    }

    @Test
    void changeStatus_ValidTransition_Success() {
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().projectTitle("Title").status(ApplicationStatus.SUBMITTED).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).status("UNDER_REVIEW").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.changeStatus(appId, "UNDER_REVIEW");

        assertNotNull(result);
        assertEquals("UNDER_REVIEW", result.status());
        verify(notificationService, times(1)).notify(any(), any(), any());
    }

    @Test
    void changeStatus_InvalidTransition_ThrowsException() {
        Long appId = 1L;
        GrantApplication app = GrantApplication.builder().status(ApplicationStatus.DRAFT).build();
        app.setId(appId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class, () -> applicationService.changeStatus(appId, "AWARDED"));
    }

    @Test
    void uploadAbstract_Draft_Success() {
        loginAs(OWNER);
        Long appId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.DRAFT).build();
        app.setId(appId);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(appId).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(documentStorage.storeAbstract(appId, file)).thenReturn("path/doc.pdf");
        when(applicationRepository.save(any(GrantApplication.class))).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        GrantApplicationResponse result = applicationService.uploadAbstract(appId, file);

        assertNotNull(result);
        assertEquals("path/doc.pdf", app.getAbstractDocPath());
    }
}
