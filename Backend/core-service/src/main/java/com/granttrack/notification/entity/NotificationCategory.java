package com.granttrack.notification.entity;

/**
 * Notification categories used by core services when publishing notifications.
 * Kept in core (unchanged package) so award/review/progress/application callers are
 * untouched; the authoritative entity lives in notification-service.
 */
public enum NotificationCategory {
    APPLICATION,
    REVIEW,
    AWARD,
    DISBURSEMENT,
    PROGRESS,
    OUTPUT
}
