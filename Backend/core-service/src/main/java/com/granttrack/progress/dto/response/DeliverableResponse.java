package com.granttrack.progress.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record DeliverableResponse(
        Long id,
        Long awardId,
        String title,
        String type,
        LocalDate dueDate,
        LocalDate submittedDate,
        String filePath,
        String fileName,
        Boolean hasFile,
        String reviewComment,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
