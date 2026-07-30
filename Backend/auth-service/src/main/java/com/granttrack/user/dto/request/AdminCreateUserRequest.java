package com.granttrack.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin/Grant-Admin provisioning of an operational user account.
 * The system generates a temporary password (returned once for secure hand-off /
 * email delivery — email integration is deferred in Phase 1).
 */
public record AdminCreateUserRequest(

        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Email @Size(max = 180)
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        Long institutionId,

        @Size(max = 120)
        String department,

        /** Target role, e.g. ROLE_REVIEWER, ROLE_COMPLIANCE_OFFICER, ROLE_FINANCE_OFFICER. */
        @NotBlank
        String role
) {
}
