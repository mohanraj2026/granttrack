package com.granttrack.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin/Grant-Admin update of an existing user's editable details.
 * Password and role are intentionally NOT editable here (password is changed by the
 * user via the profile flow; role changes are a separate, deliberate operation).
 */
public record AdminUpdateUserRequest(

        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Email @Size(max = 180)
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
        String phone,

        Long institutionId,

        @Size(max = 120)
        String department
) {
}
