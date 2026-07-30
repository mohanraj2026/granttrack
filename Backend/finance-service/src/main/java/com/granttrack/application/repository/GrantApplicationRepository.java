package com.granttrack.application.repository;

import com.granttrack.application.entity.GrantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read access to the shared {@code grant_applications} table for finance read-scoping
 * (a researcher only sees disbursements for their own applications' awards).
 */
public interface GrantApplicationRepository extends JpaRepository<GrantApplication, Long> {

    @Query("select a.id from GrantApplication a where a.principalInvestigatorId = :pi")
    List<Long> findIdsByPrincipalInvestigatorId(@Param("pi") Long principalInvestigatorId);
}
