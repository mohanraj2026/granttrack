# GrantTrack — End-to-End API Testing Guide

A single, ordered walkthrough that exercises **every** endpoint with realistic example data. Follow it top-to-bottom: each step explains its **purpose**, the **role/token** to use, the **request + body**, and what **id to capture** for later steps.

> **Base URL:** `http://localhost:8088` (the configured default; change if you ran on another port).
> Set a variable once: `BASE=http://localhost:8088/api/v1`
>
> **Auth model:** call `/auth/login`, copy `data.accessToken`, and send it as `Authorization: Bearer <token>` on every protected call. Different actors (admin, researcher, reviewer, finance officer, compliance officer) need **their own** token — log in as each when the step says so.
>
> **IDs:** the ids below assume a **fresh database** (auto-increment starts at 1). If yours differ, substitute the id returned by each create call. The bootstrapped admin is always user **id 1**.

---

## Actors created in this guide

| Actor | Email | Password | Role | User id (fresh DB) |
|---|---|---|---|---|
| System Admin (bootstrapped) | `admin@granttrack.local` | `Admin@12345` | `ROLE_ADMIN` | 1 |
| Principal Investigator (PI) | `ada@uni.edu` | `Secret@123` | `ROLE_RESEARCHER` | 2 |
| Peer Reviewer | `rob@review.org` | `Secret@123` | `ROLE_REVIEWER` | 3 |
| Finance Officer | `fin@granttrack.local` | `Secret@123` | `ROLE_FINANCE_OFFICER` | 4 |
| Compliance Officer | `comp@granttrack.local` | `Secret@123` | `ROLE_COMPLIANCE_OFFICER` | 5 |
| Co-Investigator | `ben@uni.edu` | `Secret@123` | `ROLE_RESEARCHER` | 6 |

---

# PHASE 0 — Authenticate as Admin

### 0.1 Login as Admin
**Purpose:** obtain the admin access token used for all configuration steps (reference data, schemes, calls, reviewer assignment, awards, milestones, notifications).
`POST {BASE}/auth/login`
```json
{ "email": "admin@granttrack.local", "password": "Admin@12345" }
```
**Capture:** `data.accessToken` → call it **ADMIN_TOKEN**.

```bash
curl -s -X POST http://localhost:8088/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@granttrack.local","password":"Admin@12345"}'
```

---

# PHASE 1 — Reference data (Admin)

### 1.1 Create Institution (host university)
**Purpose:** institutions are referenced by users and applications. Create the PI's institution.
`POST {BASE}/funding/institutions`  ·  *Bearer ADMIN_TOKEN*
```json
{ "name": "University of Example", "type": "University", "country": "United Kingdom" }
```
**Capture:** `data.id` → **INSTITUTION_ID = 1**

### 1.2 Create a second Institution (industrial partner)
**Purpose:** used by the co-investigator (industrial partner) later.
`POST {BASE}/funding/institutions`  ·  *Bearer ADMIN_TOKEN*
```json
{ "name": "Acme R&D Labs", "type": "Corporate", "country": "United Kingdom" }
```
**Capture:** **INSTITUTION_ID_2 = 2**

### 1.3 Create Sponsor
**Purpose:** the funding body that owns funding schemes.
`POST {BASE}/funding/sponsors`  ·  *Bearer ADMIN_TOKEN*
```json
{ "name": "National Science Foundation", "type": "Govt", "contactEmail": "grants@nsf.example" }
```
**Capture:** **SPONSOR_ID = 1**

### 1.4 List sponsors (verify + pagination)
**Purpose:** confirm the sponsor exists and demonstrate paged listing.
`GET {BASE}/funding/sponsors?q=science&page=0&size=10`  ·  *Bearer ADMIN_TOKEN*

---

# PHASE 2 — Funding scheme & call (Admin)

### 2.1 Create Funding Scheme
**Purpose:** defines award range, discipline and duration under a sponsor. Calls are opened under a scheme.
`POST {BASE}/funding/schemes`  ·  *Bearer ADMIN_TOKEN*
```json
{
  "schemeName": "Frontier AI Research Grant",
  "sponsorId": 1,
  "researchArea": "Artificial Intelligence",
  "maxAwardAmount": 500000.00,
  "minAwardAmount": 50000.00,
  "eligibleApplicants": "Universities, Research Councils",
  "fundingDurationMonths": 36
}
```
**Capture:** **SCHEME_ID = 1**

