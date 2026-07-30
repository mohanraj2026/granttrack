-- Module 9: real document workflows for milestone evidence, progress reports and deliverables,
-- plus reviewer comments and a payment reference for fund releases.

-- Milestone evidence: a researcher now submits a note + supporting document, and Finance can
-- return it for resubmission with a reason.
ALTER TABLE disbursement_milestones
    ADD COLUMN evidence_note            VARCHAR(1000),
    ADD COLUMN evidence_doc_path        VARCHAR(500),
    ADD COLUMN evidence_doc_name        VARCHAR(255),
    ADD COLUMN evidence_submitted_date  DATE,
    ADD COLUMN evidence_review_comment  VARCHAR(1000);

-- Fund release: capture the payment/transaction reference alongside the receiving account.
ALTER TABLE fund_disbursements
    ADD COLUMN payment_reference VARCHAR(100);

-- Progress report: attach a report document and record the reviewer's comment.
ALTER TABLE progress_reports
    ADD COLUMN report_doc_path  VARCHAR(500),
    ADD COLUMN report_doc_name  VARCHAR(255),
    ADD COLUMN review_comment   VARCHAR(1000);

-- Deliverable: keep the original file name (file_path already holds the stored key) and
-- record the reviewer's comment on accept/reject.
ALTER TABLE deliverables
    ADD COLUMN file_name       VARCHAR(255),
    ADD COLUMN review_comment  VARCHAR(1000);
