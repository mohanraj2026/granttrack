-- V11: Finance Officer acceptance of an award before disbursement setup (Module 4)
-- The assigned finance officer reviews the awarded application and Accepts/Rejects it;
-- milestones can only be created once the award is ACCEPTED by finance.
ALTER TABLE grant_awards
    ADD COLUMN finance_officer_id BIGINT NULL,
    ADD COLUMN finance_review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN finance_review_comment VARCHAR(1000) NULL;

CREATE INDEX ix_awards_finance_officer ON grant_awards (finance_officer_id);
