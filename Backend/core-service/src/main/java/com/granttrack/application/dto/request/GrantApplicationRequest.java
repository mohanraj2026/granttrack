package com.granttrack.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GrantApplicationRequest(
        @NotNull Long callId,
        Long principalInvestigatorId,
        @NotBlank @Size(max = 300) String projectTitle,
        String researchAbstract,
        @Size(max = 150) String discipline,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal requestedAmount,
        @Positive Integer projectDurationMonths,
        Long institutionId
) {
}
