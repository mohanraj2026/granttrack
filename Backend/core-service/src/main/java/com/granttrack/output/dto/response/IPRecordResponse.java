package com.granttrack.output.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record IPRecordResponse(
        Long id,
        Long awardId,
        String ipType,
        String title,
        String inventors,
        LocalDate filingDate,
        LocalDate grantDate,
        BigDecimal ownershipPercent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
