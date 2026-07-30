package com.granttrack.funding.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record GrantCallResponse(
        Long id,
        Long schemeId,
        String schemeName,
        String schemeCategory,
        String eligibleApplicants,
        Integer fundingDurationMonths,
        String schemeDocumentPath,
        BigDecimal schemeMaxAwardAmount,
        String callTitle,
        LocalDate openDate,
        LocalDate closeDate,
        Integer expectedAwards,
        BigDecimal totalBudgetAllocated,

        String reviewMethod,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