### 2.2 Update Funding Scheme
**Purpose:** show editing scheme metadata (e.g. broaden eligibility).
`PUT {BASE}/funding/schemes/1`  ·  *Bearer ADMIN_TOKEN*
```json
{
  "schemeName": "Frontier AI Research Grant",
  "sponsorId": 1,
  "researchArea": "Artificial Intelligence & Data Science",
  "maxAwardAmount": 600000.00,
  "minAwardAmount": 50000.00,
  "eligibleApplicants": "Universities, Research Councils, Industry partners",
  "fundingDurationMonths": 36
}
```

### 2.3 Create Grant Call
**Purpose:** a submission window under the scheme. Applications attach to a call.
`POST {BASE}/funding/calls`  ·  *Bearer ADMIN_TOKEN*
```json
{
  "schemeId": 1,
  "callTitle": "2026 Frontier AI Call",
  "openDate": "2026-07-01",
  "closeDate": "2026-12-31",
  "expectedAwards": 10,
  "totalBudgetAllocated": 4000000.00,
  "reviewMethod": "DOUBLE_BLIND"
}
```
**Capture:** **CALL_ID = 1** (created in status `UPCOMING`)

### 2.4 Open the Grant Call
**Purpose:** transition `UPCOMING → OPEN` so applications can be submitted. (Submitting to a non-open call is rejected.)
`POST {BASE}/funding/calls/1/open`  ·  *Bearer ADMIN_TOKEN*

### 2.5 List open calls
**Purpose:** what a researcher would browse.
`GET {BASE}/funding/calls?status=OPEN&schemeId=1&sort=openDate,desc`  ·  *Bearer ADMIN_TOKEN*

---

# PHASE 3 — Register the other actors (public endpoint)

`/auth/register` is public — no token needed. Run all five.

### 3.1 Register PI (researcher)
**Purpose:** the applicant who owns the grant application.
`POST {BASE}/auth/register`
```json
{ "name": "Dr. Ada Researcher", "email": "ada@uni.edu", "password": "Secret@123",
  "phone": "9010000005", "institutionId": 1, "department": "Computer Science",
  "roles": ["ROLE_RESEARCHER"] }
```
**Capture:** **PI_ID = 2**

### 3.2 Register Reviewer
**Purpose:** performs blind peer review.
`POST {BASE}/auth/register`
```json
{ "name": "Dr. Rob Reviewer", "email": "rob@review.org", "password": "Secret@123",
  "institutionId": 2, "department": "Machine Learning", "roles": ["ROLE_REVIEWER"] }
```
**Capture:** **REVIEWER_ID = 3**

### 3.3 Register Finance Officer
**Purpose:** approves milestones and releases funds.
`POST {BASE}/auth/register`
```json
{ "name": "Fran Finance", "email": "fin@granttrack.local", "password": "Secret@123",
  "roles": ["ROLE_FINANCE_OFFICER"] }
```
**Capture:** **FINANCE_ID = 4**

### 3.4 Register Compliance Officer
**Purpose:** reviews progress reports and deliverables.
`POST {BASE}/auth/register`
```json
{ "name": "Carl Compliance", "email": "comp@granttrack.local", "password": "Secret@123",
  "roles": ["ROLE_COMPLIANCE_OFFICER"] }
```
**Capture:** **COMPLIANCE_ID = 5**

### 3.5 Register Co-Investigator
**Purpose:** a second researcher added to the application team.
`POST {BASE}/auth/register`
```json
{ "name": "Dr. Ben Co", "email": "ben@uni.edu", "password": "Secret@123",
  "institutionId": 1, "department": "Statistics", "roles": ["ROLE_RESEARCHER"] }
```
**Capture:** **COI_USER_ID = 6**

---

# PHASE 4 — Grant Application (PI)

### 4.1 Login as PI
**Purpose:** get the researcher token for application steps.
`POST {BASE}/auth/login`
```json
{ "email": "ada@uni.edu", "password": "Secret@123" }
```
**Capture:** **PI_TOKEN**

