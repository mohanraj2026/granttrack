package com.granttrack.progress.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ProgressReportResponse(
        Long id,
        Long awardId,
        String period,
        String summary,
        String keyAchievements,
        String challenges,
        BigDecimal budgetUtilisationPercent,
        Long submittedById,
        Instant submittedDate,
        String reportDocName,
        Boolean hasReportDocument,
        String reviewComment,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
