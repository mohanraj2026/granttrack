package com.granttrack.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 1000) String message,
        @NotNull String category
) {
}
