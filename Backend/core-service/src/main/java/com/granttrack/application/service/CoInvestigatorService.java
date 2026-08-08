package com.granttrack.application.service;

import com.granttrack.application.dto.request.CoInvestigatorRequest;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.dto.response.MyInvitationResponse;

import java.util.List;

public interface CoInvestigatorService {
    CoInvestigatorResponse add(Long applicationId, CoInvestigatorRequest request);
    List<CoInvestigatorResponse> listByApplication(Long applicationId);
    void remove(Long applicationId, Long id);

    /** The invited user (matching userId) accepts or declines the invitation. */
    CoInvestigatorResponse respond(Long applicationId, Long coiId, String decision);

    /** Co-investigator invitations addressed to the current user (for their Applications page). */
    List<MyInvitationResponse> listMyInvitations();
}