### 4.2 Create Application (DRAFT)
**Purpose:** start a grant application against the open call. Created in `DRAFT`.
`POST {BASE}/applications`  ·  *Bearer PI_TOKEN*
```json
{
  "callId": 1,
  "projectTitle": "Explainable Models for Genomic Medicine",
  "researchAbstract": "We propose interpretable deep models for clinical genomics...",
  "discipline": "Bioinformatics",
  "requestedAmount": 320000.00,
  "projectDurationMonths": 24,
  "institutionId": 1
}
```
**Capture:** **APPLICATION_ID = 1**

### 4.3 Update the draft
**Purpose:** edit allowed only while `DRAFT`.
`PUT {BASE}/applications/1`  ·  *Bearer PI_TOKEN*
```json
{
  "callId": 1,
  "projectTitle": "Explainable Models for Genomic Medicine (rev. 2)",
  "researchAbstract": "Revised abstract with preliminary results...",
  "discipline": "Bioinformatics",
  "requestedAmount": 300000.00,
  "projectDurationMonths": 24,
  "institutionId": 1
}
```

### 4.4 Add a Co-Investigator
**Purpose:** build the project team.
`POST {BASE}/applications/1/co-investigators`  ·  *Bearer PI_TOKEN*
```json
{ "userId": 6, "institutionId": 1, "role": "CO_INVESTIGATOR",
  "contribution": "Statistical methodology", "status": "INVITED" }
```

### 4.5 Add another team member (industrial partner)
**Purpose:** demonstrate the partner role + second institution.
`POST {BASE}/applications/1/co-investigators`  ·  *Bearer PI_TOKEN*
```json
{ "institutionId": 2, "role": "INDUSTRIAL_PARTNER",
  "contribution": "Clinical dataset access", "status": "INVITED" }
```

### 4.6 List co-investigators
`GET {BASE}/applications/1/co-investigators`  ·  *Bearer PI_TOKEN*

### 4.7 Add Budget lines
**Purpose:** detailed budget breakdown by head. Add several.
`POST {BASE}/applications/1/budgets`  ·  *Bearer PI_TOKEN*
```json
{ "budgetHead": "PERSONNEL", "amount": 180000.00, "justification": "2 postdocs for 24 months" }
```
`POST {BASE}/applications/1/budgets`
```json
{ "budgetHead": "EQUIPMENT", "amount": 60000.00, "justification": "GPU server" }
```
`POST {BASE}/applications/1/budgets`
```json
{ "budgetHead": "TRAVEL", "amount": 15000.00, "justification": "Conferences & site visits" }
```

### 4.8 List budget lines
`GET {BASE}/applications/1/budgets`  ·  *Bearer PI_TOKEN*

### 4.9 Submit the Application
**Purpose:** transition `DRAFT → SUBMITTED`, stamps submission date. Requires the call to be `OPEN`.
`POST {BASE}/applications/1/submit`  ·  *Bearer PI_TOKEN*

