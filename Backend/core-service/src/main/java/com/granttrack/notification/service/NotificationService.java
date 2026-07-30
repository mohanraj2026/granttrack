package com.granttrack.notification.service;

import com.granttrack.notification.entity.NotificationCategory;

/**
 * Core-side notification facade. Business services depend on this unchanged interface;
 * the implementation now forwards to notification-service over Feign instead of writing
 * the {@code notifications} table directly. Fire-and-forget: failures never break the
 * calling business transaction.
 */
public interface NotificationService {

    /** Publish a notification for a user. Best-effort — swallows delivery failures. */
    void notify(Long userId, String message, NotificationCategory category);
}
