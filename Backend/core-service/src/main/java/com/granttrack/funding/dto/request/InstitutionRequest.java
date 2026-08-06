package com.granttrack.funding.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 200) String universityName,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(max = 10) String pincode,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
        String mobileNumber,
        @Email @Size(max = 180) String email
) {
}
