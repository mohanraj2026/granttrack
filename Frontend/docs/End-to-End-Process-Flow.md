# GrantTrack — End-to-End Process Flow

A plain-language, scenario-based walkthrough of the **entire journey of a research grant** in GrantTrack — from the moment a funding programme is set up, through a Researcher's application, all the way to the published results — and **exactly what each role does at every step**.

> Read this top to bottom and you'll never be confused about "who does what" again.

---

## Part A — The six people in the story (roles)

Everyone in GrantTrack has one or more **roles**. A role decides which screens and buttons they get. Here they are in plain words:

| Role | Real-world person | One-line job |
|---|---|---|
| **ROLE_RESEARCHER** | Principal Investigator (PI) | Asks for the money — writes & submits applications, runs the project, reports results. |
| **ROLE_REVIEWER** | Peer Reviewer | Judges applications fairly (often without seeing who applied). |
| **ROLE_GRANT_ADMIN** | Grant Administrator | Runs the pipeline — assigns reviewers, records the panel decision, issues awards, sets milestones. |
| **ROLE_FINANCE_OFFICER** | Research Finance Officer | Handles money — approves milestone evidence and releases funds. |
| **ROLE_COMPLIANCE_OFFICER** | Compliance Officer | The watchdog — reviews progress reports and deliverables. |
| **ROLE_ADMIN** | Research Admin (system owner) | Configures the platform and manages all user accounts. |

### Who can create whom (very important)
- **The public sign-up page only creates Researchers.** A member of the public can *only* register as a Principal Investigator — never a reviewer/finance/admin. (This is enforced on the server; any attempt to self-assign another role is ignored.)
- **Operational accounts are created internally** under **User Administration**:
  - The **Admin** can create **any** role — including **Finance Officers** and Grant Admins.
  - A **Grant Admin** can create **Researchers, Reviewers and Compliance Officers** — but **not** Finance Officers (only the Admin can).
  - When an account is created, a **temporary password** is generated and shared with the person (to be emailed once mail delivery is switched on).
- **Researchers** sign in from the normal **Sign in** page. **Staff & admins** sign in from the **"Staff & Admin Login"** button in the landing-page footer. Both use the same credentials system; the split just keeps the public and operational doors separate.

---

## Part B — The big picture (the pipeline)

Every grant follows this one storyline left-to-right:

```
   ADMIN / GRANT_ADMIN        RESEARCHER            GRANT_ADMIN + REVIEWERS         GRANT_ADMIN
 ┌────────────────────┐   ┌──────────────┐   ┌───────────────────────────┐   ┌──────────────┐
 │  1. FUNDING SETUP  │ → │ 2. APPLY     │ → │ 3. PEER REVIEW + PANEL    │ → │ 4. AWARD     │
 │  scheme + call     │   │ proposal +   │   │ assign → score → decide   │   │ grant letter │
 │  (open the call)   │   │ team+budget+ │   │                           │   │              │
 │                    │   │ abstract doc │   │                           │   │              │
 └────────────────────┘   └──────────────┘   └───────────────────────────┘   └──────────────┘
                                                                                     │
       FINANCE_OFFICER              RESEARCHER + COMPLIANCE        RESEARCHER         ▼
 ┌──────────────────────┐   ┌────────────────────────────┐   ┌──────────────┐
 │ 5. DISBURSEMENT      │ ← │ 6. PROGRESS TRACKING       │ ← │ ... ongoing  │
 │ milestones → release │   │ reports + deliverables     │   │              │
 └──────────────────────┘   └────────────────────────────┘   └──────────────┘
            │
            ▼
 ┌──────────────────────┐
 │ 7. RESEARCH OUTPUTS  │   (RESEARCHER records publications, patents, datasets)
 │ + IP records         │
 └──────────────────────┘

 Running underneath everything: Authentication & Roles · Notifications · Audit logging · Soft delete
```

