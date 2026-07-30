package com.granttrack.funding.service;

import com.granttrack.funding.dto.request.FundingSchemeRequest;
import com.granttrack.funding.dto.response.FundingSchemeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FundingSchemeService {
    FundingSchemeResponse create(FundingSchemeRequest request);
    FundingSchemeResponse update(Long id, FundingSchemeRequest request);
    FundingSchemeResponse getById(Long id);
    Page<FundingSchemeResponse> search(String q, String status, Pageable pageable);
    FundingSchemeResponse changeStatus(Long id, String status);
    FundingSchemeResponse uploadDocument(Long id, org.springframework.web.multipart.MultipartFile file);
    SchemeDocument downloadDocument(Long id);
    void delete(Long id);

    record SchemeDocument(org.springframework.core.io.Resource resource, String filename) {}
}
