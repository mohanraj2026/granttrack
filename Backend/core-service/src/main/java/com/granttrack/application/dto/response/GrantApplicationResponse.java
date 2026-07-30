package com.granttrack.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record GrantApplicationResponse(
        Long id,
        Long callId,
        Long principalInvestigatorId,
        String projectTitle,
        String researchAbstract,
        String discipline,
        BigDecimal requestedAmount,
        Integer projectDurationMonths,
        Long institutionId,
        Instant submissionDate,
        String abstractDocPath,
        String abstractDocName,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
