package com.granttrack.notification.service.impl;

import com.granttrack.notification.client.NotificationClient;
import com.granttrack.notification.client.NotificationPublishRequest;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Forwards notifications to notification-service via Feign. Deliberately best-effort:
 * a notification-service outage must never roll back or fail the business operation that
 * triggered the notification, so all delivery failures are logged and swallowed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationClient notificationClient;

    @Value("${granttrack.internal.token:}")
    private String internalToken;

    @Override
    public void notify(Long userId, String message, NotificationCategory category) {
        try {
            notificationClient.publish(internalToken,
                    new NotificationPublishRequest(userId, message, category.name()));
        } catch (Exception ex) {
            log.warn("Failed to publish notification to user {} (category {}): {}",
                    userId, category, ex.getMessage());
        }
    }
}
