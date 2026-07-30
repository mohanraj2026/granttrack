package com.granttrack.review.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Blind-review projection of a {@link com.granttrack.review.entity.ReviewerAssignment}.
 *
 * <p>To preserve review anonymity this DTO exposes only opaque identifiers
 * ({@code applicationId}, {@code reviewerId}). The Principal Investigator's identity
 * (and any other PI personal-identity field) is never exposed to reviewers.</p>
 */
@Builder
public record ReviewerAssignmentResponse(
        Long id,
        Long applicationId,
        Long reviewerId,
        LocalDate assignedDate,
        LocalDate reviewDeadline,
        String conflictScreeningStatus,
        String status,
        String responseComment,
        Instant createdAt,
        Instant updatedAt
) {
}
