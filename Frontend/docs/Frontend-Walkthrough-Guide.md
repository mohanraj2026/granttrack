# GrantTrack — End-to-End Frontend Walkthrough Guide

A step-by-step guide to drive the **entire** grant lifecycle from the UI, in the correct order —
from a researcher applying to a call, through review, award, disbursement, progress and outputs,
to award completion.

This mirrors the authoritative spec in `End-to-End-Process-Flow.md` and reflects the business
rules currently enforced by the backend.

---

## 0. Before you start

### Run the apps
- **Backend (microservices):** the backend is split into an **Eureka registry**, an **API gateway**,
  and the **auth / notification / finance / core** services (needs MySQL on `localhost:3306`). Start
  them in order — from `Backend/`, run `./run-all.ps1` (Windows), or start manually:
  `eureka-server` → `core-service` (migrates the DB) → `auth-service`, `notification-service`,
  `finance-service` → `api-gateway`. The frontend talks **only** to the gateway on
  **http://localhost:8080**. See `Backend/README.md` for details.
- **Frontend:** from `Frontend/`, run `npm start` (Angular dev server on **http://localhost:4200**).

### The one account that already exists
On first startup the backend seeds **only** a platform administrator:

| Field | Value |
|---|---|
| Email | `admin@granttrack.local` |
| Password | `Admin@12345` |
| Role | ROLE_ADMIN |

Everyone else must be created (below). Change the admin password after first login (Profile page).

### Two front doors
- **Researchers** use the public **Login** (`/login`) and self-service **Register** (`/register`) pages.
- **Staff** (Admin, Grant Admin, Finance Officer, Compliance Officer, Reviewer) use the **Staff Login** page (`/staff-login`).

### The redesigned interface (what to expect)
The front end has been rebuilt as an enterprise dashboard (deep-navy / emerald theme):
- The **dashboard is role-aware** — its summary cards and primary action change with your role.
- Each area (Funding, Reviews, Disbursements, Progress, Outputs) has **underlined section tabs** near the top.
- **Forms** are grouped into labelled sections with a leading icon per field, inline validation
  messages, and a 👁 show/hide toggle on passwords.
- **All monetary amounts are shown in ₹ (INR).**
- **Documents are real uploads**: milestone evidence, progress reports and deliverables let you attach
  an actual file, and the reviewing role can download it before deciding.
- The **notifications bell (top bar) shows a red unread-count badge** so new messages aren't missed.

### The six roles and what each one can see (left sidebar)
| Role | Menus visible | Part of the flow |
|---|---|---|
| **Admin** | Everything | Platform owner; can do any step |
| **Grant Admin** | Funding, Applications, Reviews, Awards, Disbursements, Progress, Outputs, User Admin | Runs calls, assignments, panel decisions, awards, completion |
| **Researcher** | Open Calls, Applications, Awards, Disbursements, Progress, Outputs | Applies, submits evidence/reports/deliverables/outputs |
| **Reviewer** | Reviews (blind) | Scores assigned applications only |
| **Finance Officer** | Awards, Disbursements, Progress, Outputs | Milestones, approvals, fund releases |
| **Compliance Officer** | Awards, Disbursements, Progress, Outputs | Reviews progress reports & deliverables; audit oversight |

> **You will switch accounts often.** No single user performs the whole lifecycle — that separation
> is the point. Use a private/incognito window (or log out) to switch roles cleanly.

---

## 1. Stage 0 — Set up accounts and funding (Admin / Grant Admin)

### 1a. Create the staff accounts — *log in as Admin* (Staff Login)
Go to **User Admin** (sidebar) → create one of each:
1. A **Grant Admin**
2. A **Reviewer**
3. A **Finance Officer**
4. A **Compliance Officer**

Set each user's email + a password; you'll log in as them later.
(Only the **Admin** can create Finance Officers and Grant Admins. A Grant Admin may create only
Researchers, Reviewers, and Compliance Officers.)

> **Editing users:** each row's ⋯ menu has an **Edit** action (name, email, phone, institution,
> department — role is fixed). The **system administrator's own row shows Edit only** — it has no
> Deactivate/Delete, because that account is permanent.

