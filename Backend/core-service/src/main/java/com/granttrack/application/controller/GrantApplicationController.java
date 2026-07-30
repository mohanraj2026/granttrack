package com.granttrack.application.controller;

import com.granttrack.application.dto.request.ApplicationBudgetRequest;
import com.granttrack.application.dto.request.CoInvestigatorRequest;
import com.granttrack.application.dto.request.GrantApplicationRequest;
import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.dto.response.BlindApplicationResponse;
import com.granttrack.application.service.ApplicationBudgetService;
import com.granttrack.application.service.CoInvestigatorService;
import com.granttrack.application.service.GrantApplicationService;
import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Submit and manage grant applications, co-investigators and budgets")
public class GrantApplicationController {

    private final GrantApplicationService applicationService;
    private final CoInvestigatorService coInvestigatorService;
    private final ApplicationBudgetService budgetService;

    @PostMapping
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Create a grant application as DRAFT")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> create(
            @Valid @RequestBody GrantApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created", applicationService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Update a DRAFT application")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody GrantApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated", applicationService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get an application by id")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getById(id)));
    }

    @GetMapping("/{id}/blind")
    @PreAuthorize("hasAnyRole('REVIEWER','GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Get a blind application by id (without PI/institution info); reviewers see only assigned apps")
    public ResponseEntity<ApiResponse<BlindApplicationResponse>> getBlind(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getBlindById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search applications (filter by status / call, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<GrantApplicationResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long callId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(applicationService.search(q, status, callId, pageable))));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Submit a DRAFT application")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application submitted", applicationService.submit(id)));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Withdraw a DRAFT or SUBMITTED application")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn", applicationService.withdraw(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Administratively transition an application status")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> changeStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Application status updated",
                applicationService.changeStatus(id, status)));
    }

    @PostMapping("/{id}/co-investigators")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Add a co-investigator to an application")
    public ResponseEntity<ApiResponse<CoInvestigatorResponse>> addCoInvestigator(
            @PathVariable Long id, @Valid @RequestBody CoInvestigatorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Co-investigator added", coInvestigatorService.add(id, request)));
    }

    @GetMapping("/{id}/co-investigators")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List co-investigators for an application")
    public ResponseEntity<ApiResponse<List<CoInvestigatorResponse>>> listCoInvestigators(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(coInvestigatorService.listByApplication(id)));
    }

    @DeleteMapping("/{id}/co-investigators/{coiId}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Remove a co-investigator from an application")
    public ResponseEntity<ApiResponse<Void>> removeCoInvestigator(
            @PathVariable Long id, @PathVariable Long coiId) {
        coInvestigatorService.remove(id, coiId);
        return ResponseEntity.ok(ApiResponse.success("Co-investigator removed", null));
    }

    @PostMapping("/{id}/co-investigators/{coiId}/respond")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Accept or decline a co-investigator invitation (the invited user only)")
    public ResponseEntity<ApiResponse<CoInvestigatorResponse>> respondCoInvestigator(
            @PathVariable Long id, @PathVariable Long coiId, @RequestParam String decision) {
        return ResponseEntity.ok(ApiResponse.success("Invitation response recorded",
                coInvestigatorService.respond(id, coiId, decision)));
    }

    @PostMapping("/{id}/budgets")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Add a budget line to an application")
    public ResponseEntity<ApiResponse<ApplicationBudgetResponse>> addBudget(
            @PathVariable Long id, @Valid @RequestBody ApplicationBudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget line added", budgetService.add(id, request)));
    }

    @GetMapping("/{id}/budgets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List budget lines for an application")
    public ResponseEntity<ApiResponse<List<ApplicationBudgetResponse>>> listBudgets(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.listByApplication(id)));
    }

    @DeleteMapping("/{id}/budgets/{budgetId}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Remove a budget line from an application")
    public ResponseEntity<ApiResponse<Void>> removeBudget(
            @PathVariable Long id, @PathVariable Long budgetId) {
        budgetService.remove(id, budgetId);
        return ResponseEntity.ok(ApiResponse.success("Budget line removed", null));
    }

    @PostMapping(value = "/{id}/abstract-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Upload the abstract document (PDF/DOC/DOCX, max 10 MB) for a DRAFT application")
    public ResponseEntity<ApiResponse<GrantApplicationResponse>> uploadAbstract(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.success("Abstract document uploaded", applicationService.uploadAbstract(id, file)));
    }

    @GetMapping("/{id}/abstract-document")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download the abstract document for an application")
    public ResponseEntity<Resource> downloadAbstract(@PathVariable Long id) {
        GrantApplicationService.AbstractDocument doc = applicationService.downloadAbstract(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.resource());
    }
}
