package com.granttrack.funding.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.funding.dto.request.SponsorRequest;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.service.SponsorService;
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
@RequestMapping("/api/v1/funding/sponsors")
@RequiredArgsConstructor
@Tag(name = "Funding — Sponsors", description = "Manage funding sponsors")
public class SponsorController {

    private final SponsorService sponsorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Create a sponsor")
    public ResponseEntity<ApiResponse<SponsorResponse>> create(@Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sponsor created", sponsorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Update a sponsor")
    public ResponseEntity<ApiResponse<SponsorResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sponsor updated", sponsorService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sponsor by id")
    public ResponseEntity<ApiResponse<SponsorResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sponsorService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List/search sponsors")
    public ResponseEntity<ApiResponse<PageResponse<SponsorResponse>>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(sponsorService.list(q, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Soft-delete a sponsor")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sponsorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Sponsor deleted"));
    }
}
