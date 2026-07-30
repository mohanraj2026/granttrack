package com.granttrack.output.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record ResearchOutputResponse(
        Long id,
        Long awardId,
        String type,
        String title,
        String authors,
        String publicationVenue,
        String doi,
        LocalDate publishedDate,
        Boolean openAccessCompliant,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
