package com.granttrack.review.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReviewerAssignmentRequest(
        @NotNull Long applicationId,
        @NotNull Long reviewerId,
        LocalDate reviewDeadline
) {
}
