# GrantTrack — Database Structure Specification

> Paste this section into your prompt **after** "Database / Use MySQL" and **before** the "Modules" section.
> It defines exact column types, constraints, relationships, indexes, and enum values so the generated JPA entities and MySQL schema are consistent and correct.

---

## 0. Global Conventions (apply to ALL tables)

Every table includes these base columns (implement via a `@MappedSuperclass` `BaseEntity` / `Auditable`):

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` | Primary key, `AUTO_INCREMENT`, `@GeneratedValue(strategy = IDENTITY)` |
| `created_at` | `DATETIME(6)` | `@CreatedDate`, not null, immutable |
| `updated_at` | `DATETIME(6)` | `@LastModifiedDate`, not null |
| `created_by` | `BIGINT` | nullable, user id (JPA auditing) |
| `updated_by` | `BIGINT` | nullable, user id |
| `deleted` | `BOOLEAN` | not null, default `FALSE` — soft delete flag |
| `version` | `BIGINT` | nullable, `@Version` for optimistic locking |

Rules:
- **Money** → `DECIMAL(15,2)` everywhere. Never `double`/`float`.
- **Percent** → `DECIMAL(5,2)` (0.00–100.00).
- **Enums** → store as `VARCHAR` via `@Enumerated(EnumType.STRING)`. All enum constants are `UPPER_SNAKE_CASE`.
- **Soft delete** → all repositories filter `deleted = false` (use `@SQLRestriction("deleted = false")` or `@Where`).
- **Dates**: calendar dates → `DATE`; timestamps → `DATETIME(6)`.
- **FK behaviour** → `ON DELETE RESTRICT` by default (rely on soft delete, not physical delete).
- All FK columns get an index. All `status`/`category` enum columns used in list filters get an index.

---

## 1. Auth / User / Common module

### `institutions`  *(NEW — referenced by InstitutionID but undefined in spec)*
| Column | Type | Constraints |
|---|---|---|
| name | VARCHAR(200) | not null |
| type | VARCHAR(50) | nullable (University/Research Council/Corporate/Govt) |
| country | VARCHAR(100) | nullable |
| `+ base columns` | | |
Index: `name`.

### `sponsors`  *(NEW — referenced by SponsorID but undefined in spec)*
| Column | Type | Constraints |
|---|---|---|
| name | VARCHAR(200) | not null |
| type | VARCHAR(50) | nullable (Govt/Corporate/Foundation) |
| contact_email | VARCHAR(180) | nullable |
| `+ base columns` | | |

### `users`
| Column | Type | Constraints |
|---|---|---|
| name | VARCHAR(150) | not null |
| email | VARCHAR(180) | not null, **UNIQUE** |
| password | VARCHAR(255) | not null (BCrypt hash) |
| phone | VARCHAR(20) | nullable |
| institution_id | BIGINT | FK → institutions(id), nullable |
| department | VARCHAR(120) | nullable |
| status | ENUM | not null, default `ACTIVE` |
| `+ base columns` | | |
- `status`: `ACTIVE`, `INACTIVE`
- Indexes: unique(`email`), `institution_id`, `status`
- **Relationship**: `users` ⇄ `roles` is **Many-to-Many** via `user_roles`.

### `roles`
| Column | Type | Constraints |
|---|---|---|
| name | VARCHAR(50) | not null, UNIQUE |
| description | VARCHAR(255) | nullable |
- Seed values: `ROLE_RESEARCHER`, `ROLE_REVIEWER`, `ROLE_GRANT_ADMIN`, `ROLE_FINANCE_OFFICER`, `ROLE_COMPLIANCE_OFFICER`, `ROLE_ADMIN`

### `user_roles`  *(join table, M:N)*
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | FK → users(id), not null |
| role_id | BIGINT | FK → roles(id), not null |
- Composite PK (`user_id`, `role_id`).

### `refresh_tokens`  *(NEW — required by your "Refresh Token Support" but undefined in spec)*
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | FK → users(id), not null |
| token | VARCHAR(512) | not null, UNIQUE |
| expiry_date | DATETIME(6) | not null |
| revoked | BOOLEAN | not null, default FALSE |
| `+ base columns` | | |
- Index: `user_id`, unique(`token`).

### `audit_logs`
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | FK → users(id), nullable |
| action | VARCHAR(100) | not null |
| entity_type | VARCHAR(100) | not null |
| record_id | BIGINT | nullable |
| details | TEXT | nullable (JSON of before/after) |
| timestamp | DATETIME(6) | not null |
- Indexes: `user_id`, composite(`entity_type`, `record_id`), `timestamp`.

---

## 2. Funding module

### `funding_schemes`
| Column | Type | Constraints |
|---|---|---|
| scheme_name | VARCHAR(200) | not null |
| sponsor_id | BIGINT | FK → sponsors(id), not null |
| research_area | VARCHAR(200) | nullable |
| max_award_amount | DECIMAL(15,2) | not null |
| min_award_amount | DECIMAL(15,2) | not null |
| eligible_applicants | VARCHAR(500) | nullable |
| funding_duration_months | INT | nullable |
| status | ENUM | not null, default `ACTIVE` |
| `+ base columns` | | |
- `status`: `ACTIVE`, `CLOSED`, `SUSPENDED`
- Indexes: `sponsor_id`, `status`.
- **Relationship**: FundingScheme **1 : N** GrantCall.

### `grant_calls`
| Column | Type | Constraints |
|---|---|---|
| scheme_id | BIGINT | FK → funding_schemes(id), not null |
| call_title | VARCHAR(250) | not null |
| open_date | DATE | not null |
| close_date | DATE | not null |
| expected_awards | INT | nullable |
| total_budget_allocated | DECIMAL(15,2) | nullable |
| review_method | ENUM | not null |
| status | ENUM | not null, default `UPCOMING` |
| `+ base columns` | | |
- `review_method`: `SINGLE_BLIND`, `DOUBLE_BLIND`, `PANEL`
- `status`: `UPCOMING`, `OPEN`, `UNDER_REVIEW`, `AWARDED`, `CLOSED`
- Indexes: `scheme_id`, `status`, composite(`open_date`, `close_date`).
- **Relationship**: GrantCall **1 : N** GrantApplication.

---

## 3. Application module

### `grant_applications`
| Column | Type | Constraints |
|---|---|---|
| call_id | BIGINT | FK → grant_calls(id), not null |
| principal_investigator_id | BIGINT | FK → users(id), not null |
| project_title | VARCHAR(300) | not null |
| research_abstract | TEXT | nullable |
| discipline | VARCHAR(150) | nullable |
| requested_amount | DECIMAL(15,2) | not null |
| project_duration_months | INT | nullable |
| institution_id | BIGINT | FK → institutions(id), nullable |
| submission_date | DATETIME(6) | nullable (set on submit) |
| status | ENUM | not null, default `DRAFT` |
| `+ base columns` | | |
- `status`: `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `AWARDED`, `DECLINED`, `WITHDRAWN`
- **Allowed transitions**: `DRAFT→SUBMITTED→UNDER_REVIEW→{AWARDED|DECLINED}`; `DRAFT/SUBMITTED→WITHDRAWN`.
- Indexes: `call_id`, `principal_investigator_id`, `status`.
- **Relationships**: 1 : N CoInvestigator, 1 : N ApplicationBudget, 1 : N ReviewerAssignment, 1 : 1 PanelDecision, 1 : 1 GrantAward.

