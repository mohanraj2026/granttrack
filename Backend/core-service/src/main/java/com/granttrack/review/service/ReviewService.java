package com.granttrack.review.service;

import com.granttrack.review.dto.request.PanelDecisionRequest;
import com.granttrack.review.dto.request.ReviewScoreRequest;
import com.granttrack.review.dto.request.ReviewerAssignmentRequest;
import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {

    ReviewerAssignmentResponse assignReviewer(ReviewerAssignmentRequest request);

    ReviewerAssignmentResponse recordConflictCheck(Long assignmentId, String status);

    ReviewerAssignmentResponse respond(Long assignmentId, String decision, String reason);

    ReviewScoreResponse submitScore(Long assignmentId, ReviewScoreRequest request);

    ReviewerAssignmentResponse submitAssignment(Long assignmentId);

    Page<ReviewerAssignmentResponse> searchAssignments(Long applicationId, Long reviewerId, String status, Pageable pageable);

    List<ReviewScoreResponse> getScores(Long assignmentId);

    /** All submitted review scores for an application — for the Grant Admin to read before the panel decision. */
    List<ReviewScoreResponse> getApplicationReviews(Long applicationId);

    PanelDecisionResponse createPanelDecision(Long applicationId, PanelDecisionRequest request);

    /** Update the editable details of an existing panel decision (award outcome is immutable). */
    PanelDecisionResponse updatePanelDecision(Long applicationId, PanelDecisionRequest request);

    PanelDecisionResponse getPanelDecision(Long applicationId);

    /** Paginated list of all panel decisions (Grant Admin / Admin). */
    Page<PanelDecisionResponse> searchPanelDecisions(Pageable pageable);
}
