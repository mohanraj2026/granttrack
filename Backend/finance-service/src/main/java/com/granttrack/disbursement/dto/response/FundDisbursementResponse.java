package com.granttrack.disbursement.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record FundDisbursementResponse(
        Long id,
        Long milestoneId,
        String milestoneDescription,
        Long awardId,
        BigDecimal amount,
        LocalDate disbursedDate,
        String receivingAccountRef,
        String paymentReference,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
