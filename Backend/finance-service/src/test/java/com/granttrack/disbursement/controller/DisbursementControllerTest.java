package com.granttrack.disbursement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.service.DisbursementMilestoneService;
import com.granttrack.disbursement.service.FundDisbursementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DisbursementController.class)
@AutoConfigureMockMvc(addFilters = false)
class DisbursementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisbursementMilestoneService milestoneService;

    @MockBean
    private FundDisbursementService disbursementService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void createMilestone_Success() throws Exception {
        MilestoneRequest request = new MilestoneRequest(1L, 1, "Milestone 1", LocalDate.now(), new BigDecimal("10000"), true);
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneService.create(any(MilestoneRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/disbursements/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(milestoneService, times(1)).create(any(MilestoneRequest.class));
    }

    @Test
    void updateMilestone_Success() throws Exception {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest("Updated", LocalDate.now(), new BigDecimal("15000"), false);
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneService.update(eq(1L), any(MilestoneUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/disbursements/milestones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(milestoneService, times(1)).update(eq(1L), any(MilestoneUpdateRequest.class));
    }

    @Test
    void searchMilestones_Success() throws Exception {
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();
        when(milestoneService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/disbursements/milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(milestoneService, times(1)).search(any(), any(), any(Pageable.class));
    }

    @Test
    void getMilestone_Success() throws Exception {
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/disbursements/milestones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(milestoneService, times(1)).getById(1L);
    }

    @Test
    void verifyMilestone_Success() throws Exception {
        MilestoneResponse response = MilestoneResponse.builder().id(1L).build();

        when(milestoneService.verify(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/disbursements/milestones/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(milestoneService, times(1)).verify(1L);
    }

    @Test
    void release_Success() throws Exception {
        ReleaseFundsRequest request = new ReleaseFundsRequest("AccountRef123", "UTR-1", LocalDate.now());
        FundDisbursementResponse response = FundDisbursementResponse.builder().id(1L).build();

        when(milestoneService.release(eq(1L), any(ReleaseFundsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/disbursements/milestones/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(milestoneService, times(1)).release(eq(1L), any(ReleaseFundsRequest.class));
    }

    @Test
    void searchDisbursements_Success() throws Exception {
        FundDisbursementResponse response = FundDisbursementResponse.builder().id(1L).build();
        when(disbursementService.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/disbursements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(disbursementService, times(1)).search(any(), any(), any(), any(Pageable.class));
    }
}
