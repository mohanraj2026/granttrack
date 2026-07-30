package com.granttrack.funding.service;

import com.granttrack.funding.dto.request.InstitutionRequest;
import com.granttrack.funding.dto.response.InstitutionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstitutionService {
    InstitutionResponse create(InstitutionRequest request);
    InstitutionResponse update(Long id, InstitutionRequest request);
    InstitutionResponse getById(Long id);
    Page<InstitutionResponse> list(String q, Pageable pageable);
    void delete(Long id);
}
