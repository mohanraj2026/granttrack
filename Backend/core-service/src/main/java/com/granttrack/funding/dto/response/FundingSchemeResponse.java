package com.granttrack.funding.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record FundingSchemeResponse(
        Long id,
        String schemeCode,
        String schemeName,
        Long sponsorId,
        String sponsorName,
        String researchArea,
        String category,
        BigDecimal maxAwardAmount,
        BigDecimal minAwardAmount,
        String eligibleApplicants,
        Integer fundingDurationMonths,
        LocalDate fromDate,
        LocalDate toDate,
        String description,
        String documentPath,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
