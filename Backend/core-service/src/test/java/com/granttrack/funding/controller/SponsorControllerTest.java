package com.granttrack.funding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.funding.dto.request.SponsorRequest;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.service.SponsorService;
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

@WebMvcTest(controllers = SponsorController.class)
@AutoConfigureMockMvc(addFilters = false)
class SponsorControllerTest {

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
    private SponsorService sponsorService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        // Since we disabled filters with addFilters = false, we inject a dummy principal manually just in case
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void create_Success() throws Exception {
        SponsorRequest request = new SponsorRequest("Name", "Type", "email@test.com", "1234567890", "Add", "Web");
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Name").build();

        when(sponsorService.create(any(SponsorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Name"));

        verify(sponsorService, times(1)).create(any(SponsorRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        SponsorRequest request = new SponsorRequest("Upd", "Type", "email@test.com", "1234567890", "Add", "Web");
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Upd").build();

        when(sponsorService.update(eq(1L), any(SponsorRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/funding/sponsors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Upd"));

        verify(sponsorService, times(1)).update(eq(1L), any(SponsorRequest.class));
    }

    @Test
    void get_Success() throws Exception {
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Name").build();

        when(sponsorService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/funding/sponsors/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Name"));

        verify(sponsorService, times(1)).getById(1L);
    }

    @Test
    void list_Success() throws Exception {
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Name").build();
        Page<SponsorResponse> page = new PageImpl<>(List.of(response));

        when(sponsorService.list(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/funding/sponsors")
                        .param("q", "Name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Name"));

        verify(sponsorService, times(1)).list(eq("Name"), any(Pageable.class));
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/funding/sponsors/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sponsorService, times(1)).delete(1L);
    }
}

