package com.granttrack.progress.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.progress.dto.request.ProgressReportRequest;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.service.ProgressReportService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/progress/reports")
@RequiredArgsConstructor
@Tag(name = "Progress — Reports", description = "Submit and review periodic progress reports")
public class ProgressReportController {

    private final ProgressReportService reportService;

    @PostMapping
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Create a draft progress report")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> create(@Valid @RequestBody ProgressReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Progress report created", reportService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Update a draft progress report")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ProgressReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Progress report updated", reportService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Submit a draft progress report for review")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Progress report submitted", reportService.submit(id)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Review a submitted progress report (APPROVE or REQUEST_REVISION), with an optional comment")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> review(@PathVariable Long id,
                                                                      @RequestParam String decision,
                                                                      @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(ApiResponse.success("Progress report reviewed", reportService.review(id, decision, comment)));
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Attach/replace the report document (PDF/DOC/DOCX) on a DRAFT or REVISION_REQUESTED report")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> uploadDocument(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Report document uploaded", reportService.uploadDocument(id, file)));
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Download a progress report's document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        ProgressReportService.ReportDocument doc = reportService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.resource());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a progress report by id")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List progress reports (filter by award / status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ProgressReportResponse>>> search(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "submittedDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(reportService.search(awardId, status, pageable))));
    }
}
