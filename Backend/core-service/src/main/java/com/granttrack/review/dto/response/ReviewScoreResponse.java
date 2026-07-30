package com.granttrack.review.dto.response;

import lombok.Builder;

import java.time.Instant;

/**
 * Blind-review projection of a {@link com.granttrack.review.entity.ReviewScore}.
 *
 * <p>Exposes only the owning {@code assignmentId} as an opaque identifier; no reviewer
 * or PI personal-identity field is included. The Principal Investigator's identity is
 * never exposed to reviewers.</p>
 */
@Builder
public record ReviewScoreResponse(
        Long id,
        Long assignmentId,
        String criterion,
        Integer score,
        String comments,
        String overallRecommendation,
        Instant submittedDate,
        Instant createdAt,
        Instant updatedAt
) {
}
