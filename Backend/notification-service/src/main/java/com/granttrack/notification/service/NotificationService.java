package com.granttrack.notification.service;

import com.granttrack.notification.dto.request.NotificationRequest;
import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.NotificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationResponse> listForCurrentUser(String status, Pageable pageable);
    long unreadCountForCurrentUser();
    NotificationResponse markRead(Long id);
    NotificationResponse dismiss(Long id);
    NotificationResponse create(NotificationRequest request);

    /** Publish a notification programmatically from another module. */
    NotificationResponse notify(Long userId, String message, NotificationCategory category);
}