> Optional negative test: `POST /applications/1/withdraw` moves it to `WITHDRAWN`. Skip if you want to continue the happy path (a withdrawn app can't be reviewed).

---

# PHASE 5 — Peer Review

### 5.1 (Admin) Move application into review
**Purpose:** `SUBMITTED → UNDER_REVIEW` before reviewers are assigned.
`PATCH {BASE}/applications/1/status?status=UNDER_REVIEW`  ·  *Bearer ADMIN_TOKEN*

### 5.2 (Admin) Assign a Reviewer
**Purpose:** create a reviewer assignment (blind). A reviewer can be assigned to an application at most once.
`POST {BASE}/reviews/assignments`  ·  *Bearer ADMIN_TOKEN*
```json
{ "applicationId": 1, "reviewerId": 3, "reviewDeadline": "2026-10-15" }
```
**Capture:** **ASSIGNMENT_ID = 1**

### 5.3 (Reviewer) Login
`POST {BASE}/auth/login`
```json
{ "email": "rob@review.org", "password": "Secret@123" }
```
**Capture:** **REVIEWER_TOKEN**

### 5.4 (Reviewer) Conflict-of-interest screening
**Purpose:** declare no conflict (`CLEAR`) — or `COI_DECLARED` to flag one.
`POST {BASE}/reviews/assignments/1/conflict-check?status=CLEAR`  ·  *Bearer REVIEWER_TOKEN*

### 5.5 (Reviewer) Accept the assignment
**Purpose:** `ASSIGNED → ACCEPTED`. Must accept before scoring/submitting.
`POST {BASE}/reviews/assignments/1/respond?decision=ACCEPT`  ·  *Bearer REVIEWER_TOKEN*

### 5.6 (Reviewer) Submit scores — one per criterion
**Purpose:** structured scoring (1–10). A criterion can be scored once per assignment.
`POST {BASE}/reviews/assignments/1/scores`  ·  *Bearer REVIEWER_TOKEN*
```json
{ "criterion": "SCIENTIFIC_MERIT", "score": 8, "comments": "Strong methodology",
  "overallRecommendation": "FUND_AT_FULL_AMOUNT" }
```
```json
{ "criterion": "FEASIBILITY", "score": 7, "comments": "Achievable within 24 months" }
```
```json
{ "criterion": "TEAM_EXPERTISE", "score": 9, "comments": "Excellent team" }
```
```json
{ "criterion": "IMPACT", "score": 8, "comments": "High clinical impact" }
```
```json
{ "criterion": "INNOVATION", "score": 7, "comments": "Novel interpretability angle" }
```
```json
{ "criterion": "BUDGET_JUSTIFICATION", "score": 6, "comments": "Equipment slightly high" }
```

### 5.7 (Reviewer) List submitted scores
`GET {BASE}/reviews/assignments/1/scores`  ·  *Bearer REVIEWER_TOKEN*

### 5.8 (Reviewer) Submit the completed review
**Purpose:** `ACCEPTED → SUBMITTED` (finalises the reviewer's work).
`POST {BASE}/reviews/assignments/1/submit`  ·  *Bearer REVIEWER_TOKEN*

### 5.9 (Admin) List assignments
**Purpose:** monitor review progress.
`GET {BASE}/reviews/assignments?applicationId=1&status=SUBMITTED`  ·  *Bearer ADMIN_TOKEN*

### 5.10 (Admin) Record Panel Decision
**Purpose:** the funding panel's consensus & recommendation (one per application). Drives award eligibility.
`POST {BASE}/reviews/applications/1/panel-decision`  ·  *Bearer ADMIN_TOKEN*
```json
{ "panelDate": "2026-10-20", "consensusScore": 7.50, "awardDecision": "FULL_AWARD",
  "awardedAmount": 300000.00, "conditionsAttached": "Annual progress reporting" }
```

### 5.11 (Admin) Get the panel decision
`GET {BASE}/reviews/applications/1/panel-decision`  ·  *Bearer ADMIN_TOKEN*

### 5.12 (Admin) Mark application AWARDED
**Purpose:** `UNDER_REVIEW → AWARDED`.
`PATCH {BASE}/applications/1/status?status=AWARDED`  ·  *Bearer ADMIN_TOKEN*

---

# PHASE 6 — Award (Admin)

### 6.1 Create the Award
**Purpose:** issue a grant award for the application (one award per application).
`POST {BASE}/awards`  ·  *Bearer ADMIN_TOKEN*
```json
{ "applicationId": 1, "awardedAmount": 300000.00,
  "startDate": "2026-11-01", "endDate": "2028-10-31", "conditionsRef": "T&C-2026-AI-001" }
```
**Capture:** **AWARD_ID = 1**

### 6.2 Approve the Award
**Purpose:** stamps the award-letter date and ensures status `ACTIVE`.
`POST {BASE}/awards/1/approve`  ·  *Bearer ADMIN_TOKEN*

### 6.3 Get / List awards
`GET {BASE}/awards/1`  ·  *Bearer ADMIN_TOKEN*
`GET {BASE}/awards?status=ACTIVE`  ·  *Bearer ADMIN_TOKEN*

### 6.4 (Optional) Change award status
**Purpose:** demonstrate lifecycle transition (e.g. suspend then reactivate).
`PATCH {BASE}/awards/1/status?status=SUSPENDED`  ·  *Bearer ADMIN_TOKEN*
`PATCH {BASE}/awards/1/status?status=ACTIVE`  ·  *Bearer ADMIN_TOKEN*

---

# PHASE 7 — Disbursement

### 7.1 (Admin) Create a Disbursement Milestone
**Purpose:** milestone-based fund release schedule (unique per award + number).
`POST {BASE}/disbursements/milestones`  ·  *Bearer ADMIN_TOKEN*
```json
{ "awardId": 1, "milestoneNumber": 1, "description": "Kick-off & ethics approval",
  "dueDate": "2027-01-31", "amount": 100000.00, "evidenceRequired": true }
```
**Capture:** **MILESTONE_ID = 1**

### 7.2 (Admin) Add a second milestone
`POST {BASE}/disbursements/milestones`  ·  *Bearer ADMIN_TOKEN*
```json
{ "awardId": 1, "milestoneNumber": 2, "description": "Mid-project deliverables",
  "dueDate": "2027-09-30", "amount": 120000.00, "evidenceRequired": true }
```

### 7.3 (PI) Submit milestone evidence
**Purpose:** `UPCOMING → EVIDENCE_SUBMITTED` (researcher claims completion).
`POST {BASE}/disbursements/milestones/1/submit-evidence`  ·  *Bearer PI_TOKEN*

### 7.4 (Finance Officer) Login
`POST {BASE}/auth/login`
```json
{ "email": "fin@granttrack.local", "password": "Secret@123" }
```
**Capture:** **FINANCE_TOKEN**

### 7.5 (Finance) Approve the milestone
**Purpose:** `EVIDENCE_SUBMITTED → APPROVED`. (Admin cannot — this requires FINANCE_OFFICER or GRANT_ADMIN.)
`POST {BASE}/disbursements/milestones/1/approve`  ·  *Bearer FINANCE_TOKEN*

### 7.6 (Finance) Release funds
**Purpose:** creates a `FundDisbursement` (RELEASED) and sets the milestone `DISBURSED`. This action is **audit-logged**.
`POST {BASE}/disbursements/milestones/1/release`  ·  *Bearer FINANCE_TOKEN*
```json
{ "receivingAccountRef": "GB29-NWBK-0000-0000-1234" }
```

### 7.7 List disbursements for the award
`GET {BASE}/disbursements?awardId=1`  ·  *Bearer FINANCE_TOKEN*

---

# PHASE 8 — Progress & Deliverables

### 8.1 (PI) Create a Progress Report (DRAFT)
**Purpose:** periodic reporting against the award.
`POST {BASE}/progress/reports`  ·  *Bearer PI_TOKEN*
```json
{ "awardId": 1, "period": "2027-Q1", "summary": "Set up infrastructure and ethics.",
  "keyAchievements": "Cluster provisioned; IRB approval obtained.",
  "challenges": "Recruitment delays.", "budgetUtilisationPercent": 22.50 }
```
**Capture:** **REPORT_ID = 1**

### 8.2 (PI) Submit the Progress Report
**Purpose:** `DRAFT → SUBMITTED`; stamps submitter + date.
`POST {BASE}/progress/reports/1/submit`  ·  *Bearer PI_TOKEN*

### 8.3 (Compliance Officer) Login
`POST {BASE}/auth/login`
```json
{ "email": "comp@granttrack.local", "password": "Secret@123" }
```
**Capture:** **COMPLIANCE_TOKEN**

### 8.4 (Compliance) Review the report
**Purpose:** `SUBMITTED → APPROVED` (or `REQUEST_REVISION`). Audit-logged.
`POST {BASE}/progress/reports/1/review?decision=APPROVE`  ·  *Bearer COMPLIANCE_TOKEN*
> Variation: `?decision=REQUEST_REVISION` → status `REVISION_REQUESTED`.

### 8.5 (PI) Create a Deliverable (PENDING)
**Purpose:** register an expected deliverable.
`POST {BASE}/progress/deliverables`  ·  *Bearer PI_TOKEN*
```json
{ "awardId": 1, "title": "Interim Technical Report", "type": "REPORT", "dueDate": "2027-06-30" }
```
**Capture:** **DELIVERABLE_ID = 1**

### 8.6 (PI) Upload the deliverable file (path/key only)
**Purpose:** `PENDING → SUBMITTED`; stores a storage path/key (no binary upload in Phase 1).
`POST {BASE}/progress/deliverables/1/upload`  ·  *Bearer PI_TOKEN*
```json
{ "filePath": "s3://granttrack-deliverables/award-1/interim-report.pdf" }
```

### 8.7 (Compliance) Review the deliverable
**Purpose:** `SUBMITTED → ACCEPTED` (or `REJECT`).
`POST {BASE}/progress/deliverables/1/review?decision=ACCEPT`  ·  *Bearer COMPLIANCE_TOKEN*

### 8.8 List reports & deliverables
`GET {BASE}/progress/reports?awardId=1`  ·  *Bearer COMPLIANCE_TOKEN*
`GET {BASE}/progress/deliverables?awardId=1`  ·  *Bearer COMPLIANCE_TOKEN*

---

# PHASE 9 — Research Output & IP (PI)

### 9.1 Add a Research Output (publication)
**Purpose:** record a journal article produced by the grant.
`POST {BASE}/outputs`  ·  *Bearer PI_TOKEN*
```json
{ "awardId": 1, "type": "JOURNAL_ARTICLE", "title": "Explainable Genomic Models",
  "authors": "Ada Researcher, Ben Co", "publicationVenue": "Nature Methods",
  "doi": "10.1000/xyz123", "publishedDate": "2027-06-01",
  "openAccessCompliant": true, "status": "PUBLISHED" }
```
**Capture:** **OUTPUT_ID = 1**

### 9.2 Update the output
**Purpose:** correct/enrich metadata.
`PUT {BASE}/outputs/1`  ·  *Bearer PI_TOKEN*
```json
{ "awardId": 1, "type": "JOURNAL_ARTICLE", "title": "Explainable Genomic Models (camera-ready)",
  "authors": "Ada Researcher, Ben Co", "publicationVenue": "Nature Methods",
  "doi": "10.1000/xyz123", "publishedDate": "2027-06-15",
  "openAccessCompliant": true, "status": "PUBLISHED" }
```

### 9.3 Add an IP Record (patent)
**Purpose:** record intellectual property arising from the grant.
`POST {BASE}/outputs/ip`  ·  *Bearer PI_TOKEN*
```json
{ "awardId": 1, "ipType": "PATENT", "title": "Method for Interpretable Genomic Inference",
  "inventors": "Ada Researcher", "filingDate": "2027-07-01",
  "ownershipPercent": 60.00, "status": "FILED" }
```
**Capture:** **IP_ID = 1**

### 9.4 Search outputs & list IP
`GET {BASE}/outputs?awardId=1&type=JOURNAL_ARTICLE`  ·  *Bearer PI_TOKEN*
`GET {BASE}/outputs/ip?awardId=1`  ·  *Bearer PI_TOKEN*

---

# PHASE 10 — Notifications

### 10.1 (Admin) Create a notification for the PI
**Purpose:** in-app message to a user (internal/admin use; other modules also publish these programmatically).
`POST {BASE}/notifications`  ·  *Bearer ADMIN_TOKEN*
```json
{ "userId": 2, "message": "Your grant has been awarded. Congratulations!", "category": "AWARD" }
```
**Capture:** **NOTIFICATION_ID = 1**

### 10.2 (PI) List my notifications
**Purpose:** the PI sees only their own notifications.
`GET {BASE}/notifications?status=UNREAD`  ·  *Bearer PI_TOKEN*

### 10.3 (PI) Unread count
`GET {BASE}/notifications/unread-count`  ·  *Bearer PI_TOKEN*

### 10.4 (PI) Mark as read
`PATCH {BASE}/notifications/1/read`  ·  *Bearer PI_TOKEN*

### 10.5 (PI) Dismiss
`PATCH {BASE}/notifications/1/dismiss`  ·  *Bearer PI_TOKEN*

---

# PHASE 11 — User administration (Admin)

### 11.1 Search/list users
**Purpose:** admin user directory with pagination + filter.
`GET {BASE}/users?q=ada&status=ACTIVE&page=0&size=10`  ·  *Bearer ADMIN_TOKEN*

### 11.2 Get a user by id
`GET {BASE}/users/2`  ·  *Bearer ADMIN_TOKEN*

### 11.3 Deactivate / reactivate a user
**Purpose:** soft account control.
`PATCH {BASE}/users/6/status?status=INACTIVE`  ·  *Bearer ADMIN_TOKEN*
`PATCH {BASE}/users/6/status?status=ACTIVE`  ·  *Bearer ADMIN_TOKEN*

---

# PHASE 12 — Auth lifecycle (any user)

### 12.1 Refresh access token
**Purpose:** obtain a new access token (rotates the refresh token). Use a `refreshToken` captured from a login response.
`POST {BASE}/auth/refresh`
```json
{ "refreshToken": "<paste a refreshToken from a login response>" }
```

### 12.2 Change password (PI)
**Purpose:** authenticated password change; revokes the user's refresh tokens.
`POST {BASE}/auth/change-password`  ·  *Bearer PI_TOKEN*
```json
{ "currentPassword": "Secret@123", "newPassword": "NewSecret@456" }
```

### 12.3 Forgot password (structure only)
**Purpose:** Phase-1 stub — always returns success (no email delivery); avoids user enumeration.
`POST {BASE}/auth/forgot-password`
```json
{ "email": "ada@uni.edu" }
```

### 12.4 Logout
**Purpose:** revoke a refresh token.
`POST {BASE}/auth/logout`  ·  *Bearer PI_TOKEN*
```json
{ "refreshToken": "<the PI's current refreshToken>" }
```

---

# Appendix A — Negative / validation tests (optional)

| Test | Request | Expected |
|---|---|---|
| Missing/invalid token | any protected GET without `Authorization` | **401** standard envelope |
| Wrong role | PI calls `POST /funding/schemes` | **403** access denied |
| Validation failure | `POST /auth/register` with `"email":"bad"` | **400** with `fieldErrors` |
| Not found | `GET /awards/999` | **404** |
| Bad state transition | `POST /funding/calls/1/open` when already OPEN | **409** business error |
| Duplicate | second `POST /awards` for application 1 | **409** duplicate |
| Duplicate reviewer | assign reviewer 3 to application 1 twice | **409** duplicate |

---

# Appendix B — Enum reference (valid values)

- **Scheme status:** `ACTIVE, CLOSED, SUSPENDED`
- **Call review method:** `SINGLE_BLIND, DOUBLE_BLIND, PANEL` · **Call status:** `UPCOMING, OPEN, UNDER_REVIEW, AWARDED, CLOSED`
- **Application status:** `DRAFT, SUBMITTED, UNDER_REVIEW, AWARDED, DECLINED, WITHDRAWN`
- **Co-investigator role:** `CO_INVESTIGATOR, RESEARCH_ASSISTANT, INDUSTRIAL_PARTNER` · **status:** `INVITED, CONFIRMED, DECLINED`
- **Budget head:** `PERSONNEL, EQUIPMENT, TRAVEL, CONSUMABLES, OVERHEAD, SUBCONTRACT`
- **Conflict screening:** `CLEAR, COI_DECLARED` · **Assignment status:** `ASSIGNED, ACCEPTED, DECLINED, SUBMITTED`
- **Review criterion:** `SCIENTIFIC_MERIT, FEASIBILITY, TEAM_EXPERTISE, IMPACT, INNOVATION, BUDGET_JUSTIFICATION`
- **Overall recommendation:** `FUND_AT_FULL_AMOUNT, FUND_AT_REDUCED, DO_NOT_FUND`
- **Award decision:** `FULL_AWARD, REDUCED_AWARD, RESERVE_LIST, REJECTED`
- **Award status:** `ACTIVE, SUSPENDED, COMPLETED, TERMINATED`
- **Milestone status:** `UPCOMING, EVIDENCE_SUBMITTED, APPROVED, DISBURSED, OVERDUE` · **Disbursement status:** `PENDING, RELEASED, FAILED`
- **Progress status:** `DRAFT, SUBMITTED, APPROVED, REVISION_REQUESTED`
- **Deliverable type:** `REPORT, DATASET, PROTOTYPE, PUBLICATION, TRAINING, POLICY` · **status:** `PENDING, SUBMITTED, ACCEPTED, REJECTED`
- **Output type:** `JOURNAL_ARTICLE, CONFERENCE_PAPER, PATENT, DATASET, SOFTWARE, POLICY_BRIEF` · **status:** `PUBLISHED, SUBMITTED, IN_PREPARATION`
- **IP type:** `PATENT, COPYRIGHT, TRADEMARK, TRADE_SECRET` · **status:** `FILED, GRANTED, ABANDONED`
- **Notification category:** `APPLICATION, REVIEW, AWARD, DISBURSEMENT, PROGRESS, OUTPUT` · **status:** `UNREAD, READ, DISMISSED`

---

# Appendix C — Fastest way to run it

Use **Swagger UI** at `http://localhost:8088/swagger-ui.html`:
1. `POST /auth/login` (admin) → copy `accessToken`.
2. Click **Authorize** (top-right), paste the token, **Authorize**.
3. Execute the calls in the order above. Re-**Authorize** with a different user's token when a phase says "Login as …".

Or import the endpoints into Postman from the OpenAPI spec at `http://localhost:8088/v3/api-docs` and set a `{{token}}` collection variable.
