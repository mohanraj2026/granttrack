package com.granttrack.disbursement.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.disbursement.dto.request.MilestoneRequest;
import com.granttrack.disbursement.dto.request.MilestoneUpdateRequest;
import com.granttrack.disbursement.dto.request.ReleaseFundsRequest;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.service.DisbursementMilestoneService;
import com.granttrack.disbursement.service.FundDisbursementService;
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
@RequestMapping("/api/v1/disbursements")
@RequiredArgsConstructor
@Tag(name = "Disbursements", description = "Manage payment milestones and fund releases for grant awards")
public class DisbursementController {

    private final DisbursementMilestoneService milestoneService;
    private final FundDisbursementService disbursementService;

    @PostMapping("/milestones")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Create a disbursement milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(@Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milestone created", milestoneService.create(request)));
    }

    @PutMapping("/milestones/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Update a disbursement milestone (only when UPCOMING)")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(@PathVariable Long id,
                                                                          @Valid @RequestBody MilestoneUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", milestoneService.update(id, request)));
    }

    @GetMapping("/milestones")
    @Operation(summary = "List disbursement milestones (filter by award / status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<MilestoneResponse>>> searchMilestones(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "milestoneNumber") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(milestoneService.search(awardId, status, pageable))));
    }

    @GetMapping("/milestones/{id}")
    @Operation(summary = "Get a disbursement milestone by id")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestone(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.getById(id)));
    }

    @PostMapping(value = "/milestones/{id}/submit-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Submit evidence (completion note + supporting document) for a milestone (UPCOMING -> EVIDENCE_SUBMITTED)")
    public ResponseEntity<ApiResponse<MilestoneResponse>> submitEvidence(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Evidence submitted", milestoneService.submitEvidence(id, note, file)));
    }

    @GetMapping("/milestones/{id}/evidence-document")
    @Operation(summary = "Download a milestone's evidence document")
    public ResponseEntity<Resource> downloadEvidence(@PathVariable Long id) {
        DisbursementMilestoneService.EvidenceDocument doc = milestoneService.downloadEvidence(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.resource());
    }

    @PostMapping("/milestones/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','GRANT_ADMIN')")
    @Operation(summary = "Approve a milestone (EVIDENCE_SUBMITTED -> APPROVED)")
    public ResponseEntity<ApiResponse<MilestoneResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Milestone approved", milestoneService.approve(id)));
    }

    @PostMapping("/milestones/{id}/reject-evidence")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','GRANT_ADMIN')")
    @Operation(summary = "Return milestone evidence to the researcher for resubmission (EVIDENCE_SUBMITTED -> UPCOMING)")
    public ResponseEntity<ApiResponse<MilestoneResponse>> rejectEvidence(@PathVariable Long id,
                                                                         @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Evidence returned for resubmission",
                milestoneService.rejectEvidence(id, reason)));
    }

    @PostMapping("/milestones/{id}/release")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(summary = "Release funds for an APPROVED milestone (creates a fund disbursement)")
    public ResponseEntity<ApiResponse<FundDisbursementResponse>> release(@PathVariable Long id,
                                                                         @RequestBody(required = false) ReleaseFundsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Funds released", milestoneService.release(id, request)));
    }

    @GetMapping
    @Operation(summary = "List fund disbursements (filter by award / milestone / status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<FundDisbursementResponse>>> searchDisbursements(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) Long milestoneId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "disbursedDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(disbursementService.search(awardId, milestoneId, status, pageable))));
    }
}
