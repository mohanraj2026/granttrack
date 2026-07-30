package com.granttrack.review.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.review.dto.request.PanelDecisionRequest;
import com.granttrack.review.dto.request.ReviewScoreRequest;
import com.granttrack.review.dto.request.ReviewerAssignmentRequest;
import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Reviewer assignments, blind scoring and panel decisions")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Assign a reviewer to an application")
    public ResponseEntity<ApiResponse<ReviewerAssignmentResponse>> assign(
            @Valid @RequestBody ReviewerAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reviewer assigned", reviewService.assignReviewer(request)));
    }

    @PostMapping("/assignments/{id}/conflict-check")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','REVIEWER')")
    @Operation(summary = "Record conflict-of-interest screening for an assignment")
    public ResponseEntity<ApiResponse<ReviewerAssignmentResponse>> conflictCheck(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Conflict screening recorded",
                reviewService.recordConflictCheck(id, status)));
    }

    @PostMapping("/assignments/{id}/respond")
    @PreAuthorize("hasRole('REVIEWER')")
    @Operation(summary = "Accept or decline a review assignment (a reason is required to decline)")
    public ResponseEntity<ApiResponse<ReviewerAssignmentResponse>> respond(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Response recorded",
                reviewService.respond(id, decision, reason)));
    }

    @PostMapping("/assignments/{id}/scores")
    @PreAuthorize("hasRole('REVIEWER')")
    @Operation(summary = "Submit a score for one criterion under an assignment")
    public ResponseEntity<ApiResponse<ReviewScoreResponse>> submitScore(
            @PathVariable Long id,
            @Valid @RequestBody ReviewScoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Score submitted", reviewService.submitScore(id, request)));
    }

    @PostMapping("/assignments/{id}/submit")
    @PreAuthorize("hasRole('REVIEWER')")
    @Operation(summary = "Mark an assignment as submitted (review complete)")
    public ResponseEntity<ApiResponse<ReviewerAssignmentResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Assignment submitted",
                reviewService.submitAssignment(id)));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('REVIEWER','GRANT_ADMIN','ADMIN')")
    @Operation(summary = "List reviewer assignments (filter by application / reviewer / status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ReviewerAssignmentResponse>>> listAssignments(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "assignedDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(reviewService.searchAssignments(applicationId, reviewerId, status, pageable))));
    }

    @GetMapping("/assignments/{id}/scores")
    @PreAuthorize("hasAnyRole('REVIEWER','GRANT_ADMIN','ADMIN')")
    @Operation(summary = "List the scores submitted under an assignment")
    public ResponseEntity<ApiResponse<List<ReviewScoreResponse>>> listScores(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getScores(id)));
    }

    @GetMapping("/applications/{appId}/reviews")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "List all submitted review scores for an application (for the panel to read before deciding)")
    public ResponseEntity<ApiResponse<List<ReviewScoreResponse>>> listApplicationReviews(@PathVariable Long appId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getApplicationReviews(appId)));
    }

    @PostMapping("/applications/{appId}/panel-decision")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Record the panel award decision for an application")
    public ResponseEntity<ApiResponse<PanelDecisionResponse>> createPanelDecision(
            @PathVariable Long appId,
            @Valid @RequestBody PanelDecisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Panel decision recorded",
                        reviewService.createPanelDecision(appId, request)));
    }

    @PutMapping("/applications/{appId}/panel-decision")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "Update the editable details of a panel decision (award outcome is immutable)")
    public ResponseEntity<ApiResponse<PanelDecisionResponse>> updatePanelDecision(
            @PathVariable Long appId,
            @Valid @RequestBody PanelDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Panel decision updated",
                reviewService.updatePanelDecision(appId, request)));
    }

    @GetMapping("/applications/{appId}/panel-decision")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN','RESEARCHER','FINANCE_OFFICER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Get the panel decision for an application (researcher sees only their own)")
    public ResponseEntity<ApiResponse<PanelDecisionResponse>> getPanelDecision(@PathVariable Long appId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getPanelDecision(appId)));
    }

    @GetMapping("/panel-decisions")
    @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")
    @Operation(summary = "List all recorded panel decisions (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<PanelDecisionResponse>>> listPanelDecisions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(reviewService.searchPanelDecisions(pageable))));
    }
}
