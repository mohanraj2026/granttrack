package com.granttrack.progress.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.progress.dto.request.ProgressReportRequest;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.service.ProgressReportService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProgressReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProgressReportService reportService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void create_Success() throws Exception {
        ProgressReportRequest request = new ProgressReportRequest(1L, "Q1", "Summary", "Achievements", "Challenges", new BigDecimal("50"));
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();

        when(reportService.create(any(ProgressReportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/progress/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(reportService, times(1)).create(any(ProgressReportRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        ProgressReportRequest request = new ProgressReportRequest(1L, "Q1", "Summary", "Achievements", "Challenges", new BigDecimal("50"));
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();

        when(reportService.update(eq(1L), any(ProgressReportRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/progress/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(reportService, times(1)).update(eq(1L), any(ProgressReportRequest.class));
    }

    @Test
    void submit_Success() throws Exception {
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();

        when(reportService.submit(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/progress/reports/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(reportService, times(1)).submit(1L);
    }

    @Test
    void review_Success() throws Exception {
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();

        when(reportService.review(eq(1L), eq("APPROVE"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/progress/reports/1/review")
                        .param("decision", "APPROVE")
                        .param("comment", "Well documented"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(reportService, times(1)).review(eq(1L), eq("APPROVE"), any());
    }

    @Test
    void getById_Success() throws Exception {
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();

        when(reportService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/progress/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(reportService, times(1)).getById(1L);
    }

    @Test
    void search_Success() throws Exception {
        ProgressReportResponse response = ProgressReportResponse.builder().id(1L).build();
        when(reportService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/progress/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(reportService, times(1)).search(any(), any(), any(Pageable.class));
    }
}
