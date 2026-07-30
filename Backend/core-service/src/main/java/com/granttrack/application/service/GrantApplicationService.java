package com.granttrack.application.service;

import com.granttrack.application.dto.request.GrantApplicationRequest;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.dto.response.BlindApplicationResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface GrantApplicationService {
    GrantApplicationResponse create(GrantApplicationRequest request);
    GrantApplicationResponse update(Long id, GrantApplicationRequest request);
    GrantApplicationResponse getById(Long id);
    BlindApplicationResponse getBlindById(Long id);
    Page<GrantApplicationResponse> search(String q, String status, Long callId, Pageable pageable);
    GrantApplicationResponse submit(Long id);
    GrantApplicationResponse withdraw(Long id);
    GrantApplicationResponse changeStatus(Long id, String status);

    /** Upload (or replace) the abstract document for a DRAFT application. */
    GrantApplicationResponse uploadAbstract(Long id, MultipartFile file);

    /** A downloadable handle plus original filename for the stored abstract document. */
    AbstractDocument downloadAbstract(Long id);

    record AbstractDocument(Resource resource, String filename) {
    }
}