**One sentence:** Money is announced (Funding) → a researcher applies (Application) → experts judge it (Review) → a winner is awarded (Award) → cash is released in stages (Disbursement) → the work is monitored (Progress) → and the results are recorded (Outputs).

---

## Part C — The scenario (our cast)

We'll follow one real-feeling example all the way through.

- **Riya Admin** — Research Admin (`ROLE_ADMIN`). Owns the platform.
- **Gopal** — Grant Administrator (`ROLE_GRANT_ADMIN`). Runs the "Frontier AI" programme.
- **Dr. Ada** — Principal Investigator / Researcher (`ROLE_RESEARCHER`). Wants funding for an AI-in-medicine project.
- **Dr. Ben** — co-investigator on Ada's team.
- **Dr. Rao** — Peer Reviewer (`ROLE_REVIEWER`).
- **Fiona** — Finance Officer (`ROLE_FINANCE_OFFICER`).
- **Carl** — Compliance Officer (`ROLE_COMPLIANCE_OFFICER`).

The project: **"Explainable AI for Early Cancer Detection."**

---

## Part D — Stage-by-stage, who does what

Each stage below tells you: **what happens · who acts · the screen they use · the status change · who gets notified.**

### Stage 0 — Onboarding (one-time setup)
**Who:** Riya (Admin), then Gopal (Grant Admin).

1. **Riya** signs in via **Staff & Admin Login**. In **User Administration → Create User**, she provisions the operational team: she creates **Gopal (Grant Admin)**, **Fiona (Finance Officer)**, **Carl (Compliance Officer)**, and **Dr. Rao (Reviewer)**. Each gets a temporary password.
   - *Note:* Fiona (Finance) **must** be created by Riya — Gopal isn't allowed to create finance staff.
2. **Dr. Ada** goes to the public site and clicks **Register** → she becomes a **Researcher** automatically. **Dr. Ben** registers the same way.

> Why this matters: this is the "separation of duties." The person who judges money requests (reviewer), the person who releases cash (finance), and the person who applies (researcher) are deliberately different people.

---

### Stage 1 — Funding setup (announce the money)
**Who:** Gopal (Grant Admin) — or Riya (Admin).
**Screen:** **Funding → Schemes**, then **Funding → Grant Calls**.

1. Gopal creates a **Funding Scheme** — e.g. *"Frontier AI Research Grant"* with a sponsor, research area (Artificial Intelligence), and award range (min/max amount). *Status: ACTIVE.*
2. Gopal creates a **Grant Call** under that scheme — e.g. *"2026 Frontier AI Call,"* with an **open date, close date**, expected number of awards, total budget, and a **review method** (Single-Blind / Double-Blind / Panel). *Status: UPCOMING.*
3. Gopal clicks **Open** on the call. *Status: UPCOMING → OPEN.*

> **Until a call is OPEN, nobody can apply.** This is the starting gun.

---

### Stage 2 — The Researcher applies (the heart of the story)
**Who:** Dr. Ada (Researcher).
**Screen:** **Open Calls** (Funding Opportunities) → **Application Wizard**.

1. Ada signs in (normal **Sign in**). On her sidebar she sees **Open Calls** — a gallery of currently open grant calls, each showing the scheme, research area, closing date and budget. She finds the *2026 Frontier AI Call* (it matches her project area) and clicks **Apply now**.
2. The **4-step Application Wizard** opens with the call pre-selected:
   - **Step 1 – Proposal:** project title, abstract text, discipline, requested amount, duration, lead institution. Saving here creates the application as a **DRAFT**.
   - **Step 2 – Team:** she adds **Dr. Ben** as a co-investigator (and any industrial partners), with their contribution.
   - **Step 3 – Budget:** she itemises the money by **budget head** (Personnel, Equipment, Travel, Consumables, Overhead, Subcontract) with justifications. A running total is shown against her requested amount.
   - **Step 4 – Review & Submit:** she **uploads her Abstract document** (PDF/DOC/DOCX, up to 10 MB — the system rejects wrong types/oversize files), checks the summary, and clicks **Submit Application**.
