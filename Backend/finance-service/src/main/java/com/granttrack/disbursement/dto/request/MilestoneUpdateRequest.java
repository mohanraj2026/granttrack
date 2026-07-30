package com.granttrack.disbursement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MilestoneUpdateRequest(
        @Size(max = 500) String description,
        LocalDate dueDate,
        @NotNull @Positive BigDecimal amount,
        Boolean evidenceRequired
) {
}
