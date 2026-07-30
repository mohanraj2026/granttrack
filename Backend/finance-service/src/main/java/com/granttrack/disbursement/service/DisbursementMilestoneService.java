package com.granttrack.disbursement.service;

import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DisbursementMilestoneService {
    MilestoneResponse create(MilestoneRequest request);
    MilestoneResponse update(Long id, MilestoneUpdateRequest request);
    MilestoneResponse getById(Long id);
    Page<MilestoneResponse> search(Long awardId, String status, Pageable pageable);

    /** Researcher (owning PI) submits a completion note and optional supporting document. */
    MilestoneResponse submitEvidence(Long id, String note, MultipartFile document);

    /** Finance returns unsatisfactory evidence to the researcher for resubmission, with a reason. */
    MilestoneResponse rejectEvidence(Long id, String reason);

    MilestoneResponse approve(Long id);
    FundDisbursementResponse release(Long id, ReleaseFundsRequest request);

    /** Download the milestone's evidence document (read-scoped). */
    EvidenceDocument downloadEvidence(Long id);

    record EvidenceDocument(Resource resource, String filename) {
    }
}
