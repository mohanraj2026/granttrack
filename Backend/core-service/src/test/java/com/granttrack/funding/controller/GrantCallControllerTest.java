package com.granttrack.funding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.funding.dto.request.GrantCallRequest;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.service.GrantCallService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GrantCallController.class)
@AutoConfigureMockMvc(addFilters = false)
class GrantCallControllerTest {

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
    private GrantCallService grantCallService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void create_Success() throws Exception {
        GrantCallRequest request = new GrantCallRequest(1L, "Call Title", LocalDate.now(), LocalDate.now().plusDays(30), null, null, "PANEL");
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Call Title").build();

        when(grantCallService.create(any(GrantCallRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.callTitle").value("Call Title"));

        verify(grantCallService, times(1)).create(any(GrantCallRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        GrantCallRequest request = new GrantCallRequest(1L, "Upd Title", LocalDate.now(), LocalDate.now().plusDays(30), null, null, "PANEL");
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Upd Title").build();

        when(grantCallService.update(eq(1L), any(GrantCallRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/funding/calls/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.callTitle").value("Upd Title"));

        verify(grantCallService, times(1)).update(eq(1L), any(GrantCallRequest.class));
    }

    @Test
    void get_Success() throws Exception {
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Title").build();

        when(grantCallService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/funding/calls/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.callTitle").value("Title"));

        verify(grantCallService, times(1)).getById(1L);
    }

    @Test
    void list_Success() throws Exception {
        GrantCallResponse response = GrantCallResponse.builder().id(1L).callTitle("Title").build();
        Page<GrantCallResponse> page = new PageImpl<>(List.of(response));

        when(grantCallService.search(anyString(), anyString(), eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/funding/calls")
                        .param("q", "Title")
                        .param("status", "UPCOMING")
                        .param("schemeId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].callTitle").value("Title"));

        verify(grantCallService, times(1)).search(eq("Title"), eq("UPCOMING"), eq(1L), any(Pageable.class));
    }

    @Test
    void open_Success() throws Exception {
        GrantCallResponse response = GrantCallResponse.builder().id(1L).status("OPEN").build();

        when(grantCallService.open(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/calls/1/open")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(grantCallService, times(1)).open(1L);
    }

    @Test
    void close_Success() throws Exception {
        GrantCallResponse response = GrantCallResponse.builder().id(1L).status("CLOSED").build();

        when(grantCallService.close(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/calls/1/close")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        verify(grantCallService, times(1)).close(1L);
    }

    @Test
    void terminate_Success() throws Exception {
        GrantCallResponse response = GrantCallResponse.builder().id(1L).status("TERMINATED").build();

        when(grantCallService.terminate(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/funding/calls/1/terminate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("TERMINATED"));

        verify(grantCallService, times(1)).terminate(1L);
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/funding/calls/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(grantCallService, times(1)).delete(1L);
    }
}

