-- ============================================================================
-- GrantTrack — V1 baseline schema (MySQL 8)
-- Conventions: every business table carries the BaseEntity columns
--   (created_at, updated_at, created_by, updated_by, deleted, version).
-- Money DECIMAL(15,2); percent DECIMAL(5,2); enums stored as VARCHAR.
-- FK behaviour: ON DELETE RESTRICT (soft delete is the norm).
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 1. Auth / User / Common
-- ---------------------------------------------------------------------------

CREATE TABLE institutions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(50)  NULL,
    country     VARCHAR(100) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NULL,
    PRIMARY KEY (id),
    KEY ix_institutions_name (name)
) ENGINE = InnoDB;

CREATE TABLE sponsors (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(50)  NULL,
    contact_email VARCHAR(180) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    BIGINT       NULL,
    updated_by    BIGINT       NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    version       BIGINT       NULL,
    PRIMARY KEY (id),
    KEY ix_sponsors_name (name)
) ENGINE = InnoDB;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(180) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    phone          VARCHAR(20)  NULL,
    institution_id BIGINT       NULL,
    department     VARCHAR(120) NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NULL,
    updated_by     BIGINT       NULL,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY ix_users_institution (institution_id),
    KEY ix_users_status (status),
    CONSTRAINT fk_users_institution FOREIGN KEY (institution_id) REFERENCES institutions (id)
) ENGINE = InnoDB;

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY ix_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB;

CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL,
    expiry_date DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_token (token),
    KEY ix_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    record_id   BIGINT       NULL,
    details     TEXT         NULL,
    timestamp   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY ix_audit_user (user_id),
    KEY ix_audit_entity (entity_type, record_id),
    KEY ix_audit_timestamp (timestamp),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 2. Funding
-- ---------------------------------------------------------------------------

CREATE TABLE funding_schemes (
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    scheme_name              VARCHAR(200)   NOT NULL,
    sponsor_id               BIGINT         NOT NULL,
    research_area            VARCHAR(200)   NULL,
    max_award_amount         DECIMAL(15, 2) NOT NULL,
    min_award_amount         DECIMAL(15, 2) NOT NULL,
    eligible_applicants      VARCHAR(500)   NULL,
    funding_duration_months  INT            NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at               DATETIME(6)    NOT NULL,
    updated_at               DATETIME(6)    NOT NULL,
    created_by               BIGINT         NULL,
    updated_by               BIGINT         NULL,
    deleted                  BOOLEAN        NOT NULL DEFAULT FALSE,
    version                  BIGINT         NULL,
    PRIMARY KEY (id),
    KEY ix_funding_schemes_sponsor (sponsor_id),
    KEY ix_funding_schemes_status (status),
    CONSTRAINT fk_funding_schemes_sponsor FOREIGN KEY (sponsor_id) REFERENCES sponsors (id)
) ENGINE = InnoDB;

