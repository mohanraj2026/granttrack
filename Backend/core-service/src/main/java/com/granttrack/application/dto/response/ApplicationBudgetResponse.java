package com.granttrack.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ApplicationBudgetResponse(
        Long id,
        Long applicationId,
        String budgetHead,
        BigDecimal amount,
        String justification,
        Instant createdAt,
        Instant updatedAt
) {
}
