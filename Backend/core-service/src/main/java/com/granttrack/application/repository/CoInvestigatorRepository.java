package com.granttrack.application.repository;

import com.granttrack.application.entity.CoInvestigator;
import com.granttrack.application.entity.CoInvestigatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoInvestigatorRepository
        extends JpaRepository<CoInvestigator, Long>, JpaSpecificationExecutor<CoInvestigator> {

    List<CoInvestigator> findByApplicationId(Long applicationId);

    /** Co-investigator invitations addressed to a specific existing user (their "my invitations"). */
    List<CoInvestigator> findByUserId(Long userId);

    /** True if the user is a co-investigator of the given status on the application (e.g. CONFIRMED). */
    boolean existsByApplicationIdAndUserIdAndStatus(Long applicationId, Long userId, CoInvestigatorStatus status);

    /** Application ids a user is a co-investigator of the given status on (for read scoping). */
    @Query("select c.application.id from CoInvestigator c where c.userId = :userId and c.status = :status")
    List<Long> findApplicationIdsByUserIdAndStatus(@Param("userId") Long userId,
                                                   @Param("status") CoInvestigatorStatus status);
}
