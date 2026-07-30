package com.granttrack.progress.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.progress.dto.request.DeliverableRequest;
import com.granttrack.progress.dto.response.DeliverableResponse;
import org.springframework.mock.web.MockMultipartFile;
import com.granttrack.progress.service.DeliverableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeliverableController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliverableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliverableService deliverableService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void create_Success() throws Exception {
        DeliverableRequest request = new DeliverableRequest(1L, "Title", "REPORT", LocalDate.now());
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();

        when(deliverableService.create(any(DeliverableRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/progress/deliverables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(deliverableService, times(1)).create(any(DeliverableRequest.class));
    }

    @Test
    void upload_Success() throws Exception {
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("file", "deliverable.pdf", "application/pdf", "data".getBytes());

        when(deliverableService.upload(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/progress/deliverables/1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(deliverableService, times(1)).upload(eq(1L), any());
    }

    @Test
    void review_Success() throws Exception {
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();

        when(deliverableService.review(eq(1L), eq("ACCEPT"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/progress/deliverables/1/review")
                        .param("decision", "ACCEPT")
                        .param("comment", "Looks good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(deliverableService, times(1)).review(eq(1L), eq("ACCEPT"), any());
    }

    @Test
    void getById_Success() throws Exception {
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();

        when(deliverableService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/progress/deliverables/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(deliverableService, times(1)).getById(1L);
    }

    @Test
    void search_Success() throws Exception {
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/progress/deliverables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(deliverableService, times(1)).search(any(), any(), any(Pageable.class));
    }
}
