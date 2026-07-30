package com.granttrack.funding.service;

import com.granttrack.funding.dto.request.GrantCallRequest;
import com.granttrack.funding.dto.response.GrantCallResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GrantCallService {
    GrantCallResponse create(GrantCallRequest request);
    GrantCallResponse update(Long id, GrantCallRequest request);
    GrantCallResponse getById(Long id);
    Page<GrantCallResponse> search(String q, String status, Long schemeId, Pageable pageable);
    GrantCallResponse open(Long id);
    GrantCallResponse close(Long id);
    GrantCallResponse terminate(Long id);
    void delete(Long id);
}
