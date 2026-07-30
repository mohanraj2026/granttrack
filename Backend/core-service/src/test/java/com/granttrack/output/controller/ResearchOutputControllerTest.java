package com.granttrack.output.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.output.dto.request.ResearchOutputRequest;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.service.ResearchOutputService;
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

@WebMvcTest(controllers = ResearchOutputController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResearchOutputControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResearchOutputService outputService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void create_Success() throws Exception {
        ResearchOutputRequest request = new ResearchOutputRequest(1L, "JOURNAL_ARTICLE", "Title", "Authors", "Venue", "DOI", LocalDate.now(), true, "PUBLISHED");
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();

        when(outputService.create(any(ResearchOutputRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/outputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(outputService, times(1)).create(any(ResearchOutputRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        ResearchOutputRequest request = new ResearchOutputRequest(1L, "JOURNAL_ARTICLE", "Title", "Authors", "Venue", "DOI", LocalDate.now(), true, "PUBLISHED");
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();

        when(outputService.update(eq(1L), any(ResearchOutputRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/outputs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(outputService, times(1)).update(eq(1L), any(ResearchOutputRequest.class));
    }

    @Test
    void getById_Success() throws Exception {
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();

        when(outputService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/outputs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(outputService, times(1)).getById(1L);
    }

    @Test
    void search_Success() throws Exception {
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputService.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/outputs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(outputService, times(1)).search(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/outputs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(outputService, times(1)).delete(1L);
    }
}