### `co_investigators`
| Column | Type | Constraints |
|---|---|---|
| application_id | BIGINT | FK → grant_applications(id), not null |
| user_id | BIGINT | FK → users(id), nullable |
| institution_id | BIGINT | FK → institutions(id), nullable |
| role | ENUM | not null |
| contribution | VARCHAR(500) | nullable |
| status | ENUM | not null, default `INVITED` |
| `+ base columns` | | |
- `role`: `CO_INVESTIGATOR`, `RESEARCH_ASSISTANT`, `INDUSTRIAL_PARTNER`
- `status`: `INVITED`, `CONFIRMED`, `DECLINED`
- Indexes: `application_id`, `user_id`.

### `application_budgets`
| Column | Type | Constraints |
|---|---|---|
| application_id | BIGINT | FK → grant_applications(id), not null |
| budget_head | ENUM | not null |
| amount | DECIMAL(15,2) | not null |
| justification | VARCHAR(500) | nullable |
| `+ base columns` | | |
- `budget_head`: `PERSONNEL`, `EQUIPMENT`, `TRAVEL`, `CONSUMABLES`, `OVERHEAD`, `SUBCONTRACT`
- Index: `application_id`.

---

## 4. Review module

### `reviewer_assignments`
| Column | Type | Constraints |
|---|---|---|
| application_id | BIGINT | FK → grant_applications(id), not null |
| reviewer_id | BIGINT | FK → users(id), not null |
| assigned_date | DATE | not null |
| review_deadline | DATE | nullable |
| conflict_screening_status | ENUM | not null, default `CLEAR` |
| status | ENUM | not null, default `ASSIGNED` |
| `+ base columns` | | |
- `conflict_screening_status`: `CLEAR`, `COI_DECLARED`
- `status`: `ASSIGNED`, `ACCEPTED`, `DECLINED`, `SUBMITTED`
- **UNIQUE**(`application_id`, `reviewer_id`) — a reviewer is assigned at most once per application.
- Indexes: `application_id`, `reviewer_id`, `status`.
- **Relationship**: 1 : N ReviewScore.

