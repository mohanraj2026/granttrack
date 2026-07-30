package com.granttrack.funding.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FundingSchemeRequest(
        @NotBlank @Size(max = 200) String schemeName,
        @NotNull Long sponsorId,
        @NotBlank @Size(max = 200) String researchArea,
        @NotBlank @Size(max = 100) String category,
        @NotNull @DecimalMin("0.0") BigDecimal maxAwardAmount,
        @NotNull @DecimalMin("0.0") BigDecimal minAwardAmount,
        @NotBlank @Size(max = 500) String eligibleApplicants,
        @Positive Integer fundingDurationMonths,
        LocalDate fromDate,
        LocalDate toDate,
        String description,
        String status
) {
}
