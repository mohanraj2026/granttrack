package com.granttrack.output.service;

import com.granttrack.output.dto.request.ResearchOutputRequest;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResearchOutputService {
    ResearchOutputResponse create(ResearchOutputRequest request);
    ResearchOutputResponse update(Long id, ResearchOutputRequest request);
    ResearchOutputResponse getById(Long id);
    Page<ResearchOutputResponse> search(Long awardId, String type, String status, String q, Pageable pageable);
    void delete(Long id);
}
