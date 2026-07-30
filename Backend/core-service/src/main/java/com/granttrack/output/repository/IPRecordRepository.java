package com.granttrack.output.repository;

import com.granttrack.output.entity.IPRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IPRecordRepository extends JpaRepository<IPRecord, Long>, JpaSpecificationExecutor<IPRecord> {
}
