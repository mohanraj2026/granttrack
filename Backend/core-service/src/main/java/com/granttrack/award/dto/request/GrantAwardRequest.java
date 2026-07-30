package com.granttrack.award.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrantAwardRequest(
        @NotNull Long applicationId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal awardedAmount,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 255) String conditionsRef,
        LocalDate awardLetterDate
) {
}
