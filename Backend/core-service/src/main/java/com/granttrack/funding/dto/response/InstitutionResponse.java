package com.granttrack.funding.dto.response;

import lombok.Builder;
import java.time.Instant;

@Builder
public record InstitutionResponse(
        Long id,
        String name,
        String institutionCode,
        String type,
        String country,
        String universityName,
        String address,
        String city,
        String state,
        String pincode,
        String mobileNumber,
        String email,
        Instant createdAt
) {
}
