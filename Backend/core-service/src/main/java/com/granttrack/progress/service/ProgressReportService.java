package com.granttrack.progress.service;

import com.granttrack.progress.dto.request.ProgressReportRequest;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProgressReportService {
    ProgressReportResponse create(ProgressReportRequest request);
    ProgressReportResponse update(Long id, ProgressReportRequest request);
    ProgressReportResponse submit(Long id);

    /** Compliance officer reviews a submitted report, with an optional comment shown to the researcher. */
    ProgressReportResponse review(Long id, String decision, String comment);

    ProgressReportResponse getById(Long id);
    Page<ProgressReportResponse> search(Long awardId, String status, Pageable pageable);

    /** Owning PI attaches / replaces the report document (DRAFT or REVISION_REQUESTED). */
    ProgressReportResponse uploadDocument(Long id, MultipartFile document);

    /** Download the report document (read-scoped). */
    ReportDocument downloadDocument(Long id);

    record ReportDocument(Resource resource, String filename) {
    }
}
