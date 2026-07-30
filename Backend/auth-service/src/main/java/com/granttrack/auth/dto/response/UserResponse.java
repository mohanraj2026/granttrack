package com.granttrack.auth.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/** Public projection of a {@link com.granttrack.auth.entity.User}. Never exposes the password. */
@Builder
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String countryCode,
        Long institutionId,
        String department,
        String education,
        String collegeIdPath,
        String profilePhotoPath,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public String getFormattedId() {
        return id != null ? String.format("GTU%04d", id) : null;
    }
}
