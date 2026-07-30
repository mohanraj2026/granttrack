package com.granttrack.notification.client;

/**
 * Body of the internal notification-publish call. Mirrors notification-service's
 * {@code NotificationRequest} JSON shape ({@code userId}, {@code message}, {@code category}).
 */
public record NotificationPublishRequest(Long userId, String message, String category) {
}
