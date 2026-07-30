package com.granttrack.application.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CoInvestigatorResponse(
        Long id,
        Long applicationId,
        Long userId,
        Long institutionId,
        String role,
        String contribution,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
