package com.granttrack.award.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.award.dto.request.AwardTermsRequest;
import com.granttrack.award.dto.request.GrantAwardRequest;
import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.service.GrantAwardService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GrantAwardController.class)
@AutoConfigureMockMvc(addFilters = false)
class GrantAwardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GrantAwardService awardService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @BeforeEach
    void setUp() {
    }

    @Test
    void create_Success() throws Exception {
        GrantAwardRequest request = new GrantAwardRequest(
                1L, new BigDecimal("50000"), LocalDate.now(), LocalDate.now().plusYears(1), "conditions.pdf", LocalDate.now());
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardService.create(any(GrantAwardRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/awards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(awardService, times(1)).create(any(GrantAwardRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        AwardTermsRequest request = new AwardTermsRequest(
                new BigDecimal("60000"), LocalDate.now(), LocalDate.now().plusYears(2), "new-conditions.pdf");
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardService.update(eq(1L), any(AwardTermsRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/awards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(awardService, times(1)).update(eq(1L), any(AwardTermsRequest.class));
    }

    @Test
    void approve_Success() throws Exception {
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardService.approve(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/awards/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(awardService, times(1)).approve(1L);
    }

    @Test
    void changeStatus_Success() throws Exception {
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardService.changeStatus(eq(1L), anyString())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/awards/1/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(awardService, times(1)).changeStatus(1L, "ACTIVE");
    }

    @Test
    void get_Success() throws Exception {
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();

        when(awardService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/awards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(awardService, times(1)).getById(1L);
    }

    @Test
    void search_Success() throws Exception {
        GrantAwardResponse response = GrantAwardResponse.builder().id(1L).build();
        when(awardService.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/awards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(awardService, times(1)).search(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void delete_Success() throws Exception {
        doNothing().when(awardService).delete(1L);

        mockMvc.perform(delete("/api/v1/awards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(awardService, times(1)).delete(1L);
    }
}
