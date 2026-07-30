package com.granttrack.funding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.funding.dto.request.FundingSchemeRequest;
import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.service.FundingSchemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

@WebMvcTest(controllers = FundingSchemeController.class)
@AutoConfigureMockMvc(addFilters = false)
class FundingSchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FundingSchemeService schemeService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void create_Success() throws Exception {
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Scheme", 1L, "Area", "Cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                "Eligible", 12, LocalDate.now(), LocalDate.now().plusMonths(12),
                "Desc", "ACTIVE"
        );
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Scheme").build();

        when(schemeService.create(any(FundingSchemeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/schemes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schemeName").value("Scheme"));

        verify(schemeService, times(1)).create(any(FundingSchemeRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Upd Scheme", 1L, "Area", "Cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                "Eligible", 12, LocalDate.now(), LocalDate.now().plusMonths(12),
                "Desc", "ACTIVE"
        );
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Upd Scheme").build();

        when(schemeService.update(eq(1L), any(FundingSchemeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/funding/schemes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schemeName").value("Upd Scheme"));

        verify(schemeService, times(1)).update(eq(1L), any(FundingSchemeRequest.class));
    }

    @Test
    void get_Success() throws Exception {
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Scheme").build();

        when(schemeService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/funding/schemes/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schemeName").value("Scheme"));

        verify(schemeService, times(1)).getById(1L);
    }

    @Test
    void list_Success() throws Exception {
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Scheme").build();
        Page<FundingSchemeResponse> page = new PageImpl<>(List.of(response));

        when(schemeService.search(anyString(), anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/funding/schemes")
                        .param("q", "Scheme")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].schemeName").value("Scheme"));

        verify(schemeService, times(1)).search(eq("Scheme"), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    void changeStatus_Success() throws Exception {
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).status("INACTIVE").build();

        when(schemeService.changeStatus(1L, "INACTIVE")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/funding/schemes/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(schemeService, times(1)).changeStatus(1L, "INACTIVE");
    }

    @Test
    void uploadDocument_Success() throws Exception {
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).build();
        MockMultipartFile filePart = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        when(schemeService.uploadDocument(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/funding/schemes/1/document")
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(schemeService, times(1)).uploadDocument(eq(1L), any());
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/funding/schemes/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(schemeService, times(1)).delete(1L);
    }
}

