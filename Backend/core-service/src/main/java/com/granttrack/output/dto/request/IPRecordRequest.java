package com.granttrack.output.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IPRecordRequest(
        @NotNull Long awardId,
        @NotBlank String ipType,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 1000) String inventors,
        LocalDate filingDate,
        LocalDate grantDate,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal ownershipPercent,
        String status
) {
}
