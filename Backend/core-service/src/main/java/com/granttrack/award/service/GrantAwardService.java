package com.granttrack.award.service;

import com.granttrack.award.dto.request.AwardTermsRequest;
import com.granttrack.award.dto.request.GrantAwardRequest;
import com.granttrack.award.dto.response.GrantAwardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GrantAwardService {
    GrantAwardResponse create(GrantAwardRequest request);
    GrantAwardResponse update(Long id, AwardTermsRequest request);
    GrantAwardResponse approve(Long id);
    GrantAwardResponse changeStatus(Long id, String status);
    GrantAwardResponse getById(Long id);
    Page<GrantAwardResponse> search(String status, Long applicationId, Long financeOfficerId,
                                    String financeReviewStatus, Pageable pageable);

    /** The assigned Finance Officer accepts or rejects an award for disbursement (reason required to reject). */
    GrantAwardResponse financeReview(Long id, String decision, String reason);

    void delete(Long id);
}
