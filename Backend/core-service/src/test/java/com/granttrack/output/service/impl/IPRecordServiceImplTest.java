package com.granttrack.output.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.output.dto.request.IPRecordRequest;
import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.entity.IPRecord;
import com.granttrack.output.mapper.OutputMapper;
import com.granttrack.output.repository.IPRecordRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IPRecordServiceImplTest {

    private static final long PI = 100L;
    private static final long AWARD_ID = 1L;
    private static final long APP_ID = 1L;

    @Mock
    private IPRecordRepository ipRecordRepository;
    @Mock
    private OutputMapper mapper;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;

    @InjectMocks
    private IPRecordServiceImpl ipRecordService;

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

    private void asAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ROLE_ADMIN")).thenReturn(true);
    }

    private GrantAward award() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.COMPLETED).build();
        award.setId(AWARD_ID);
        return award;
    }

    private GrantApplication piApp() {
        return GrantApplication.builder().principalInvestigatorId(PI).build();
    }

    private void wireOwnership() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));
    }

    private IPRecordRequest request(LocalDate filing, LocalDate grant) {
        return new IPRecordRequest(AWARD_ID, "PATENT", "Title", "Inventors", filing, grant, new BigDecimal("100"), "FILED");
    }

    @Test
    void create_Success() {
        loginAs(PI);
        wireOwnership();
        IPRecord record = IPRecord.builder().build();
        IPRecordResponse response = IPRecordResponse.builder().id(1L).build();
        when(ipRecordRepository.save(any(IPRecord.class))).thenReturn(record);
        when(mapper.toResponse(record)).thenReturn(response);

        assertNotNull(ipRecordService.create(request(LocalDate.now(), LocalDate.now().plusDays(1))));
        verify(ipRecordRepository, times(1)).save(any(IPRecord.class));
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        // Date validation runs before award/ownership lookup.
        assertThrows(BusinessException.class,
                () -> ipRecordService.create(request(LocalDate.now(), LocalDate.now().minusDays(1))));
        verify(ipRecordRepository, never()).save(any(IPRecord.class));
    }

    @Test
    void create_AwardNotFound_ThrowsException() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ipRecordService.create(request(LocalDate.now(), LocalDate.now().plusDays(1))));
        verify(ipRecordRepository, never()).save(any(IPRecord.class));
    }

    @Test
    void create_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();

        assertThrows(AccessDeniedException.class,
                () -> ipRecordService.create(request(LocalDate.now(), LocalDate.now().plusDays(1))));
        verify(ipRecordRepository, never()).save(any(IPRecord.class));
    }

    @Test
    void update_Success() {
        loginAs(PI);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        IPRecordResponse response = IPRecordResponse.builder().id(1L).build();
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(ipRecordRepository.save(any(IPRecord.class))).thenReturn(record);
        when(mapper.toResponse(record)).thenReturn(response);

        assertNotNull(ipRecordService.update(1L, request(LocalDate.now(), LocalDate.now().plusDays(1))));
        verify(ipRecordRepository, times(1)).save(any(IPRecord.class));
    }

    @Test
    void update_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class,
                () -> ipRecordService.update(1L, request(LocalDate.now(), LocalDate.now().plusDays(1))));
        verify(ipRecordRepository, never()).save(any(IPRecord.class));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        IPRecordResponse response = IPRecordResponse.builder().id(1L).build();
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(mapper.toResponse(record)).thenReturn(response);

        assertNotNull(ipRecordService.getById(1L));
    }

    @Test
    void getById_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class, () -> ipRecordService.getById(1L));
    }

    @Test
    void getById_Staff_Success() {
        asStaff();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        IPRecordResponse response = IPRecordResponse.builder().id(1L).build();
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(mapper.toResponse(record)).thenReturn(response);

        assertNotNull(ipRecordService.getById(1L));
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ipRecordService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        IPRecord record = IPRecord.builder().build();
        Page<IPRecord> page = new PageImpl<>(List.of(record));
        IPRecordResponse response = IPRecordResponse.builder().id(1L).build();
        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(APP_ID));
        when(awardRepository.findIdsByApplicationIdIn(List.of(APP_ID))).thenReturn(List.of(AWARD_ID));
        when(ipRecordRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(IPRecord.class))).thenReturn(response);

        Page<IPRecordResponse> result = ipRecordService.list(AWARD_ID, "FILED", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void delete_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(ipRecordRepository.save(any(IPRecord.class))).thenReturn(record);

        ipRecordService.delete(1L);

        assertEquals(true, record.isDeleted());
        verify(ipRecordRepository, times(1)).save(any(IPRecord.class));
    }

    @Test
    void delete_Admin_Success() {
        asAdmin();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(ipRecordRepository.save(any(IPRecord.class))).thenReturn(record);

        ipRecordService.delete(1L);

        verify(ipRecordRepository, times(1)).save(any(IPRecord.class));
    }

    @Test
    void delete_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        IPRecord record = IPRecord.builder().awardId(AWARD_ID).build();
        record.setId(1L);
        when(ipRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class, () -> ipRecordService.delete(1L));
        verify(ipRecordRepository, never()).save(any(IPRecord.class));
    }
}
