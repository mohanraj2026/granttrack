package com.granttrack.auth.dto.response;

import lombok.Builder;

import java.time.Instant;

/** A single append-only audit-trail entry, exposed to Compliance / Admin for oversight. */
@Builder
public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String entityType,
        Long recordId,
        String details,
        Instant timestamp
) {
}
