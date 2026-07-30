package com.granttrack.disbursement.repository;

import com.granttrack.disbursement.entity.FundDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FundDisbursementRepository extends JpaRepository<FundDisbursement, Long>, JpaSpecificationExecutor<FundDisbursement> {
}
