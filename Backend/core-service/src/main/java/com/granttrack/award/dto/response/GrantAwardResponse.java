package com.granttrack.award.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record GrantAwardResponse(
        Long id,
        Long applicationId,
        BigDecimal awardedAmount,
        LocalDate startDate,
        LocalDate endDate,
        String conditionsRef,
        LocalDate awardLetterDate,
        String status,
        Long financeOfficerId,
        String financeReviewStatus,
        String financeReviewComment,
        Instant createdAt,
        Instant updatedAt
) {
}
