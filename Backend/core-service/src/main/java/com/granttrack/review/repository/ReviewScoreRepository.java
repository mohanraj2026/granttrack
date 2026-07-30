package com.granttrack.review.repository;

import com.granttrack.review.entity.ReviewCriterion;
import com.granttrack.review.entity.ReviewScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewScoreRepository
        extends JpaRepository<ReviewScore, Long>, JpaSpecificationExecutor<ReviewScore> {

    boolean existsByAssignmentIdAndCriterion(Long assignmentId, ReviewCriterion criterion);

    List<ReviewScore> findByAssignmentId(Long assignmentId);
}
