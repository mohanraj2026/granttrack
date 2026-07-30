package com.granttrack.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoInvestigatorRequest(
        Long userId,
        Long institutionId,
        @NotNull String role,
        @Size(max = 500) String contribution
) {
}
