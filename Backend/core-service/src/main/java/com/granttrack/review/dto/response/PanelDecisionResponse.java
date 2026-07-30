package com.granttrack.review.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record PanelDecisionResponse(
        Long id,
        Long applicationId,
        LocalDate panelDate,
        BigDecimal consensusScore,
        String awardDecision,
        BigDecimal awardedAmount,
        String conditionsAttached,
        Long decidedById,
        Long financeOfficerId,
        Instant createdAt,
        Instant updatedAt
) {
}
