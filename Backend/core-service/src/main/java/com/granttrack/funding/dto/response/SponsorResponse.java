package com.granttrack.funding.dto.response;

import lombok.Builder;

@Builder
public record SponsorResponse(
        Long id,
        String sponsorCode,
        String name,
        String type,
        String contactEmail,
        String phone,
        String address,
        String website
) {
}
