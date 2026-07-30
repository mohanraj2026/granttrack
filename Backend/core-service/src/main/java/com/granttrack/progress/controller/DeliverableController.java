package com.granttrack.progress.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.progress.dto.request.DeliverableRequest;
import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.service.DeliverableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/progress/deliverables")
@RequiredArgsConstructor
@Tag(name = "Progress — Deliverables", description = "Track, upload and review grant deliverables")
public class DeliverableController {

    private final DeliverableService deliverableService;

    @PostMapping
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Create a pending deliverable")
    public ResponseEntity<ApiResponse<DeliverableResponse>> create(@Valid @RequestBody DeliverableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deliverable created", deliverableService.create(request)));
    }

    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Upload the deliverable document and submit it (PENDING/REJECTED -> SUBMITTED)")
    public ResponseEntity<ApiResponse<DeliverableResponse>> upload(@PathVariable Long id,
                                                                   @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Deliverable uploaded", deliverableService.upload(id, file)));
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Download a deliverable's document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        DeliverableService.DeliverableDocument doc = deliverableService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.resource());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Review a submitted deliverable (ACCEPT or REJECT), with an optional comment")
    public ResponseEntity<ApiResponse<DeliverableResponse>> review(@PathVariable Long id,
                                                                   @RequestParam String decision,
                                                                   @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(ApiResponse.success("Deliverable reviewed", deliverableService.review(id, decision, comment)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a deliverable by id")
    public ResponseEntity<ApiResponse<DeliverableResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(deliverableService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List deliverables (filter by award / status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<DeliverableResponse>>> search(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(deliverableService.search(awardId, status, pageable))));
    }
}
