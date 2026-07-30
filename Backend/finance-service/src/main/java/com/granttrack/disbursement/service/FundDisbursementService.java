package com.granttrack.disbursement.service;

import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FundDisbursementService {
    Page<FundDisbursementResponse> search(Long awardId, Long milestoneId, String status, Pageable pageable);
}
