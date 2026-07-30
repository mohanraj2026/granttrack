package com.granttrack.output.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResearchOutputRequest(
        @NotNull Long awardId,
        @NotBlank String type,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 1000) String authors,
        @Size(max = 250) String publicationVenue,
        @Size(max = 100) String doi,
        LocalDate publishedDate,
        Boolean openAccessCompliant,
        String status
) {
}
