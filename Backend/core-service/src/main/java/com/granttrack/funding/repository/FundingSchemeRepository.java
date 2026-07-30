package com.granttrack.funding.repository;

import com.granttrack.funding.entity.FundingScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FundingSchemeRepository extends JpaRepository<FundingScheme, Long>, JpaSpecificationExecutor<FundingScheme> {
}
