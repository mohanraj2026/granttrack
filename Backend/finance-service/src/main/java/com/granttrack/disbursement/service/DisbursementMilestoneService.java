package com.granttrack.disbursement.service;

import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DisbursementMilestoneService {
    MilestoneResponse create(MilestoneRequest request);
    MilestoneResponse update(Long id, MilestoneUpdateRequest request);
    MilestoneResponse getById(Long id);
    Page<MilestoneResponse> search(Long awardId, String status, Pageable pageable);

    /**
     * Finance verifies a milestone whose linked progress report has been APPROVED by the Compliance
     * Officer. Enforces sequential order (earlier milestones must be disbursed). Moves the milestone
     * to COMPLETED, from which funds can be released.
     */
    MilestoneResponse verify(Long id);

    /** Release funds for a COMPLETED (finance-verified) milestone (creates a fund disbursement). */
    FundDisbursementResponse release(Long id, ReleaseFundsRequest request);
}
