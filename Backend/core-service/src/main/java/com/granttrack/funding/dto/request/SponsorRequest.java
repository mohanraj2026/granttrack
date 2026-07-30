package com.granttrack.funding.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SponsorRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Email @Size(max = 180) String contactEmail,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 250) String website
) {
}
