package com.granttrack.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.application.dto.request.ApplicationBudgetRequest;
import com.granttrack.application.dto.request.CoInvestigatorRequest;
import com.granttrack.application.dto.request.GrantApplicationRequest;
import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.dto.response.BlindApplicationResponse;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.service.ApplicationBudgetService;
import com.granttrack.application.service.CoInvestigatorService;
import com.granttrack.application.service.GrantApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GrantApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GrantApplicationControllerTest {

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
    private GrantApplicationService applicationService;

    @MockBean
    private CoInvestigatorService coInvestigatorService;

    @MockBean
    private ApplicationBudgetService budgetService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void create_Success() throws Exception {
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 2L, "Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).projectTitle("Title").build();

        when(applicationService.create(any(GrantApplicationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectTitle").value("Title"));

        verify(applicationService, times(1)).create(any(GrantApplicationRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        GrantApplicationRequest request = new GrantApplicationRequest(
                1L, 2L, "Upd Title", "Abstract", "Science", new BigDecimal("10000"), 12, 1L);
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).projectTitle("Upd Title").build();

        when(applicationService.update(eq(1L), any(GrantApplicationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/applications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectTitle").value("Upd Title"));

        verify(applicationService, times(1)).update(eq(1L), any(GrantApplicationRequest.class));
    }

    @Test
    void get_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).projectTitle("Title").build();

        when(applicationService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/applications/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectTitle").value("Title"));

        verify(applicationService, times(1)).getById(1L);
    }

    @Test
    void getBlind_Success() throws Exception {
        BlindApplicationResponse response = BlindApplicationResponse.builder().id(1L).projectTitle("Title").build();

        when(applicationService.getBlindById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/applications/1/blind")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectTitle").value("Title"));

        verify(applicationService, times(1)).getBlindById(1L);
    }

    @Test
    void search_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).projectTitle("Title").build();
        Page<GrantApplicationResponse> page = new PageImpl<>(List.of(response));

        when(applicationService.search(anyString(), anyString(), eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/applications")
                        .param("q", "Title")
                        .param("status", "DRAFT")
                        .param("callId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].projectTitle").value("Title"));

        verify(applicationService, times(1)).search(eq("Title"), eq("DRAFT"), eq(1L), any(Pageable.class));
    }

    @Test
    void submit_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).status("SUBMITTED").build();

        when(applicationService.submit(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications/1/submit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        verify(applicationService, times(1)).submit(1L);
    }

    @Test
    void withdraw_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).status("WITHDRAWN").build();

        when(applicationService.withdraw(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        verify(applicationService, times(1)).withdraw(1L);
    }

    @Test
    void changeStatus_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).status("UNDER_REVIEW").build();

        when(applicationService.changeStatus(1L, "UNDER_REVIEW")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .param("status", "UNDER_REVIEW")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        verify(applicationService, times(1)).changeStatus(1L, "UNDER_REVIEW");
    }

    @Test
    void addCoInvestigator_Success() throws Exception {
        CoInvestigatorRequest request = new CoInvestigatorRequest(2L, 1L, "CO_INVESTIGATOR", "Contribution");
        CoInvestigatorResponse response = CoInvestigatorResponse.builder().id(1L).role("CO_INVESTIGATOR").build();

        when(coInvestigatorService.add(eq(1L), any(CoInvestigatorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications/1/co-investigators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("CO_INVESTIGATOR"));

        verify(coInvestigatorService, times(1)).add(eq(1L), any(CoInvestigatorRequest.class));
    }

    @Test
    void listCoInvestigators_Success() throws Exception {
        CoInvestigatorResponse response = CoInvestigatorResponse.builder().id(1L).role("CO_INVESTIGATOR").build();

        when(coInvestigatorService.listByApplication(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/applications/1/co-investigators")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].role").value("CO_INVESTIGATOR"));

        verify(coInvestigatorService, times(1)).listByApplication(1L);
    }

    @Test
    void addBudget_Success() throws Exception {
        ApplicationBudgetRequest request = new ApplicationBudgetRequest("PERSONNEL", new BigDecimal("5000"), "Justification");
        ApplicationBudgetResponse response = ApplicationBudgetResponse.builder().id(1L).budgetHead("PERSONNEL").build();

        when(budgetService.add(eq(1L), any(ApplicationBudgetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications/1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.budgetHead").value("PERSONNEL"));

        verify(budgetService, times(1)).add(eq(1L), any(ApplicationBudgetRequest.class));
    }

    @Test
    void listBudgets_Success() throws Exception {
        ApplicationBudgetResponse response = ApplicationBudgetResponse.builder().id(1L).budgetHead("PERSONNEL").build();

        when(budgetService.listByApplication(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/applications/1/budgets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].budgetHead").value("PERSONNEL"));

        verify(budgetService, times(1)).listByApplication(1L);
    }

    @Test
    void uploadAbstract_Success() throws Exception {
        GrantApplicationResponse response = GrantApplicationResponse.builder().id(1L).build();
        MockMultipartFile filePart = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        when(applicationService.uploadAbstract(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/applications/1/abstract-document")
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(applicationService, times(1)).uploadAbstract(eq(1L), any());
    }

    @Test
    void downloadAbstract_Success() throws Exception {
        GrantApplicationService.AbstractDocument doc = new GrantApplicationService.AbstractDocument(
                new ByteArrayResource("content".getBytes()), "doc.pdf");

        when(applicationService.downloadAbstract(1L)).thenReturn(doc);

        mockMvc.perform(get("/api/v1/applications/1/abstract-document")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"doc.pdf\""))
                .andExpect(content().bytes("content".getBytes()));

        verify(applicationService, times(1)).downloadAbstract(1L);
    }
}

