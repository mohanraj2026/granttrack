package com.granttrack.review.repository;

import com.granttrack.review.entity.ReviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewerAssignmentRepository
        extends JpaRepository<ReviewerAssignment, Long>, JpaSpecificationExecutor<ReviewerAssignment> {

    boolean existsByApplicationIdAndReviewerId(Long applicationId, Long reviewerId);

    List<ReviewerAssignment> findByApplicationId(Long applicationId);
}
