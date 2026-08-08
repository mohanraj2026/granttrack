package com.granttrack.application.dto.response;

import lombok.Builder;

/**
 * A co-investigator invitation as seen by the invited user, enriched with the application's title
 * so it can be shown (and accepted/declined) on their Applications page without needing read access
 * to the full application before they accept.
 */
@Builder
public record MyInvitationResponse(
        Long coInvestigatorId,
        Long applicationId,
        String projectTitle,
        String role,
        String status
) {
}
