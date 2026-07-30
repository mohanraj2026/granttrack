package com.granttrack.funding.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.funding.dto.request.InstitutionRequest;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.service.InstitutionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funding/institutions")
@RequiredArgsConstructor
@Tag(name = "Funding — Institutions", description = "Manage research institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Create an institution")
    public ResponseEntity<ApiResponse<InstitutionResponse>> create(@Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Institution created", institutionService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Update an institution")
    public ResponseEntity<ApiResponse<InstitutionResponse>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Institution updated", institutionService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an institution by id")
    public ResponseEntity<ApiResponse<InstitutionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(institutionService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List/search institutions")
    public ResponseEntity<ApiResponse<PageResponse<InstitutionResponse>>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(institutionService.list(q, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Soft-delete an institution")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        institutionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Institution deleted"));
    }
}
