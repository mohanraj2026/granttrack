package com.granttrack.application.repository;

import com.granttrack.application.entity.GrantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrantApplicationRepository
        extends JpaRepository<GrantApplication, Long>, JpaSpecificationExecutor<GrantApplication> {

    @Query("select a.id from GrantApplication a where a.principalInvestigatorId = :pi")
    List<Long> findIdsByPrincipalInvestigatorId(@Param("pi") Long principalInvestigatorId);
}
