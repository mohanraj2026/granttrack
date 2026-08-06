package com.granttrack.funding.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SponsorRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Email @Size(max = 180) String contactEmail,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
        String phone,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 250) String website
) {
}
