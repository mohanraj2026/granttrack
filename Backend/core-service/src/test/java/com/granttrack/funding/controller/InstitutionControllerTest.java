package com.granttrack.funding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.funding.dto.request.InstitutionRequest;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.service.InstitutionService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InstitutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InstitutionControllerTest {

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
    private InstitutionService institutionService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void create_Success() throws Exception {
        InstitutionRequest request = new InstitutionRequest("Name", "Type", "US", "Uni", "Addr", "City", "State", "123", "999", "e@t.com");
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Name").build();

        when(institutionService.create(any(InstitutionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Name"));

        verify(institutionService, times(1)).create(any(InstitutionRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        InstitutionRequest request = new InstitutionRequest("Upd", "Type", "US", "Uni", "Addr", "City", "State", "123", "999", "e@t.com");
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Upd").build();

        when(institutionService.update(eq(1L), any(InstitutionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/funding/institutions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Upd"));

        verify(institutionService, times(1)).update(eq(1L), any(InstitutionRequest.class));
    }

    @Test
    void get_Success() throws Exception {
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Name").build();

        when(institutionService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/funding/institutions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Name"));

        verify(institutionService, times(1)).getById(1L);
    }

    @Test
    void list_Success() throws Exception {
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Name").build();
        Page<InstitutionResponse> page = new PageImpl<>(List.of(response));

        when(institutionService.list(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/funding/institutions")
                        .param("q", "Name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Name"));

        verify(institutionService, times(1)).list(eq("Name"), any(Pageable.class));
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/funding/institutions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(institutionService, times(1)).delete(1L);
    }
}

