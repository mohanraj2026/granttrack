package com.granttrack.review.mapper;

import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.entity.PanelDecision;
import com.granttrack.review.entity.ReviewScore;
import com.granttrack.review.entity.ReviewerAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps review-module entities to blind-review-safe response DTOs. */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "conflictScreeningStatus", expression = "java(assignment.getConflictScreeningStatus().name())")
    @Mapping(target = "status", expression = "java(assignment.getStatus().name())")
    ReviewerAssignmentResponse toResponse(ReviewerAssignment assignment);

    @Mapping(target = "assignmentId", source = "assignment.id")
    @Mapping(target = "criterion", expression = "java(score.getCriterion().name())")
    @Mapping(target = "overallRecommendation",
            expression = "java(score.getOverallRecommendation() == null ? null : score.getOverallRecommendation().name())")
    ReviewScoreResponse toResponse(ReviewScore score);

    @Mapping(target = "awardDecision", expression = "java(decision.getAwardDecision().name())")
    PanelDecisionResponse toResponse(PanelDecision decision);
}
