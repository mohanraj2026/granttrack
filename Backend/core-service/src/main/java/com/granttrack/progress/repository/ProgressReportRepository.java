package com.granttrack.progress.repository;

import com.granttrack.progress.entity.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long>, JpaSpecificationExecutor<ProgressReport> {
}