### `review_scores`
| Column | Type | Constraints |
|---|---|---|
| assignment_id | BIGINT | FK → reviewer_assignments(id), not null |
| criterion | ENUM | not null |
| score | INT | not null, range 1–10 (validate in DTO) |
| comments | VARCHAR(1000) | nullable |
| overall_recommendation | ENUM | nullable |
| submitted_date | DATETIME(6) | nullable |
| `+ base columns` | | |
- `criterion`: `SCIENTIFIC_MERIT`, `FEASIBILITY`, `TEAM_EXPERTISE`, `IMPACT`, `INNOVATION`, `BUDGET_JUSTIFICATION`
- `overall_recommendation`: `FUND_AT_FULL_AMOUNT`, `FUND_AT_REDUCED`, `DO_NOT_FUND`
- **UNIQUE**(`assignment_id`, `criterion`) — one score per criterion per assignment.
- Index: `assignment_id`.

### `panel_decisions`
| Column | Type | Constraints |
|---|---|---|
| application_id | BIGINT | FK → grant_applications(id), not null, **UNIQUE** (1:1) |
| panel_date | DATE | nullable |
| consensus_score | DECIMAL(5,2) | nullable |
| award_decision | ENUM | not null |
| awarded_amount | DECIMAL(15,2) | nullable |
| conditions_attached | VARCHAR(1000) | nullable |
| decided_by_id | BIGINT | FK → users(id), nullable |
| `+ base columns` | | |
- `award_decision`: `FULL_AWARD`, `REDUCED_AWARD`, `RESERVE_LIST`, `REJECTED`

---

## 5. Award module

### `grant_awards`
| Column | Type | Constraints |
|---|---|---|
| application_id | BIGINT | FK → grant_applications(id), not null, **UNIQUE** (1:1) |
| awarded_amount | DECIMAL(15,2) | not null |
| start_date | DATE | nullable |
| end_date | DATE | nullable |
| conditions_ref | VARCHAR(255) | nullable |
| award_letter_date | DATE | nullable |
| status | ENUM | not null, default `ACTIVE` |
| `+ base columns` | | |
- `status`: `ACTIVE`, `SUSPENDED`, `COMPLETED`, `TERMINATED`
- **Relationships**: 1 : N DisbursementMilestone, 1 : N ProgressReport, 1 : N Deliverable, 1 : N ResearchOutput, 1 : N IPRecord.

---

## 6. Disbursement module

### `disbursement_milestones`
| Column | Type | Constraints |
|---|---|---|
| award_id | BIGINT | FK → grant_awards(id), not null |
| milestone_number | INT | not null |
| description | VARCHAR(500) | nullable |
| due_date | DATE | nullable |
| amount | DECIMAL(15,2) | not null |
| evidence_required | BOOLEAN | not null, default TRUE |
| status | ENUM | not null, default `UPCOMING` |
| `+ base columns` | | |
- `status`: `UPCOMING`, `EVIDENCE_SUBMITTED`, `APPROVED`, `DISBURSED`, `OVERDUE`
- **UNIQUE**(`award_id`, `milestone_number`).
- **Relationship**: 1 : N FundDisbursement (usually 1:1, model as 1:N for partial releases).

### `fund_disbursements`
| Column | Type | Constraints |
|---|---|---|
| milestone_id | BIGINT | FK → disbursement_milestones(id), not null |
| award_id | BIGINT | FK → grant_awards(id), not null |
| amount | DECIMAL(15,2) | not null |
| disbursed_date | DATE | nullable |
| receiving_account_ref | VARCHAR(100) | nullable |
| status | ENUM | not null, default `PENDING` |
| `+ base columns` | | |
- `status`: `PENDING`, `RELEASED`, `FAILED`
- Indexes: `milestone_id`, `award_id`.

---

## 7. Progress module

### `progress_reports`
| Column | Type | Constraints |
|---|---|---|
| award_id | BIGINT | FK → grant_awards(id), not null |
| period | VARCHAR(50) | nullable (e.g. "2026-Q1") |
| summary | TEXT | nullable |
| key_achievements | TEXT | nullable |
| challenges | TEXT | nullable |
| budget_utilisation_percent | DECIMAL(5,2) | nullable |
| submitted_by_id | BIGINT | FK → users(id), nullable |
| submitted_date | DATETIME(6) | nullable |
| status | ENUM | not null, default `DRAFT` |
| `+ base columns` | | |
- `status`: `DRAFT`, `SUBMITTED`, `APPROVED`, `REVISION_REQUESTED`
- Index: `award_id`, `status`.

