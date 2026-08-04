package com.granttrack.progress.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProgressReportRequest(
        @NotNull Long awardId,
        /** Optional: the disbursement milestone this report is the proof for. */
        Long milestoneId,
        @Size(max = 50) String period,
        String summary,
        String keyAchievements,
        String challenges,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal budgetUtilisationPercent
) {
}
