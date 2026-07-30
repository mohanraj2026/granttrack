package com.granttrack.funding.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.funding.dto.request.FundingSchemeRequest;
import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.service.FundingSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funding/schemes")
@RequiredArgsConstructor
@Tag(name = "Funding — Schemes", description = "Configure grant funding schemes")
public class FundingSchemeController {

    private final FundingSchemeService schemeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Create a funding scheme")
    public ResponseEntity<ApiResponse<FundingSchemeResponse>> create(@Valid @RequestBody FundingSchemeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Funding scheme created", schemeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Update a funding scheme")
    public ResponseEntity<ApiResponse<FundingSchemeResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody FundingSchemeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Funding scheme updated", schemeService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a funding scheme by id")
    public ResponseEntity<ApiResponse<FundingSchemeResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(schemeService.getById(id)));
    }
    

    @GetMapping
    @Operation(summary = "Search funding schemes (filter by status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<FundingSchemeResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(schemeService.search(q, status, pageable))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Change scheme status (ACTIVE/CLOSED/SUSPENDED)")
    public ResponseEntity<ApiResponse<FundingSchemeResponse>> changeStatus(@PathVariable Long id,
                                                                           @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Scheme status updated", schemeService.changeStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Soft-delete a funding scheme")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        schemeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Funding scheme deleted"));
    }

    @PostMapping(value = "/{id}/document", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Upload a scheme document")
    public ResponseEntity<ApiResponse<FundingSchemeResponse>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", schemeService.uploadDocument(id, file)));
    }
    @GetMapping("/{id}/document")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download the scheme document")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(@PathVariable Long id) {
        FundingSchemeService.SchemeDocument doc = schemeService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.resource());
    }
}
