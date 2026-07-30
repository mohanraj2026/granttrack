package com.granttrack.review.repository;

import com.granttrack.review.entity.PanelDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PanelDecisionRepository
        extends JpaRepository<PanelDecision, Long>, JpaSpecificationExecutor<PanelDecision> {

    boolean existsByApplicationId(Long applicationId);

    Optional<PanelDecision> findByApplicationId(Long applicationId);
}
