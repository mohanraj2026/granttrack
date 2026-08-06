package com.granttrack.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload. Self-registrants are ALWAYS created as {@code ROLE_RESEARCHER};
 * the role is fixed server-side and cannot be supplied by the client. Operational accounts
 * (reviewer, finance, compliance, grant-admin, admin) are provisioned only via User Administration.
 */
public record RegisterRequest(

        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Email @Size(max = 180)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
        String phone,

        String countryCode,

        Long institutionId,

        @Size(max = 120)
        String department,

        @Size(max = 200)
        String education
) {
}
