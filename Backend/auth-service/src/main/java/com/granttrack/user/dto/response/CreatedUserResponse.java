package com.granttrack.user.dto.response;

import com.granttrack.auth.dto.response.UserResponse;
import lombok.Builder;

/**
 * Returned once when an admin provisions a user. The temporary password is shown
 * a single time so the administrator can securely relay it (or it can be emailed
 * to the user once email delivery is enabled).
 */
@Builder
public record CreatedUserResponse(
        UserResponse user
) {
}