CREATE TABLE grant_calls (
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    scheme_id              BIGINT         NOT NULL,
    call_title             VARCHAR(250)   NOT NULL,
    open_date              DATE           NOT NULL,
    close_date             DATE           NOT NULL,
    expected_awards        INT            NULL,
    total_budget_allocated DECIMAL(15, 2) NULL,
    review_method          VARCHAR(20)    NOT NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'UPCOMING',
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    created_by             BIGINT         NULL,
    updated_by             BIGINT         NULL,
    deleted                BOOLEAN        NOT NULL DEFAULT FALSE,
    version                BIGINT         NULL,
    PRIMARY KEY (id),
    KEY ix_grant_calls_scheme (scheme_id),
    KEY ix_grant_calls_status (status),
    KEY ix_grant_calls_window (open_date, close_date),
    CONSTRAINT fk_grant_calls_scheme FOREIGN KEY (scheme_id) REFERENCES funding_schemes (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 3. Application
-- ---------------------------------------------------------------------------

CREATE TABLE grant_applications (
    id                        BIGINT         NOT NULL AUTO_INCREMENT,
    call_id                   BIGINT         NOT NULL,
    principal_investigator_id BIGINT         NOT NULL,
    project_title             VARCHAR(300)   NOT NULL,
    research_abstract         TEXT           NULL,
    discipline                VARCHAR(150)   NULL,
    requested_amount          DECIMAL(15, 2) NOT NULL,
    project_duration_months   INT            NULL,
    institution_id            BIGINT         NULL,
    submission_date           DATETIME(6)    NULL,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at                DATETIME(6)    NOT NULL,
    updated_at                DATETIME(6)    NOT NULL,
    created_by                BIGINT         NULL,
    updated_by                BIGINT         NULL,
    deleted                   BOOLEAN        NOT NULL DEFAULT FALSE,
    version                   BIGINT         NULL,
    PRIMARY KEY (id),
    KEY ix_applications_call (call_id),
    KEY ix_applications_pi (principal_investigator_id),
    KEY ix_applications_status (status),
    KEY ix_applications_institution (institution_id),
    CONSTRAINT fk_applications_call FOREIGN KEY (call_id) REFERENCES grant_calls (id),
    CONSTRAINT fk_applications_pi FOREIGN KEY (principal_investigator_id) REFERENCES users (id),
    CONSTRAINT fk_applications_institution FOREIGN KEY (institution_id) REFERENCES institutions (id)
) ENGINE = InnoDB;

CREATE TABLE co_investigators (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    application_id BIGINT       NOT NULL,
    user_id        BIGINT       NULL,
    institution_id BIGINT       NULL,
    role           VARCHAR(30)  NOT NULL,
    contribution   VARCHAR(500) NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'INVITED',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NULL,
    updated_by     BIGINT       NULL,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NULL,
    PRIMARY KEY (id),
    KEY ix_coinvestigators_application (application_id),
    KEY ix_coinvestigators_user (user_id),
    CONSTRAINT fk_coinvestigators_application FOREIGN KEY (application_id) REFERENCES grant_applications (id),
    CONSTRAINT fk_coinvestigators_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_coinvestigators_institution FOREIGN KEY (institution_id) REFERENCES institutions (id)
) ENGINE = InnoDB;

CREATE TABLE application_budgets (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    application_id BIGINT         NOT NULL,
    budget_head    VARCHAR(30)    NOT NULL,
    amount         DECIMAL(15, 2) NOT NULL,
    justification  VARCHAR(500)   NULL,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    created_by     BIGINT         NULL,
    updated_by     BIGINT         NULL,
    deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    version        BIGINT         NULL,
    PRIMARY KEY (id),
    KEY ix_budgets_application (application_id),
    CONSTRAINT fk_budgets_application FOREIGN KEY (application_id) REFERENCES grant_applications (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 4. Review
-- ---------------------------------------------------------------------------

CREATE TABLE reviewer_assignments (
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    application_id            BIGINT      NOT NULL,
    reviewer_id               BIGINT      NOT NULL,
    assigned_date             DATE        NOT NULL,
    review_deadline           DATE        NULL,
    conflict_screening_status VARCHAR(20) NOT NULL DEFAULT 'CLEAR',
    status                    VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    created_by                BIGINT      NULL,
    updated_by                BIGINT      NULL,
    deleted                   BOOLEAN     NOT NULL DEFAULT FALSE,
    version                   BIGINT      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_assignment_app_reviewer (application_id, reviewer_id),
    KEY ix_assignments_application (application_id),
    KEY ix_assignments_reviewer (reviewer_id),
    KEY ix_assignments_status (status),
    CONSTRAINT fk_assignments_application FOREIGN KEY (application_id) REFERENCES grant_applications (id),
    CONSTRAINT fk_assignments_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE review_scores (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    assignment_id          BIGINT        NOT NULL,
    criterion              VARCHAR(30)   NOT NULL,
    score                  INT           NOT NULL,
    comments               VARCHAR(1000) NULL,
    overall_recommendation VARCHAR(30)   NULL,
    submitted_date         DATETIME(6)   NULL,
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    created_by             BIGINT        NULL,
    updated_by             BIGINT        NULL,
    deleted                BOOLEAN       NOT NULL DEFAULT FALSE,
    version                BIGINT        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_score_assignment_criterion (assignment_id, criterion),
    KEY ix_scores_assignment (assignment_id),
    CONSTRAINT fk_scores_assignment FOREIGN KEY (assignment_id) REFERENCES reviewer_assignments (id)
) ENGINE = InnoDB;

CREATE TABLE panel_decisions (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    application_id      BIGINT         NOT NULL,
    panel_date          DATE           NULL,
    consensus_score     DECIMAL(5, 2)  NULL,
    award_decision      VARCHAR(20)    NOT NULL,
    awarded_amount      DECIMAL(15, 2) NULL,
    conditions_attached VARCHAR(1000)  NULL,
    decided_by_id       BIGINT         NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    created_by          BIGINT         NULL,
    updated_by          BIGINT         NULL,
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,
    version             BIGINT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_panel_application (application_id),
    KEY ix_panel_decided_by (decided_by_id),
    CONSTRAINT fk_panel_application FOREIGN KEY (application_id) REFERENCES grant_applications (id),
    CONSTRAINT fk_panel_decided_by FOREIGN KEY (decided_by_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 5. Award
-- ---------------------------------------------------------------------------

CREATE TABLE grant_awards (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    application_id    BIGINT         NOT NULL,
    awarded_amount    DECIMAL(15, 2) NOT NULL,
    start_date        DATE           NULL,
    end_date          DATE           NULL,
    conditions_ref    VARCHAR(255)   NULL,
    award_letter_date DATE           NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        BIGINT         NULL,
    updated_by        BIGINT         NULL,
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    version           BIGINT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_award_application (application_id),
    KEY ix_awards_status (status),
    CONSTRAINT fk_awards_application FOREIGN KEY (application_id) REFERENCES grant_applications (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 6. Disbursement
-- ---------------------------------------------------------------------------

CREATE TABLE disbursement_milestones (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    award_id         BIGINT         NOT NULL,
    milestone_number INT            NOT NULL,
    description      VARCHAR(500)   NULL,
    due_date         DATE           NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    evidence_required BOOLEAN       NOT NULL DEFAULT TRUE,
    status           VARCHAR(30)    NOT NULL DEFAULT 'UPCOMING',
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    created_by       BIGINT         NULL,
    updated_by       BIGINT         NULL,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    version          BIGINT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_milestone_award_number (award_id, milestone_number),
    KEY ix_milestones_award (award_id),
    KEY ix_milestones_status (status),
    CONSTRAINT fk_milestones_award FOREIGN KEY (award_id) REFERENCES grant_awards (id)
) ENGINE = InnoDB;

CREATE TABLE fund_disbursements (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    milestone_id          BIGINT         NOT NULL,
    award_id              BIGINT         NOT NULL,
    amount                DECIMAL(15, 2) NOT NULL,
    disbursed_date        DATE           NULL,
    receiving_account_ref VARCHAR(100)   NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at            DATETIME(6)    NOT NULL,
    updated_at            DATETIME(6)    NOT NULL,
    created_by            BIGINT         NULL,
    updated_by            BIGINT         NULL,
    deleted               BOOLEAN        NOT NULL DEFAULT FALSE,
    version               BIGINT         NULL,
    PRIMARY KEY (id),
    KEY ix_disbursements_milestone (milestone_id),
    KEY ix_disbursements_award (award_id),
    KEY ix_disbursements_status (status),
    CONSTRAINT fk_disbursements_milestone FOREIGN KEY (milestone_id) REFERENCES disbursement_milestones (id),
    CONSTRAINT fk_disbursements_award FOREIGN KEY (award_id) REFERENCES grant_awards (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 7. Progress
-- ---------------------------------------------------------------------------

CREATE TABLE progress_reports (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    award_id                   BIGINT        NOT NULL,
    period                     VARCHAR(50)   NULL,
    summary                    TEXT          NULL,
    key_achievements           TEXT          NULL,
    challenges                 TEXT          NULL,
    budget_utilisation_percent DECIMAL(5, 2) NULL,
    submitted_by_id            BIGINT        NULL,
    submitted_date             DATETIME(6)   NULL,
    status                     VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    created_at                 DATETIME(6)   NOT NULL,
    updated_at                 DATETIME(6)   NOT NULL,
    created_by                 BIGINT        NULL,
    updated_by                 BIGINT        NULL,
    deleted                    BOOLEAN       NOT NULL DEFAULT FALSE,
    version                    BIGINT        NULL,
    PRIMARY KEY (id),
    KEY ix_progress_award (award_id),
    KEY ix_progress_status (status),
    KEY ix_progress_submitted_by (submitted_by_id),
    CONSTRAINT fk_progress_award FOREIGN KEY (award_id) REFERENCES grant_awards (id),
    CONSTRAINT fk_progress_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE deliverables (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    award_id       BIGINT       NOT NULL,
    title          VARCHAR(250) NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    due_date       DATE         NULL,
    submitted_date DATE         NULL,
    file_path      VARCHAR(500) NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NULL,
    updated_by     BIGINT       NULL,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NULL,
    PRIMARY KEY (id),
    KEY ix_deliverables_award (award_id),
    KEY ix_deliverables_status (status),
    CONSTRAINT fk_deliverables_award FOREIGN KEY (award_id) REFERENCES grant_awards (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 8. Output
-- ---------------------------------------------------------------------------

CREATE TABLE research_outputs (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    award_id              BIGINT        NOT NULL,
    type                  VARCHAR(30)   NOT NULL,
    title                 VARCHAR(300)  NOT NULL,
    authors               VARCHAR(1000) NULL,
    publication_venue     VARCHAR(250)  NULL,
    doi                   VARCHAR(100)  NULL,
    published_date        DATE          NULL,
    open_access_compliant BOOLEAN       NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'IN_PREPARATION',
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    created_by            BIGINT        NULL,
    updated_by            BIGINT        NULL,
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    version               BIGINT        NULL,
    PRIMARY KEY (id),
    KEY ix_outputs_award (award_id),
    KEY ix_outputs_type (type),
    KEY ix_outputs_doi (doi),
    CONSTRAINT fk_outputs_award FOREIGN KEY (award_id) REFERENCES grant_awards (id)
) ENGINE = InnoDB;

CREATE TABLE ip_records (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    award_id          BIGINT        NOT NULL,
    ip_type           VARCHAR(20)   NOT NULL,
    title             VARCHAR(300)  NOT NULL,
    inventors         VARCHAR(1000) NULL,
    filing_date       DATE          NULL,
    grant_date        DATE          NULL,
    ownership_percent DECIMAL(5, 2) NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'FILED',
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    created_by        BIGINT        NULL,
    updated_by        BIGINT        NULL,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    version           BIGINT        NULL,
    PRIMARY KEY (id),
    KEY ix_iprecords_award (award_id),
    CONSTRAINT fk_iprecords_award FOREIGN KEY (award_id) REFERENCES grant_awards (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- 9. Notification
-- ---------------------------------------------------------------------------

CREATE TABLE notifications (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    category   VARCHAR(20)   NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'UNREAD',
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    created_by BIGINT        NULL,
    updated_by BIGINT        NULL,
    deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    version    BIGINT        NULL,
    PRIMARY KEY (id),
    KEY ix_notifications_user (user_id),
    KEY ix_notifications_user_status (user_id, status),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;