### 1b. Create the researcher account
Either create a **Researcher** in User Admin, **or** open a fresh browser and use **Register**
(`/register`) to self-sign-up as a researcher. Note the email + password.

### 1c. Set up the funding pipeline — *log in as Grant Admin* (or Admin)
Under **Funding** (sidebar), create the reference data in this order:
1. **Sponsors** → add a sponsor (the money source).
2. **Institutions** → add an institution (also used by the public register page).
3. **Schemes** → create a **Funding Scheme** (award range, dates, research area). Set it **ACTIVE**.
4. **Calls** → create a **Grant Call** under that scheme (title, open/close dates, review method,
   optional expected awards & total budget). Then **Open** the call.

**Rules enforced:** a call can only be **Opened** when its scheme is **ACTIVE** and the call's close
date is not in the past.

At this point a researcher can see the open call.

---

## 2. Stage 1 — Apply to a call (Researcher)

*Log in as the Researcher* (Login page).

1. Open **Open Calls** (sidebar) → browse the open call(s) and choose one to apply to.
2. This launches the **Application Wizard** (`/applications/new`): fill in project title, abstract,
   discipline, requested amount, duration, institution, co-investigators, and budget lines.
   Save — the application is created as **DRAFT**.
3. Go to **Applications** → open your draft → review it, then **Submit**.
   *Status: DRAFT → SUBMITTED.*

**Rules enforced:** you may only edit/submit your **own** draft; requested amount and dates are validated.
A submitted application locks for editing.

---

## 3. Stage 2 — Review & panel decision (Grant Admin + Reviewer)

### 3a. Move the application into review — *log in as Grant Admin*
1. **Applications** → open the submitted application → click **Move to Review**.
   *Application status: SUBMITTED → UNDER_REVIEW.* (This also advances the parent call
   OPEN → UNDER_REVIEW automatically.)

> This step is required: a panel decision can only be recorded once the application is UNDER_REVIEW.

### 3b. Assign a reviewer — *still as Grant Admin*
2. **Reviews → Assignments** → **Assign Reviewer**: pick the application, pick the **Reviewer**,
   set a review deadline. The reviewer receives a notification.

### 3c. Score (or decline) the application — *log in as the Reviewer* (Staff Login)
3. **Reviews → Queue** → open the assignment. In the **review gate** you may either:
   - **Accept** the invitation, then score each criterion (e.g. scientific merit) with an overall
     recommendation and comments, and **Submit** the review; **or**
   - **Decline assignment** — a **reason is required**. On decline the assignment **disappears from your
     queue** and the **Grant Admin is notified with your reason** so they can reassign.

**Rules enforced (blind review):** a reviewer sees only their own assignments; identifying applicant
details are withheld; a reviewer cannot see other reviewers' scores.

### 3d. Record the panel decision — *log in as Grant Admin*
5. **Reviews → Assignments** → **Record Panel Decision**: select the UNDER_REVIEW application.
   The dialog now shows the **submitted reviews** for that application (each reviewer's per-criterion
   scores, overall recommendation and comments) so you can decide with the evidence in front of you.
   Set the consensus, choose the outcome (**FULL_AWARD** or **REDUCED_AWARD** with an awarded amount),
   and — for an award — **assign a Finance Officer** (the picker searches **Finance-Officer users only**).
   Record it. *Application status: UNDER_REVIEW → AWARDED* (a rejection would set it to DECLINED).
   The parent call advances to **AWARDED**.

6. Recorded decisions appear in the **Panel Decisions** table on the same page — the Grant Admin can
   **Edit** the details (panel date, consensus, awarded amount, conditions, finance officer). The
   **award outcome itself is final** and not editable.

**What the researcher sees:** the panel decision (outcome, awarded amount, panel date, consensus,
conditions) is now shown on their **Application detail** page, so they're informed of the result.

**Rules enforced:** a panel decision requires the application to be UNDER_REVIEW with at least one
submitted review; an **award decision requires a Finance Officer to be assigned**.

---

## 4. Stage 3 — Create the award (Grant Admin)

*Still as Grant Admin.*

1. **Awards** → **Create Award**: pick the **AWARDED** application. The awarded amount is pre-filled
   from the panel decision. Set the start/end dates and conditions reference, then create.
   The award is created **ACTIVE**. The **Finance Officer assigned on the panel decision** is carried
   onto the award, its **finance review starts as PENDING**, and that officer is **notified**.
