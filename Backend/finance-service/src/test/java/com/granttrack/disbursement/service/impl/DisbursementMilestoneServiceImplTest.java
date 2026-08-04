package com.granttrack.disbursement.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.FundDisbursement;
import com.granttrack.disbursement.entity.MilestoneStatus;
import com.granttrack.disbursement.mapper.DisbursementMapper;
import com.granttrack.disbursement.repository.DisbursementMilestoneRepository;
import com.granttrack.disbursement.repository.FundDisbursementRepository;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.progress.entity.ProgressReport;
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
class DisbursementMilestoneServiceImplTest {

    private static final long PI = 2L;
    private static final long AWARD_ID = 1L;
    private static final long APP_ID = 5L;

    @Mock
    private DisbursementMilestoneRepository milestoneRepository;
    @Mock
    private FundDisbursementRepository disbursementRepository;
    @Mock
    private DisbursementMapper mapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;
    @Mock
    private ProgressReportRepository progressReportRepository;

    @InjectMocks
    private DisbursementMilestoneServiceImpl milestoneService;

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

    private GrantAward activeAward(String amount) {
        GrantAward a = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.ACTIVE).awardedAmount(new BigDecimal(amount)).build();
        a.setId(AWARD_ID);
        return a;
    }

    /** Wires award -> application -> PI so ownership checks resolve to PI. */
    private void wireOwnership() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                GrantApplication.builder().principalInvestigatorId(PI).build()));
    }

    private ProgressReport report(String status) {
        ProgressReport r = new ProgressReport();
        r.setMilestoneId(1L);
        r.setStatus(status);
        return r;
    }

    @Test
    void create_Success() {
        MilestoneRequest request = new MilestoneRequest(AWARD_ID, 1, "Milestone 1", LocalDate.now(), new BigDecimal("10000"), true);
        DisbursementMilestone milestone = DisbursementMilestone.builder().build();
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(milestoneRepository.existsByAwardIdAndMilestoneNumber(AWARD_ID, 1)).thenReturn(false);
        when(milestoneRepository.sumAmountByAwardId(AWARD_ID)).thenReturn(BigDecimal.ZERO);
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(milestone);
        when(mapper.toResponse(any(DisbursementMilestone.class), anyString())).thenReturn(response);

        assertNotNull(milestoneService.create(request));
        verify(milestoneRepository, times(1)).save(any(DisbursementMilestone.class));
    }

    @Test
    void create_AwardNotActive_ThrowsException() {
        MilestoneRequest request = new MilestoneRequest(AWARD_ID, 1, "M", LocalDate.now(), new BigDecimal("10000"), true);
        GrantAward award = GrantAward.builder().status(AwardStatus.SUSPENDED).awardedAmount(new BigDecimal("100000")).build();
        award.setId(AWARD_ID);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> milestoneService.create(request));
        verify(milestoneRepository, never()).save(any(DisbursementMilestone.class));
    }

    @Test
    void create_ExceedsAwardAmount_ThrowsException() {
        MilestoneRequest request = new MilestoneRequest(AWARD_ID, 2, "M", LocalDate.now(), new BigDecimal("5000"), true);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("10000")));
        when(milestoneRepository.existsByAwardIdAndMilestoneNumber(AWARD_ID, 2)).thenReturn(false);
        when(milestoneRepository.sumAmountByAwardId(AWARD_ID)).thenReturn(new BigDecimal("8000"));

        assertThrows(BusinessException.class, () -> milestoneService.create(request));
        verify(milestoneRepository, never()).save(any(DisbursementMilestone.class));
    }

    @Test
    void create_Duplicate_ThrowsException() {
        MilestoneRequest request = new MilestoneRequest(AWARD_ID, 1, "M", LocalDate.now(), new BigDecimal("10000"), true);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(milestoneRepository.existsByAwardIdAndMilestoneNumber(AWARD_ID, 1)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> milestoneService.create(request));
    }

    @Test
    void update_Success() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest("Updated", LocalDate.now(), new BigDecimal("15000"), false);
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).amount(new BigDecimal("10000")).status(MilestoneStatus.UPCOMING).build();
        milestone.setId(1L);
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(milestoneRepository.sumAmountByAwardId(AWARD_ID)).thenReturn(new BigDecimal("10000"));
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(milestone);
        when(mapper.toResponse(any(DisbursementMilestone.class), anyString())).thenReturn(response);

        assertNotNull(milestoneService.update(1L, request));
        verify(milestoneRepository, times(1)).save(any(DisbursementMilestone.class));
    }

    @Test
    void update_NotUpcoming_ThrowsException() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest("Updated", LocalDate.now(), new BigDecimal("15000"), false);
        DisbursementMilestone milestone = DisbursementMilestone.builder().status(MilestoneStatus.COMPLETED).build();
        milestone.setId(1L);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        assertThrows(BusinessException.class, () -> milestoneService.update(1L, request));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        DisbursementMilestone milestone = DisbursementMilestone.builder().awardId(AWARD_ID).build();
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        wireOwnership();
        when(mapper.toResponse(any(DisbursementMilestone.class), anyString())).thenReturn(response);

        assertNotNull(milestoneService.getById(1L));
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> milestoneService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        DisbursementMilestone milestone = DisbursementMilestone.builder().build();
        Page<DisbursementMilestone> page = new PageImpl<>(List.of(milestone));
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(APP_ID));
        when(awardRepository.findIdsByApplicationIdIn(List.of(APP_ID))).thenReturn(List.of(AWARD_ID));
        when(milestoneRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(DisbursementMilestone.class), anyString())).thenReturn(response);

        Page<MilestoneResponse> result = milestoneService.search(AWARD_ID, "UPCOMING", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void verify_ReportApproved_Success() {
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).milestoneNumber(1).status(MilestoneStatus.UPCOMING).build();
        milestone.setId(1L);
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(progressReportRepository.findTopByMilestoneIdOrderByIdDesc(1L)).thenReturn(Optional.of(report("APPROVED")));
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(milestone);
        when(mapper.toResponse(any(DisbursementMilestone.class), anyString())).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                GrantApplication.builder().principalInvestigatorId(PI).build()));

        assertNotNull(milestoneService.verify(1L));
        assertEquals(MilestoneStatus.COMPLETED, milestone.getStatus());
    }

    @Test
    void verify_NoReport_ThrowsException() {
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).milestoneNumber(1).status(MilestoneStatus.UPCOMING).build();
        milestone.setId(1L);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(progressReportRepository.findTopByMilestoneIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> milestoneService.verify(1L));
        assertEquals(MilestoneStatus.UPCOMING, milestone.getStatus());
    }

    @Test
    void verify_ReportNotApprovedByCompliance_ThrowsException() {
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).milestoneNumber(1).status(MilestoneStatus.UPCOMING).build();
        milestone.setId(1L);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(progressReportRepository.findTopByMilestoneIdOrderByIdDesc(1L)).thenReturn(Optional.of(report("SUBMITTED")));

        assertThrows(BusinessException.class, () -> milestoneService.verify(1L));
        assertEquals(MilestoneStatus.UPCOMING, milestone.getStatus());
    }

    @Test
    void verify_EarlierMilestoneNotDisbursed_ThrowsException() {
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).milestoneNumber(2).status(MilestoneStatus.UPCOMING).build();
        milestone.setId(2L);
        when(milestoneRepository.findById(2L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.countByAwardIdAndMilestoneNumberLessThanAndStatusNot(AWARD_ID, 2, MilestoneStatus.DISBURSED))
                .thenReturn(1L);

        assertThrows(BusinessException.class, () -> milestoneService.verify(2L));
        assertEquals(MilestoneStatus.UPCOMING, milestone.getStatus());
    }

    @Test
    void release_Completed_Success() {
        ReleaseFundsRequest request = new ReleaseFundsRequest("AccountRef123", "UTR-778812", LocalDate.now());
        DisbursementMilestone milestone = DisbursementMilestone.builder()
                .awardId(AWARD_ID).milestoneNumber(1).amount(new BigDecimal("10000")).status(MilestoneStatus.COMPLETED).build();
        milestone.setId(1L);
        FundDisbursement disbursement = FundDisbursement.builder().build();
        FundDisbursementResponse response = FundDisbursementResponse.builder().id(1L).build();

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(disbursementRepository.save(any(FundDisbursement.class))).thenReturn(disbursement);
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(milestone);
        when(mapper.toResponse(disbursement)).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward("100000")));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                GrantApplication.builder().principalInvestigatorId(PI).build()));

        assertNotNull(milestoneService.release(1L, request));
        assertEquals(MilestoneStatus.DISBURSED, milestone.getStatus());
    }

    @Test
    void release_NotCompleted_ThrowsException() {
        ReleaseFundsRequest request = new ReleaseFundsRequest("AccountRef123", "UTR-778812", LocalDate.now());
        DisbursementMilestone milestone = DisbursementMilestone.builder().status(MilestoneStatus.UPCOMING).build();
        milestone.setId(1L);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        assertThrows(BusinessException.class, () -> milestoneService.release(1L, request));
    }
}
