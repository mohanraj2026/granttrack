package com.granttrack.progress.repository;

import com.granttrack.progress.entity.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Read access to the shared {@code progress_reports} table for finance milestone gating.
 * All report mutations are owned by core-service.
 */
public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {

    /** The most recent progress report filed for a milestone (handles resubmissions). */
    Optional<ProgressReport> findTopByMilestoneIdOrderByIdDesc(Long milestoneId);
}