2. (Optional) **Approve** issues the formal award letter; **Change status** can later move the award
   to SUSPENDED / COMPLETED / TERMINATED.

**Rules enforced:** an award can only be created for an application that is **AWARDED** and has a
favourable panel decision, and the **awarded amount must not exceed the panel-approved amount**.
Exactly one award per application.

### 4a. Finance accepts (or rejects) the award — *log in as the Finance Officer*
The awarded grant now appears in the Finance Officer's **Awards** list. For the award **assigned to
them** (finance review = PENDING), the ⋯ menu offers **View application** and **Accept / Reject**:
- **Accept** → finance review **ACCEPTED**; the officer can now schedule milestones (below).
- **Reject** → a **reason is required**; finance review **REJECTED**; the Grant Admins are notified.

**Rule enforced:** for an award that has an assigned Finance Officer, **milestones cannot be created
until that officer has ACCEPTED** the award. (Awards with no assigned officer are unaffected.)

The award is now the anchor for everything that follows.

---

## 5. Stage 4 — Disbursement in stages (Finance Officer + Researcher)

### 5a. Schedule milestones — *log in as Finance Officer* (or Grant Admin)
1. **Disbursements → Milestones**: add milestones for the award (number, description, due date, amount,
   evidence-required flag). The **New milestone / Edit** buttons are available to the **Finance Officer**
   (previously only Grant Admin/Admin).

**Rules enforced:** milestones can only be added to an **ACTIVE** award whose finance review has been
**ACCEPTED** (when a Finance Officer is assigned), and the **sum of milestone amounts must not exceed
the award amount**.

### 5b. Submit milestone evidence — *log in as the Researcher*
2. **Disbursements → Milestones**: on your award's milestone, **Submit Evidence** — enter a short note
   **and attach a supporting document** (the document is required when the milestone is marked
   *Evidence required*).
   *Milestone: UPCOMING → EVIDENCE_SUBMITTED.*

**Rules enforced:** only the award's **principal investigator** may submit evidence for its milestones.

### 5c. Review evidence, approve, and release funds — *log in as Finance Officer* (or Grant Admin)
3. **Disbursements → Milestones**: **Review evidence** on the evidence-submitted milestone — read the
   note, **download the attached document**, then **Approve** (or **Return for revision** with a
   comment, which sends it back to the researcher).
   *Milestone: EVIDENCE_SUBMITTED → APPROVED.* (audited)
4. **Disbursements → Releases**: **Release Funds** for the approved milestone. The release captures the
   **beneficiary account**, the bank **payment reference (UTR/NEFT)**, and the **release date**.
   *Milestone: APPROVED → DISBURSED; a fund-release record is created.* (audited)
   The researcher is notified.

---

## 6. Stage 5 — Progress tracking (Researcher + Compliance Officer)

### 6a. Submit a progress report — *log in as the Researcher*
1. **Progress → Reports** → create a report (period, summary, achievements, challenges, budget
   utilisation %) and optionally **attach a report document** (PDF/Word). Save as **DRAFT**, then
   **Submit**. *Status: DRAFT → SUBMITTED.*

### 6b. Register & upload deliverables — *still as the Researcher*
2. **Progress → Deliverables** → create a deliverable (type, title, due date) — created **PENDING** —
   then **Upload & Submit** by choosing a real file (PDF/Word/Excel/image/ZIP). *Status: PENDING → SUBMITTED.*

### 6c. Review them — *log in as the Compliance Officer*
3. **Progress → Reports** → open the submitted report, **download its document**, then **Approve** or
   **Request Revision**. *SUBMITTED → APPROVED / REVISION_REQUESTED.* (audited; researcher notified)
4. **Progress → Deliverables** → **download the deliverable document**, then **Accept** or **Reject**.
   *SUBMITTED → ACCEPTED / REJECTED.* (audited; researcher notified)

**Loops:** if a report is sent back (**REVISION_REQUESTED**), the researcher can edit and re-submit it.
A **REJECTED** deliverable can be re-uploaded and re-submitted.

### 6d. See the health dashboard
5. **Progress → Overview** (pick the award at the top): budget-utilisation gauge, report/deliverable
   status donuts, funds-released bar, and milestone timeline.

