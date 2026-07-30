package com.granttrack.notification.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponse(
        Long id,
        Long userId,
        String message,
        String category,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
