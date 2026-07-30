package com.granttrack.disbursement.repository;

import com.granttrack.disbursement.entity.DisbursementMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface DisbursementMilestoneRepository extends JpaRepository<DisbursementMilestone, Long>, JpaSpecificationExecutor<DisbursementMilestone> {

    boolean existsByAwardIdAndMilestoneNumber(Long awardId, Integer milestoneNumber);

    @Query("select coalesce(sum(m.amount), 0) from DisbursementMilestone m where m.awardId = :awardId")
    BigDecimal sumAmountByAwardId(@Param("awardId") Long awardId);
}
