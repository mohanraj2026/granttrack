package com.granttrack.auth.dto.response;

import lombok.Builder;

/** Issued on login / refresh. */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}
