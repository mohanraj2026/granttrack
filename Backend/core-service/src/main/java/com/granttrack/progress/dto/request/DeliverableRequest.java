package com.granttrack.progress.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeliverableRequest(
        @NotNull Long awardId,
        @NotBlank @Size(max = 250) String title,
        @NotNull String type,
        LocalDate dueDate
) {
}