3. *Status: DRAFT → SUBMITTED* (submission date stamped). While it's a DRAFT she can keep editing or **Withdraw** it; once SUBMITTED she can still withdraw but not edit.

> What Ada controls: her proposal, team, budget, and abstract document. Everything she sees is her own applications (in **Applications**, grouped by status).

---

### Stage 3 — Peer review (judging it fairly)
**Who:** Gopal (Grant Admin) sets it up; Dr. Rao (Reviewer) does the judging.

**3a. Move into review & assign reviewers — Gopal**
**Screen:** **Applications** (admin view) and **Reviews → Assignment Panel**.
1. Gopal moves the application **SUBMITTED → UNDER_REVIEW**.
2. In the **Assignment Panel**, Gopal assigns **Dr. Rao** (and usually others) to the application, after a **conflict-of-interest** check. A reviewer can be assigned to an application only once. *Reviewer assignment status: ASSIGNED.*

**3b. Review the application — Dr. Rao**
**Screen:** **Reviews → Reviewer Queue** → **Scoring Card**.
1. Dr. Rao opens his queue and the assigned application. **For double-blind calls he does NOT see the applicant's name or institution** — only the proposal content. (This is the "blind review" protection, handled by the system.)
2. He clicks **Declare No Conflict**, then **Accept** the assignment. *Status: ASSIGNED → ACCEPTED.*
3. He scores the proposal **1–10 on each criterion**: Scientific Merit, Feasibility, Team Expertise, Impact, Innovation, Budget Justification — with comments and an overall recommendation (Fund at full / Fund at reduced / Do not fund).
4. He clicks **Submit Review**. *Status: ACCEPTED → SUBMITTED.*

**3c. Panel decision — Gopal**
**Screen:** **Reviews → Panel / Assignment Panel.**
1. Once reviews are in, Gopal records the **Panel Decision** for the application: a consensus score and an outcome — **Full Award / Reduced Award / Reserve List / Rejected** — plus any conditions and the awarded amount. (One panel decision per application.)
2. If favourable, Gopal moves the application **UNDER_REVIEW → AWARDED**.

> Who sees what here: Reviewers only see applications **assigned to them**, and never the applicant's identity on blind calls. Only Grant Admins assign reviewers and record the panel decision.

---

### Stage 4 — The award (turning a "yes" into a grant)
**Who:** Gopal (Grant Admin).
**Screen:** **Awards.**

1. Gopal **creates a Grant Award** from the favourable decision: awarded amount, start/end dates, and any conditions reference. *Award status: ACTIVE.* (Exactly one award per application.)
2. He clicks **Approve** — this stamps the **award-letter date** and confirms the award is live.
3. Over the project's life the award can be tracked and, if needed, moved between **ACTIVE ↔ SUSPENDED → COMPLETED / TERMINATED**.

> The **GrantAward is now the anchor** of everything that follows — disbursements, progress reports, deliverables, and research outputs all link back to this award.

---

### Stage 5 — Disbursement (releasing the money in stages)
**Who:** Gopal (Grant Admin) plans it; Dr. Ada submits evidence; Fiona (Finance) approves & pays.
**Screen:** **Disbursements → Milestone Scheduler.**

