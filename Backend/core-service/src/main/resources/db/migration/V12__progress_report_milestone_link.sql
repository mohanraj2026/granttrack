-- V12: Link a progress report to a disbursement milestone.
-- A milestone's proof is now a progress report (reviewed by Compliance, then verified by Finance)
-- instead of a free-form evidence upload. The link is optional (period reports may be unlinked).
ALTER TABLE progress_reports
    ADD COLUMN milestone_id BIGINT NULL;

CREATE INDEX ix_progress_milestone ON progress_reports (milestone_id);
