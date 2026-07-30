package com.granttrack.output.service;

import com.granttrack.output.dto.request.IPRecordRequest;
import com.granttrack.output.dto.response.IPRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPRecordService {
    IPRecordResponse create(IPRecordRequest request);
    IPRecordResponse update(Long id, IPRecordRequest request);
    IPRecordResponse getById(Long id);
    Page<IPRecordResponse> list(Long awardId, String status, Pageable pageable);
    void delete(Long id);
}
