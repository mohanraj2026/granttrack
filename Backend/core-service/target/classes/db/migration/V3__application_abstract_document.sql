-- Abstract document upload support for grant applications (file stored on disk; path persisted).
ALTER TABLE grant_applications
    ADD COLUMN abstract_doc_path VARCHAR(500) NULL AFTER submission_date,
    ADD COLUMN abstract_doc_name VARCHAR(255) NULL AFTER abstract_doc_path;
