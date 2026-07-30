package com.granttrack.notification.entity;

/**
 * Notification categories used when finance publishes notifications. Kept in the unchanged
 * package so the moved disbursement code is untouched; the authoritative entity lives in
 * notification-service.
 */
public enum NotificationCategory {
    APPLICATION,
    REVIEW,
    AWARD,
    DISBURSEMENT,
    PROGRESS,
    OUTPUT
}
