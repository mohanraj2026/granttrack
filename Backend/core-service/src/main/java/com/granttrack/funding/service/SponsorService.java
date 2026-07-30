package com.granttrack.funding.service;

import com.granttrack.funding.dto.request.SponsorRequest;
import com.granttrack.funding.dto.response.SponsorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SponsorService {
    SponsorResponse create(SponsorRequest request);
    SponsorResponse update(Long id, SponsorRequest request);
    SponsorResponse getById(Long id);
    Page<SponsorResponse> list(String q, Pageable pageable);
    void delete(Long id);
}
