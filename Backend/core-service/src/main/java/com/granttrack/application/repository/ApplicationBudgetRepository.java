package com.granttrack.application.repository;

import com.granttrack.application.entity.ApplicationBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApplicationBudgetRepository
        extends JpaRepository<ApplicationBudget, Long>, JpaSpecificationExecutor<ApplicationBudget> {

    List<ApplicationBudget> findByApplicationId(Long applicationId);
}
