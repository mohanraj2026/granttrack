package com.granttrack.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.notification.dto.request.NotificationRequest;
import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void list_Success() throws Exception {
        NotificationResponse response = NotificationResponse.builder().id(1L).build();
        when(notificationService.listForCurrentUser(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("status", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(notificationService, times(1)).listForCurrentUser(anyString(), any(Pageable.class));
    }

    @Test
    void unreadCount_Success() throws Exception {
        when(notificationService.unreadCountForCurrentUser()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5));

        verify(notificationService, times(1)).unreadCountForCurrentUser();
    }

    @Test
    void markRead_Success() throws Exception {
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationService.markRead(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).markRead(1L);
    }

    @Test
    void dismiss_Success() throws Exception {
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationService.dismiss(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/1/dismiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).dismiss(1L);
    }

    @Test
    void create_Success() throws Exception {
        NotificationRequest request = new NotificationRequest(1L, "Message", "AWARD");
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationService.create(any(NotificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(notificationService, times(1)).create(any(NotificationRequest.class));
    }
}
