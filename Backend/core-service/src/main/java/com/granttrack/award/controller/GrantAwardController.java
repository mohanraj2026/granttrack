package com.granttrack.award.controller;

import com.granttrack.award.dto.request.AwardTermsRequest;
import com.granttrack.award.dto.request.GrantAwardRequest;
import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.service.GrantAwardService;
import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
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
@RequestMapping("/api/v1/awards")
@RequiredArgsConstructor
@Tag(name = "Awards", description = "Manage grant awards")
public class GrantAwardController {

    private final GrantAwardService awardService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Create a grant award")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> create(@Valid @RequestBody GrantAwardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Award created", awardService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Update award terms (amount, dates, conditions)")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody AwardTermsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Award updated", awardService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Approve an award (records the award letter date)")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Award approved", awardService.approve(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Change award status (ACTIVE/SUSPENDED/COMPLETED/TERMINATED)")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> changeStatus(@PathVariable Long id,
                                                                        @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Award status updated", awardService.changeStatus(id, status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a grant award by id")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(awardService.getById(id)));
    }

    @PostMapping("/{id}/finance-review")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(summary = "Assigned finance officer accepts or rejects the award for disbursement (reason required to reject)")
    public ResponseEntity<ApiResponse<GrantAwardResponse>> financeReview(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Finance review recorded",
                awardService.financeReview(id, decision, reason)));
    }

    @GetMapping
    @Operation(summary = "Search grant awards (filter by status, applicationId, finance officer/review, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<GrantAwardResponse>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long financeOfficerId,
            @RequestParam(required = false) String financeReviewStatus,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(awardService.search(status, applicationId, financeOfficerId, financeReviewStatus, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a grant award")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        awardService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Award deleted"));
    }
}
