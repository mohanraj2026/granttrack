package com.granttrack.review.mapper;

import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.entity.ReviewScore;
import com.granttrack.review.entity.ReviewerAssignment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T13:42:54+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewerAssignmentResponse toResponse(ReviewerAssignment assignment) {
        if ( assignment == null ) {
            return null;
        }

        ReviewerAssignmentResponse.ReviewerAssignmentResponseBuilder reviewerAssignmentResponse = ReviewerAssignmentResponse.builder();

        reviewerAssignmentResponse.id( assignment.getId() );
        reviewerAssignmentResponse.applicationId( assignment.getApplicationId() );
        reviewerAssignmentResponse.reviewerId( assignment.getReviewerId() );
        reviewerAssignmentResponse.assignedDate( assignment.getAssignedDate() );
        reviewerAssignmentResponse.reviewDeadline( assignment.getReviewDeadline() );
        reviewerAssignmentResponse.responseComment( assignment.getResponseComment() );
        reviewerAssignmentResponse.createdAt( assignment.getCreatedAt() );
        reviewerAssignmentResponse.updatedAt( assignment.getUpdatedAt() );

        reviewerAssignmentResponse.conflictScreeningStatus( assignment.getConflictScreeningStatus().name() );
        reviewerAssignmentResponse.status( assignment.getStatus().name() );

        return reviewerAssignmentResponse.build();
    }

    @Override
    public ReviewScoreResponse toResponse(ReviewScore score) {
        if ( score == null ) {
            return null;
        }

        ReviewScoreResponse.ReviewScoreResponseBuilder reviewScoreResponse = ReviewScoreResponse.builder();

        reviewScoreResponse.assignmentId( scoreAssignmentId( score ) );
        reviewScoreResponse.id( score.getId() );
        reviewScoreResponse.score( score.getScore() );
        reviewScoreResponse.comments( score.getComments() );
        reviewScoreResponse.submittedDate( score.getSubmittedDate() );
        reviewScoreResponse.createdAt( score.getCreatedAt() );
        reviewScoreResponse.updatedAt( score.getUpdatedAt() );

        reviewScoreResponse.criterion( score.getCriterion().name() );
        reviewScoreResponse.overallRecommendation( score.getOverallRecommendation() == null ? null : score.getOverallRecommendation().name() );

        return reviewScoreResponse.build();
    }

    @Override
    public PanelDecisionResponse toResponse(PanelDecision decision) {
        if ( decision == null ) {
            return null;
        }

        PanelDecisionResponse.PanelDecisionResponseBuilder panelDecisionResponse = PanelDecisionResponse.builder();

        panelDecisionResponse.id( decision.getId() );
        panelDecisionResponse.applicationId( decision.getApplicationId() );
        panelDecisionResponse.panelDate( decision.getPanelDate() );
        panelDecisionResponse.consensusScore( decision.getConsensusScore() );
        panelDecisionResponse.awardedAmount( decision.getAwardedAmount() );
        panelDecisionResponse.conditionsAttached( decision.getConditionsAttached() );
        panelDecisionResponse.decidedById( decision.getDecidedById() );
        panelDecisionResponse.financeOfficerId( decision.getFinanceOfficerId() );
        panelDecisionResponse.createdAt( decision.getCreatedAt() );
        panelDecisionResponse.updatedAt( decision.getUpdatedAt() );

        panelDecisionResponse.awardDecision( decision.getAwardDecision().name() );

        return panelDecisionResponse.build();
    }

    private Long scoreAssignmentId(ReviewScore reviewScore) {
        ReviewerAssignment assignment = reviewScore.getAssignment();
        if ( assignment == null ) {
            return null;
        }
        return assignment.getId();
    }
}
