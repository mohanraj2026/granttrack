package com.granttrack.progress.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.progress.dto.request.ProgressReportRequest;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.entity.ProgressReport;
import com.granttrack.progress.entity.ProgressStatus;
import com.granttrack.progress.mapper.ProgressMapper;
import com.granttrack.progress.repository.ProgressReportRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressReportServiceImplTest {

    private static final long PI = 100L;
    private static final long AWARD_ID = 1L;
    private static final long APP_ID = 1L;

    @Mock
    private ProgressReportRepository reportRepository;
    @Mock
    private ProgressMapper mapper;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private com.granttrack.application.service.DocumentStorageService documentStorageService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgressReportServiceImpl reportService;

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

    private void asStaff() {
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any(String[].class))).thenReturn(true);
    }

    private GrantAward activeAward() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.ACTIVE).build();
        award.setId(AWARD_ID);
        return award;
    }

    private GrantApplication piApp() {
        return GrantApplication.builder().principalInvestigatorId(PI).build();
    }

    private void wireOwnership() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));
    }

    private ProgressReportRequest request() {
        return new ProgressReportRequest(AWARD_ID, null, "Q1", "Summary", "Achievements", "Challenges", new BigDecimal("50"));
    }

    @Test
    void create_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().build();
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.create(request()));
        verify(reportRepository, times(1)).save(any(ProgressReport.class));
    }

    @Test
    void create_AwardNotActive_ThrowsException() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.SUSPENDED).build();
        award.setId(AWARD_ID);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> reportService.create(request()));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }

    @Test
    void create_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();

        assertThrows(AccessDeniedException.class, () -> reportService.create(request()));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }

    @Test
    void update_Draft_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.DRAFT).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.update(1L, request()));
        verify(reportRepository, times(1)).save(any(ProgressReport.class));
    }

    @Test
    void update_RevisionRequested_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.REVISION_REQUESTED).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.update(1L, request()));
    }

    @Test
    void update_Approved_ThrowsException() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.APPROVED).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(BusinessException.class, () -> reportService.update(1L, request()));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }

    @Test
    void submit_Draft_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.DRAFT).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.submit(1L));
        assertEquals(ProgressStatus.SUBMITTED, report.getStatus());
        assertEquals(PI, report.getSubmittedById());
    }

    @Test
    void submit_RevisionRequested_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.REVISION_REQUESTED).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.submit(1L));
        assertEquals(ProgressStatus.SUBMITTED, report.getStatus());
    }

    @Test
    void review_Approve_Success() {
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).period("Q1").status(ProgressStatus.SUBMITTED).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));

        assertNotNull(reportService.review(1L, "APPROVE", "Well documented progress"));
        assertEquals(ProgressStatus.APPROVED, report.getStatus());
        verify(notificationService, times(1)).notify(eq(PI), anyString(), eq(NotificationCategory.PROGRESS));
    }

    @Test
    void review_RequestRevision_Success() {
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).status(ProgressStatus.SUBMITTED).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));

        assertNotNull(reportService.review(1L, "REQUEST_REVISION", "Please add budget detail"));
        assertEquals(ProgressStatus.REVISION_REQUESTED, report.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void submit_NotifiesComplianceOfficers() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).period("Q1").status(ProgressStatus.DRAFT).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(ProgressReportResponse.builder().id(1L).build());
        User officer = User.builder().build();
        officer.setId(77L);
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(officer));

        reportService.submit(1L);

        verify(notificationService, times(1)).notify(eq(77L), anyString(), eq(NotificationCategory.PROGRESS));
    }

    @Test
    void review_Approve_NotifiesAssignedFinanceOfficerAndResearcher() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.ACTIVE).financeOfficerId(50L).build();
        award.setId(AWARD_ID);
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).period("Q1").status(ProgressStatus.SUBMITTED).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(ProgressReportResponse.builder().id(1L).build());
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));

        reportService.review(1L, "APPROVE", "ok");

        verify(notificationService, times(1)).notify(eq(50L), anyString(), eq(NotificationCategory.PROGRESS)); // finance officer
        verify(notificationService, times(1)).notify(eq(PI), anyString(), eq(NotificationCategory.PROGRESS));   // researcher
    }

    @Test
    void review_NotSubmitted_ThrowsException() {
        ProgressReport report = ProgressReport.builder().status(ProgressStatus.DRAFT).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(BusinessException.class, () -> reportService.review(1L, "APPROVE", null));
    }

    @Test
    void review_InvalidDecision_ThrowsException() {
        ProgressReport report = ProgressReport.builder().status(ProgressStatus.SUBMITTED).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(BusinessException.class, () -> reportService.review(1L, "MAYBE", null));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.getById(1L));
    }

    @Test
    void getById_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).build();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(AccessDeniedException.class, () -> reportService.getById(1L));
    }

    @Test
    void getById_Staff_Success() {
        asStaff();
        ProgressReport report = ProgressReport.builder().awardId(AWARD_ID).build();
        report.setId(1L);
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(mapper.toResponse(report)).thenReturn(response);

        assertNotNull(reportService.getById(1L));
        verify(awardRepository, never()).findById(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        ProgressReport report = ProgressReport.builder().build();
        Page<ProgressReport> page = new PageImpl<>(List.of(report));
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(APP_ID));
        when(awardRepository.findIdsByApplicationIdIn(List.of(APP_ID))).thenReturn(List.of(AWARD_ID));
        when(reportRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(ProgressReport.class))).thenReturn(response);

        Page<ProgressReportResponse> result = reportService.search(AWARD_ID, "DRAFT", pageable);

        assertEquals(1, result.getTotalElements());
    }
}
