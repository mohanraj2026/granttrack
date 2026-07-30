package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.BusinessException;
import com.granttrack.funding.dto.request.GrantCallRequest;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.entity.SchemeStatus;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.FundingSchemeRepository;
import com.granttrack.funding.repository.GrantCallRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrantCallServiceImplTest {

    @Mock
    private GrantCallRepository callRepository;

    @Mock
    private FundingSchemeRepository schemeRepository;

    @Mock
    private FundingMapper mapper;

    @InjectMocks
    private GrantCallServiceImpl grantCallService;

    @Test
    void create_Success() {
        GrantCallRequest request = new GrantCallRequest(1L, "Call Title", LocalDate.now(), LocalDate.now().plusDays(30), null, null, "PANEL");
        FundingScheme scheme = FundingScheme.builder().build();
        scheme.setId(1L);
        GrantCall call = GrantCall.builder().callTitle("Call Title").build();
        call.setId(1L);
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Call Title").build();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(callRepository.save(any(GrantCall.class))).thenReturn(call);
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.create(request);

        assertNotNull(result);
        assertEquals("Call Title", result.callTitle());
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        GrantCallRequest request = new GrantCallRequest(1L, "Title", LocalDate.now(), LocalDate.now().minusDays(1), null, null, "PANEL");

        assertThrows(BusinessException.class, () -> grantCallService.create(request));
    }

    @Test
    void update_Success() {
        Long callId = 1L;
        GrantCallRequest request = new GrantCallRequest(1L, "Upd Title", LocalDate.now(), LocalDate.now().plusDays(30), null, null, "PANEL");
        GrantCall call = GrantCall.builder().callTitle("Title").status(CallStatus.UPCOMING).build();
        call.setId(callId);
        GrantCallResponse response = GrantCallResponse.builder().id(callId).callTitle("Upd Title").build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));
        when(callRepository.save(any(GrantCall.class))).thenReturn(call);
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.update(callId, request);

        assertNotNull(result);
        assertEquals("Upd Title", result.callTitle());
    }

    @Test
    void update_ClosedCall_ThrowsException() {
        Long callId = 1L;
        GrantCallRequest request = new GrantCallRequest(1L, "Upd Title", LocalDate.now(), LocalDate.now().plusDays(30), null, null, "PANEL");
        GrantCall call = GrantCall.builder().callTitle("Title").status(CallStatus.CLOSED).build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));

        assertThrows(BusinessException.class, () -> grantCallService.update(callId, request));
    }

    @Test
    void getById_Success() {
        Long callId = 1L;
        GrantCall call = GrantCall.builder().callTitle("Title").build();
        call.setId(callId);
        GrantCallResponse response = GrantCallResponse.builder().id(callId).callTitle("Title").build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.getById(callId);

        assertNotNull(result);
        assertEquals(callId, result.id());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        GrantCall call = GrantCall.builder().callTitle("Title").build();
        call.setId(1L);
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Title").build();
        Page<GrantCall> page = new PageImpl<>(List.of(call));

        when(callRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(call)).thenReturn(response);

        Page<GrantCallResponse> result = grantCallService.search("Title", "UPCOMING", 1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void open_Success() {
        Long callId = 1L;
        FundingScheme scheme = FundingScheme.builder().status(SchemeStatus.ACTIVE).build();
        scheme.setId(1L);
        GrantCall call = GrantCall.builder().status(CallStatus.UPCOMING).scheme(scheme)
                .closeDate(LocalDate.now().plusDays(30)).build();
        call.setId(callId);
        GrantCallResponse response = GrantCallResponse.builder().id(callId).status("OPEN").build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));
        when(callRepository.save(any(GrantCall.class))).thenReturn(call);
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.open(callId);

        assertNotNull(result);
        assertEquals("OPEN", result.status());
    }

    @Test
    void open_ExpiredCall_ThrowsException() {
        Long callId = 1L;
        FundingScheme scheme = FundingScheme.builder().status(SchemeStatus.ACTIVE).build();
        scheme.setId(1L);
        GrantCall call = GrantCall.builder().status(CallStatus.UPCOMING).scheme(scheme)
                .closeDate(LocalDate.now().minusDays(1)).build();
        call.setId(callId);

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));

        assertThrows(BusinessException.class, () -> grantCallService.open(callId));
        verify(callRepository, never()).save(any(GrantCall.class));
    }

    @Test
    void open_InactiveScheme_ThrowsException() {
        Long callId = 1L;
        FundingScheme scheme = FundingScheme.builder().status(SchemeStatus.SUSPENDED).build();
        scheme.setId(1L);
        GrantCall call = GrantCall.builder().status(CallStatus.UPCOMING).scheme(scheme)
                .closeDate(LocalDate.now().plusDays(30)).build();
        call.setId(callId);

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));

        assertThrows(BusinessException.class, () -> grantCallService.open(callId));
        verify(callRepository, never()).save(any(GrantCall.class));
    }

    @Test
    void terminate_ClosedCall_ThrowsException() {
        Long callId = 1L;
        GrantCall call = GrantCall.builder().status(CallStatus.CLOSED).build();
        call.setId(callId);

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));

        assertThrows(BusinessException.class, () -> grantCallService.terminate(callId));
        verify(callRepository, never()).save(any(GrantCall.class));
    }

    @Test
    void close_Success() {
        Long callId = 1L;
        GrantCall call = GrantCall.builder().status(CallStatus.OPEN).build();
        call.setId(callId);
        GrantCallResponse response = GrantCallResponse.builder().id(callId).status("CLOSED").build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));
        when(callRepository.save(any(GrantCall.class))).thenReturn(call);
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.close(callId);

        assertNotNull(result);
        assertEquals("CLOSED", result.status());
    }

    @Test
    void terminate_Success() {
        Long callId = 1L;
        GrantCall call = GrantCall.builder().status(CallStatus.OPEN).build();
        call.setId(callId);
        GrantCallResponse response = GrantCallResponse.builder().id(callId).status("TERMINATED").build();

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));
        when(callRepository.save(any(GrantCall.class))).thenReturn(call);
        when(mapper.toResponse(call)).thenReturn(response);

        GrantCallResponse result = grantCallService.terminate(callId);

        assertNotNull(result);
        assertEquals("TERMINATED", result.status());
    }

    @Test
    void delete_Success() {
        Long callId = 1L;
        GrantCall call = GrantCall.builder().build();
        call.setId(callId);
        call.setDeleted(false);

        when(callRepository.findById(callId)).thenReturn(Optional.of(call));

        grantCallService.delete(callId);

        assertTrue(call.isDeleted());
        verify(callRepository, times(1)).save(call);
    }
}
