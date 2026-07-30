package com.granttrack.notification.service;

import com.granttrack.notification.entity.NotificationCategory;

/**
 * Finance-side notification facade. Disbursement code depends on this unchanged interface;
 * the implementation forwards to notification-service over Feign. Fire-and-forget: failures
 * never break the calling business transaction.
 */
public interface NotificationService {

    /** Publish a notification for a user. Best-effort — swallows delivery failures. */
    void notify(Long userId, String message, NotificationCategory category);
}
