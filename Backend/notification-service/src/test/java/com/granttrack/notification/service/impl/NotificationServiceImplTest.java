package com.granttrack.notification.service.impl;

import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.dto.request.NotificationRequest;
import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.Notification;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.entity.NotificationStatus;
import com.granttrack.notification.mapper.NotificationMapper;
import com.granttrack.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper mapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listForCurrentUser_Success() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = Notification.builder().userId(1L).build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(Notification.class))).thenReturn(response);

        Page<NotificationResponse> result = notificationService.listForCurrentUser("UNREAD", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void unreadCountForCurrentUser_Success() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.unreadCountForCurrentUser();

        assertEquals(5L, count);
    }

    @Test
    void markRead_Success() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        Notification notification = Notification.builder().userId(1L).status(NotificationStatus.UNREAD).build();
        notification.setId(1L);
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.markRead(1L);

        assertNotNull(result);
        assertEquals(NotificationStatus.READ, notification.getStatus());
    }

    @Test
    void markRead_NotOwned_ThrowsAccessDenied() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        Notification notification = Notification.builder().userId(2L).status(NotificationStatus.UNREAD).build();
        notification.setId(1L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> notificationService.markRead(1L));
    }

    @Test
    void dismiss_Success() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        Notification notification = Notification.builder().userId(1L).status(NotificationStatus.UNREAD).build();
        notification.setId(1L);
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.dismiss(1L);

        assertNotNull(result);
        assertEquals(NotificationStatus.DISMISSED, notification.getStatus());
    }

    @Test
    void create_Success() {
        NotificationRequest request = new NotificationRequest(1L, "Message", "AWARD");
        Notification notification = Notification.builder().userId(1L).build();
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.create(request);

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notify_Success() {
        Notification notification = Notification.builder().userId(1L).build();
        NotificationResponse response = NotificationResponse.builder().id(1L).build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.notify(1L, "Message", NotificationCategory.AWARD);

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
