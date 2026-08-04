package com.granttrack.disbursement.repository;

import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface DisbursementMilestoneRepository extends JpaRepository<DisbursementMilestone, Long>, JpaSpecificationExecutor<DisbursementMilestone> {

    boolean existsByAwardIdAndMilestoneNumber(Long awardId, Integer milestoneNumber);

    @Query("select coalesce(sum(m.amount), 0) from DisbursementMilestone m where m.awardId = :awardId")
    BigDecimal sumAmountByAwardId(@Param("awardId") Long awardId);

    /** Count earlier milestones (lower number) for the award that are not yet in the given status,
     *  used to enforce sequential completion (a milestone waits until earlier ones are DISBURSED). */
    long countByAwardIdAndMilestoneNumberLessThanAndStatusNot(Long awardId, Integer milestoneNumber, MilestoneStatus status);
}
