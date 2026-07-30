-- V9: Reviewer decline reason (Module 2)
-- A reviewer may decline an assignment; the reason is stored and surfaced to the Grant Admin.
ALTER TABLE reviewer_assignments
    ADD COLUMN response_comment VARCHAR(1000) NULL;
