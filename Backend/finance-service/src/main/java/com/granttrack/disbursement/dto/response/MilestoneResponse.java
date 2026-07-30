package com.granttrack.disbursement.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record MilestoneResponse(
        Long id,
        Long awardId,
        Integer milestoneNumber,
        String description,
        LocalDate dueDate,
        BigDecimal amount,
        Boolean evidenceRequired,
        String evidenceNote,
        String evidenceDocName,
        Boolean hasEvidenceDocument,
        LocalDate evidenceSubmittedDate,
        String evidenceReviewComment,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
