package com.granttrack.application.repository;

import com.granttrack.application.entity.CoInvestigator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CoInvestigatorRepository
        extends JpaRepository<CoInvestigator, Long>, JpaSpecificationExecutor<CoInvestigator> {

    List<CoInvestigator> findByApplicationId(Long applicationId);
}
