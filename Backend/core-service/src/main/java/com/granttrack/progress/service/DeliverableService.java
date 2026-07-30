package com.granttrack.progress.service;

import com.granttrack.progress.dto.request.DeliverableRequest;
import com.granttrack.progress.dto.response.DeliverableResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DeliverableService {
    DeliverableResponse create(DeliverableRequest request);

    /** Owning PI uploads the deliverable document (PENDING or REJECTED -> SUBMITTED). */
    DeliverableResponse upload(Long id, MultipartFile document);

    /** Compliance officer accepts/rejects, with an optional comment shown to the researcher. */
    DeliverableResponse review(Long id, String decision, String comment);

    DeliverableResponse getById(Long id);
    Page<DeliverableResponse> search(Long awardId, String status, Pageable pageable);

    /** Download the deliverable document (read-scoped). */
    DeliverableDocument downloadDocument(Long id);

    record DeliverableDocument(Resource resource, String filename) {
    }
}
