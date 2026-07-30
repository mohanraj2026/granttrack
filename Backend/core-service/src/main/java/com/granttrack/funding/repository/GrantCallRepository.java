package com.granttrack.funding.repository;

import com.granttrack.funding.entity.GrantCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GrantCallRepository extends JpaRepository<GrantCall, Long>, JpaSpecificationExecutor<GrantCall> {
}
