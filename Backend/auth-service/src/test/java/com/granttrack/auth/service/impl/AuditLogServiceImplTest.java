package com.granttrack.auth.service.impl;

import com.granttrack.auth.dto.response.AuditLogResponse;
import com.granttrack.common.audit.AuditLog;
import com.granttrack.common.audit.AuditLogRepository;
import com.granttrack.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @SuppressWarnings("unchecked")
    void search_Filtered_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = AuditLog.builder()
                .id(1L).userId(7L).action("RELEASE_FUNDS").entityType("FundDisbursement")
                .recordId(42L).details(null).timestamp(Instant.parse("2027-01-01T00:00:00Z"))
                .build();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.search(
                7L, "FundDisbursement", "RELEASE_FUNDS", 42L,
                "2027-01-01T00:00:00Z", "2027-12-31T23:59:59Z", pageable);

        assertEquals(1, result.getTotalElements());
        AuditLogResponse response = result.getContent().get(0);
        assertEquals("RELEASE_FUNDS", response.action());
        assertEquals(7L, response.userId());
        assertEquals(42L, response.recordId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_NoFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().id(1L).action("A").entityType("E").timestamp(Instant.now()).build()));
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.search(null, null, null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void search_InvalidFromTimestamp_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(BusinessException.class, () -> auditLogService.search(
                null, null, null, null, "not-a-date", null, pageable));
        verify(auditLogRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(), eq(pageable));
    }
}