**Rules enforced:** reports/deliverables can only be filed by the owning PI against an **ACTIVE**
award; a researcher sees only their own; staff (Grant Admin / Compliance / Finance / Admin) see all.

---

## 7. Stage 6 — Research outputs & IP (Researcher)

*Log in as the Researcher.*

1. **Outputs → Publications**: record research outputs (journal article, dataset, software, etc.)
   with authors, venue, DOI, published date, and open-access flag.
   *Status: IN_PREPARATION → SUBMITTED → PUBLISHED.*
2. **Outputs → IP**: record IP (patent, copyright, trademark, trade secret) with inventors,
   filing/grant dates, ownership %. *Status: FILED → GRANTED / ABANDONED.*

**Rules enforced:** only the owning PI may create/edit outputs & IP; a researcher sees only their own
(trade-secret IP is protected); the award need only **exist** (any status) — outputs legitimately
appear after the grant ends. Deletion is allowed for the owner or an Admin.

---

## 8. Stage 7 — Completion (Grant Admin)

*Log in as the Grant Admin.*

1. When deliverables are accepted, funds are disbursed, and outputs recorded, go to **Awards** →
   open the award → **Change status → COMPLETED**.

The full history — every decision, payment, report and output — stays linked to the award.

---

## 9. Cross-cutting: Notifications & Audit

- **Notifications** (all roles): the **top-bar bell shows a red unread-count badge**; the
  **Notification Center** shows your own messages (reviewer assignment/decline, award, finance
  assignment, disbursement, progress outcomes). Unread items carry a left accent bar and a **New**
  badge. Filter by status/category, then mark read / dismiss; you only ever see your own.
  **Admin / Grant Admin** can also **Send notification** to a specific user from a form here
  (choose user, category, message).
- **Audit trail** (Compliance Officer / Admin): every sensitive action (award create/approve/status,
  milestone approve, fund release, application submit/status, review score, panel decision, progress &
  deliverable reviews, and user create/status/delete) is written to an append-only audit log,
  readable via `GET /api/v1/audit-logs` (filter by user, entity type, action, record, date range).
  > **Note:** this read API currently has **no front-end screen** — it's a backend endpoint only.
  > A compliance/admin audit view is a candidate for the next UI iteration.

---

## Quick reference — full happy path in order

1. **Admin:** create Grant Admin, Reviewer, Finance Officer, Compliance Officer; create/allow a Researcher.
2. **Grant Admin:** Funding → Sponsor → Institution → Scheme (ACTIVE) → Call (Open).
3. **Researcher:** Open Calls → apply → Applications → Submit.
4. **Grant Admin:** Applications → Move to Review; Reviews → Assignments → Assign Reviewer.
5. **Reviewer:** Reviews → Queue → Accept → Score → Submit *(or Decline with a reason)*.
6. **Grant Admin:** Reviews → Assignments → review the submitted reviews → Record Panel Decision (FULL/REDUCED award **+ assign a Finance Officer**).
7. **Grant Admin:** Awards → Create Award → (Approve). *(Finance Officer is notified; award finance-review = PENDING.)*
8. **Finance Officer:** Awards → **Accept** the assigned award (or Reject with reason); then Disbursements → Milestones → add milestones.
9. **Researcher:** Disbursements → Milestones → Submit Evidence (note + document).
10. **Finance Officer:** Disbursements → Milestones → Review evidence → Approve; Releases → Release Funds (beneficiary + UTR + date).
11. **Researcher:** Progress → Reports (submit, attach document) & Deliverables (Upload & Submit a file).
12. **Compliance Officer:** Progress → Reports (download → Approve) & Deliverables (download → Accept).
13. **Researcher:** Outputs → Publications & IP.
14. **Grant Admin:** Awards → Change status → COMPLETED.

---

## Tips for testing & giving feedback
- If an action is blocked with a message, that's usually an **enforced business rule** (e.g. "award
  must be ACTIVE", "not your notification", "amount exceeds panel amount") — note the message when
  reporting.
- A **403** means the current role isn't allowed that action; a **400** means invalid input/state.
- To report a change, tell me the **stage + screen + role + what you expected vs. saw** and I'll trace it.