1. **Gopal** creates **Disbursement Milestones** on the award — each with a number, description, due date, amount, and whether evidence is required. *Milestone status: UPCOMING.* (Gopal/Admin can **Edit** a milestone while it's still UPCOMING.)
2. As the project hits a milestone, **Dr. Ada** clicks **Submit Evidence**. *Status: UPCOMING → EVIDENCE_SUBMITTED.*
3. **Fiona (Finance Officer)** reviews the claim and clicks **Approve**. *Status: EVIDENCE_SUBMITTED → APPROVED.*
   - *Note:* approving a milestone needs **Finance Officer** (or Grant Admin) — the **Admin role cannot** approve money; this is intentional.
4. **Fiona** then clicks **Release Funds**, entering the receiving account reference. This creates a **Fund Disbursement** (RELEASED) and moves the milestone to **DISBURSED**. **This action is written to the audit log** with who released it.

> Money discipline: cash only leaves against an **approved, evidenced milestone**, and only a **Finance Officer** can actually release it. Researchers can submit evidence but never approve or pay themselves.

---

### Stage 6 — Progress tracking (is the work actually happening?)
**Who:** Dr. Ada submits; Carl (Compliance) reviews.
**Screen:** **Progress → Overview / Reports / Deliverables.**

1. **Dr. Ada** submits a periodic **Progress Report** for the award: period (e.g. 2027-Q1), summary, key achievements, challenges, and **budget utilisation %**. *Status: DRAFT → SUBMITTED.*
2. **Carl (Compliance Officer)** reviews it and either **Approves** it or **Requests Revision**. *Status: SUBMITTED → APPROVED or REVISION_REQUESTED.*
3. **Dr. Ada** also registers **Deliverables** (Report, Dataset, Prototype, Publication, Training, Policy), then **uploads** the deliverable file/reference. *Status: PENDING → SUBMITTED.*
4. **Carl** reviews each deliverable and **Accepts** or **Rejects** it. *Status: SUBMITTED → ACCEPTED / REJECTED.*

**The Progress → Overview dashboard** (pick the award at the top) visualises all of this: a **budget-utilisation gauge**, **deliverable & report status donuts**, a **funds-released bar**, and an animated **milestone timeline** — so everyone can see project health at a glance.

> Accountability: the people doing the work (researchers) report; an independent **Compliance Officer** signs off. Compliance never edits the research — they review and approve/reject.

---

### Stage 7 — Research outputs & IP (recording what the money produced)
**Who:** Dr. Ada (Researcher).
**Screen:** **Outputs → Publications / IP.**

1. As results appear, **Dr. Ada** records **Research Outputs** — journal articles, conference papers, datasets, software, policy briefs — with authors, venue, **DOI**, published date, and an **Open Access compliant** flag. *Status: IN_PREPARATION → SUBMITTED → PUBLISHED.*
2. She records **IP Records** — patents, copyright, trademarks, trade secrets — with inventors, filing/grant dates and ownership %. *Status: FILED → GRANTED / ABANDONED.*

> This **closes the loop**: money went in (award → disbursement), work was monitored (progress), and now the results are captured against the same award — fully traceable.

---

### Stage 8 — Completion
When deliverables are accepted, funds are fully disbursed, and outputs recorded, the Grant Admin marks the **award COMPLETED**. The full history — every decision, payment, report and output — remains linked to the award and (for sensitive actions like awarding and releasing funds) is preserved in the **audit log**.

---

## Part E — Status lifecycles (cheat sheet)

```
Grant Call:        UPCOMING → OPEN → UNDER_REVIEW → AWARDED → CLOSED
Application:       DRAFT → SUBMITTED → UNDER_REVIEW → AWARDED | DECLINED
                   (DRAFT or SUBMITTED → WITHDRAWN)
Reviewer task:     ASSIGNED → ACCEPTED → SUBMITTED   (or DECLINED)
Panel decision:    FULL_AWARD | REDUCED_AWARD | RESERVE_LIST | REJECTED
Grant Award:       ACTIVE ↔ SUSPENDED → COMPLETED | TERMINATED
Milestone:         UPCOMING → EVIDENCE_SUBMITTED → APPROVED → DISBURSED   (or OVERDUE)
Fund disbursement: PENDING → RELEASED | FAILED
Progress report:   DRAFT → SUBMITTED → APPROVED | REVISION_REQUESTED
Deliverable:       PENDING → SUBMITTED → ACCEPTED | REJECTED
Research output:   IN_PREPARATION → SUBMITTED → PUBLISHED
IP record:         FILED → GRANTED | ABANDONED
```

---

## Part F — Role-by-role summary ("a day in the life")

**Researcher (Dr. Ada / Dr. Ben)** — *the applicant*
- Browses **Open Calls**, applies via the wizard, adds team & budget, **uploads the abstract document**, submits/withdraws.
- Submits **milestone evidence**, **progress reports**, **deliverable uploads**.
- Records **research outputs & IP**.
- Edits only their **own DRAFT** application data.

**Peer Reviewer (Dr. Rao)** — *the judge*
- Sees only applications **assigned to them** (PI identity hidden on blind calls).
- Declares no conflict, accepts, **scores 1–10 per criterion**, submits the review.

**Grant Administrator (Gopal)** — *the pipeline manager*
- Configures **schemes & calls** (open/close).
- Moves applications into review, **assigns reviewers**, records the **panel decision**, marks awarded.
- **Creates & approves awards**, plans **disbursement milestones** (and edits them).
- Can create Researchers/Reviewers/Compliance users (not Finance).

**Finance Officer (Fiona)** — *the money handler*
- **Approves** milestone evidence and **releases funds** (with bank reference). These are audit-logged.
- Cannot apply, review proposals, or create users.

**Compliance Officer (Carl)** — *the watchdog*
- **Reviews progress reports** (approve / request revision) and **deliverables** (accept / reject).
- Monitors project health on the Progress dashboards.

**Research Admin (Riya)** — *the configurator & account owner*
- **Creates all user accounts** — the only role that can create **Finance Officers** and other admins.
- Activates/deactivates users; oversees platform configuration.

---

## Part G — Why duties are split (the one big idea)

GrantTrack deliberately separates responsibilities so the same person can't both **request** money and **approve** it, or **judge** an application and **apply** for one:

- The **Researcher** asks for and uses the money — but never approves or pays it.
- The **Reviewer** judges fairly and blindly — but doesn't manage awards or money.
- The **Grant Admin** runs the process and issues awards — but doesn't release cash.
- The **Finance Officer** releases cash — but only against approved milestones, and is the only one (with Grant Admin) who can approve them; only the **Admin** can create finance staff.
- The **Compliance Officer** independently verifies the work was actually delivered.
- The **Admin** sets the rules and controls who exists in the system.

This is exactly the kind of control a real funding body needs: **fair, auditable, and accountable at every step.**

---

## Part H — Quick reference: screen → who uses it

| Screen / Sidebar item | Primary role(s) | What they do there |
|---|---|---|
| Landing page (`/`) | Everyone (public) | Learn about the platform; Sign in / Register / Staff login |
| Register | Public → Researcher | Create a Researcher account |
| Sign in / Staff & Admin Login | All | Researchers vs. operational staff entry points |
| Dashboard | All | Role-aware quick actions + lifecycle overview |
| Open Calls (Opportunities) | Researcher | Browse open calls and apply |
| Applications + Wizard | Researcher (Grant Admin views) | Create/submit applications, upload abstract |
| Reviews (Queue / Scoring) | Reviewer | Score assigned applications (blind) |
| Reviews (Assignment Panel) | Grant Admin | Assign reviewers, record panel decision |
| Funding (Schemes/Calls/Sponsors/Institutions) | Admin, Grant Admin | Configure funding & open/close calls |
| Awards | Grant Admin, Admin | Create/approve/track awards |
| Disbursements (Milestones/Releases) | Grant Admin (plan), Finance (approve/release), Researcher (evidence) | Stage payments |
| Progress (Overview/Reports/Deliverables) | Researcher (submit), Compliance (review) | Track and verify project work |
| Outputs (Publications/IP) | Researcher | Record results and IP |
| Notifications | All | In-app alerts |
| User Administration | Admin, Grant Admin | Create/manage user accounts (Finance = Admin only) |

---

*GrantTrack — Research Grant & Academic Output Management System. This document reflects the implemented Phase-1 behaviour of the platform.*
