package com.granttrack.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PanelDecisionRequest(
        LocalDate panelDate,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal consensusScore,
        @NotNull String awardDecision,
        @DecimalMin("0.0") BigDecimal awardedAmount,
        @Size(max = 1000) String conditionsAttached,
        /** Required for an award decision — the Finance Officer who will handle disbursement. */
        Long financeOfficerId
) {
}
