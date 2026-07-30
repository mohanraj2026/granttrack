package com.granttrack.application.dto.response;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record BlindApplicationResponse(
        Long id,
        String projectTitle,
        String researchAbstract,
        String discipline,
        BigDecimal requestedAmount,
        Integer projectDurationMonths,
        String abstractDocName
) {
}
