-- V10: assign a Finance Officer on the panel decision (Module 3)
-- The assigned finance officer handles disbursement for the awarded application.
ALTER TABLE panel_decisions
    ADD COLUMN finance_officer_id BIGINT NULL;

CREATE INDEX ix_panel_finance_officer ON panel_decisions (finance_officer_id);