### `deliverables`
| Column | Type | Constraints |
|---|---|---|
| award_id | BIGINT | FK → grant_awards(id), not null |
| title | VARCHAR(250) | not null |
| type | ENUM | not null |
| due_date | DATE | nullable |
| submitted_date | DATE | nullable |
| file_path | VARCHAR(500) | nullable |
| status | ENUM | not null, default `PENDING` |
| `+ base columns` | | |
- `type`: `REPORT`, `DATASET`, `PROTOTYPE`, `PUBLICATION`, `TRAINING`, `POLICY`
- `status`: `PENDING`, `SUBMITTED`, `ACCEPTED`, `REJECTED`
- Index: `award_id`, `status`.

---

## 8. Output module

### `research_outputs`
| Column | Type | Constraints |
|---|---|---|
| award_id | BIGINT | FK → grant_awards(id), not null |
| type | ENUM | not null |
| title | VARCHAR(300) | not null |
| authors | VARCHAR(1000) | nullable |
| publication_venue | VARCHAR(250) | nullable |
| doi | VARCHAR(100) | nullable |
| published_date | DATE | nullable |
| open_access_compliant | BOOLEAN | not null, default FALSE |
| status | ENUM | not null, default `IN_PREPARATION` |
| `+ base columns` | | |
- `type`: `JOURNAL_ARTICLE`, `CONFERENCE_PAPER`, `PATENT`, `DATASET`, `SOFTWARE`, `POLICY_BRIEF`
- `status`: `PUBLISHED`, `SUBMITTED`, `IN_PREPARATION`
- Indexes: `award_id`, `type`, `doi`.

### `ip_records`
| Column | Type | Constraints |
|---|---|---|
| award_id | BIGINT | FK → grant_awards(id), not null |
| ip_type | ENUM | not null |
| title | VARCHAR(300) | not null |
| inventors | VARCHAR(1000) | nullable |
| filing_date | DATE | nullable |
| grant_date | DATE | nullable |
| ownership_percent | DECIMAL(5,2) | nullable |
| status | ENUM | not null, default `FILED` |
| `+ base columns` | | |
- `ip_type`: `PATENT`, `COPYRIGHT`, `TRADEMARK`, `TRADE_SECRET`
- `status`: `FILED`, `GRANTED`, `ABANDONED`
- Index: `award_id`.

---

## 9. Notification module

### `notifications`
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | FK → users(id), not null |
| message | VARCHAR(1000) | not null |
| category | ENUM | not null |
| status | ENUM | not null, default `UNREAD` |
| `+ base columns` | | |
- `category`: `APPLICATION`, `REVIEW`, `AWARD`, `DISBURSEMENT`, `PROGRESS`, `OUTPUT`
- `status`: `UNREAD`, `READ`, `DISMISSED`
- Indexes: `user_id`, composite(`user_id`, `status`).

---

## 10. Relationship Summary (cardinalities)

```
sponsors            1 ── N  funding_schemes
funding_schemes     1 ── N  grant_calls
grant_calls         1 ── N  grant_applications
institutions        1 ── N  users
institutions        1 ── N  grant_applications
users               M ── N  roles                (via user_roles)
users               1 ── N  refresh_tokens
users               1 ── N  notifications
grant_applications  1 ── N  co_investigators
grant_applications  1 ── N  application_budgets
grant_applications  1 ── N  reviewer_assignments
grant_applications  1 ── 1  panel_decisions
grant_applications  1 ── 1  grant_awards
reviewer_assignments 1 ── N review_scores
grant_awards        1 ── N  disbursement_milestones
disbursement_milestones 1 ── N fund_disbursements
grant_awards        1 ── N  progress_reports
grant_awards        1 ── N  deliverables
grant_awards        1 ── N  research_outputs
grant_awards        1 ── N  ip_records
```

## 11. Indexing Strategy (summary)
- Unique: `users.email`, `roles.name`, `refresh_tokens.token`, `grant_awards.application_id`, `panel_decisions.application_id`, `reviewer_assignments(application_id, reviewer_id)`, `review_scores(assignment_id, criterion)`, `disbursement_milestones(award_id, milestone_number)`.
- All foreign-key columns indexed.
- All `status` / `category` columns used for list filtering indexed.
- Composite `(open_date, close_date)` on grant_calls for active-call queries; `(user_id, status)` on notifications for unread counts.

## 12. Design decisions to confirm with Claude before Phase 2
- Fetch types: default all `@ManyToOne` / `@OneToOne` to `LAZY`; never `EAGER`.
- Use a `BaseEntity` `@MappedSuperclass` for the audit/soft-delete columns rather than repeating them.
- Map enums with `@Enumerated(EnumType.STRING)` (never ORDINAL).
- For "blind review" (NFR §8): ensure reviewer-facing response DTOs exclude PI identity fields. Add this as an explicit DTO requirement.
- File storage for `deliverables.file_path` / evidence: store a path/key only; decide local disk vs cloud (out of Phase 1 per assumptions).
