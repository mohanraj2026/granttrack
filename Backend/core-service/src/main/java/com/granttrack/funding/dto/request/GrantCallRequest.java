package com.granttrack.funding.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrantCallRequest(
        @NotNull Long schemeId,
        @NotBlank @Size(max = 250) String callTitle,
        @NotNull LocalDate openDate,
        @NotNull LocalDate closeDate,

        @Positive Integer expectedAwards,

        @DecimalMin("0.0") BigDecimal totalBudgetAllocated,

        @NotNull String reviewMethod
) {
}
