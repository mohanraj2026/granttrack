# GrantTrack — Sample Test Data (End-to-End)

Copy-paste-ready sample data for testing the **entire** lifecycle through the UI, in order.
Every value here respects the backend validation rules and the business gates, and the amounts/dates
are chosen so the full chain works (e.g. award ≤ panel amount, milestone sum ≤ award, grant date ≥
filing date, call close date in the future).

> **IDs are assigned by the system.** Where a step needs an ID (e.g. "sponsorId"), just pick the
> record you created in the earlier step from the dropdown — you don't type the number.
> Dates assume "today" is around **mid-2026**; shift them if you're testing much later.

> **UI notes (redesigned front end):**
> - **Money is shown in ₹ (INR)** everywhere. You still type plain numbers into amount fields.
> - **Researchers** sign in at **`/login`** (or self-register at `/register`); **all staff roles**
>   (Admin, Grant Admin, Finance, Compliance, Reviewer) sign in at the **Staff Login** page (`/staff-login`).
> - Forms are grouped into labelled sections with a leading icon per field, inline validation, and a
>   👁 show/hide toggle on password fields. Section tabs (Funding, Reviews, Disbursements, Progress,
>   Outputs) are the underlined tabs near the top of each area.
> - The **dashboard adapts to your role** (its KPI cards and primary action differ per role).
> - The **notifications bell shows a red unread-count badge**; open the Notification Center to clear it.

Use this alongside `Frontend-Walkthrough-Guide.md` (same folder), which gives the click-by-click order.

---

## 1. Users (all six roles)

The **Admin** already exists (seeded). Create the rest in **User Admin** (as Admin), except the
Researcher, who can self-register at `/register`.

Password rule: **8–72 characters**. Role field accepts `ROLE_*` (or the short form, e.g. `REVIEWER`).

| Role | Name | Email | Password | Phone | Department |
|---|---|---|---|---|---|
| ROLE_ADMIN *(seeded)* | System Administrator | `admin@granttrack.local` | `Admin@12345` | — | — |
| ROLE_GRANT_ADMIN | Grace Grantadmin | `grantadmin@granttrack.local` | `Grant@12345` | `9010000001` | Research Office |
| ROLE_REVIEWER | Rob Reviewer | `reviewer@granttrack.local` | `Review@12345` | `9010000002` | Materials Science |
| ROLE_FINANCE_OFFICER | Fiona Finance | `finance@granttrack.local` | `Finance@123` | `9010000003` | Finance |
| ROLE_COMPLIANCE_OFFICER | Carl Compliance | `compliance@granttrack.local` | `Comply@123` | `9010000004` | Compliance |
| ROLE_RESEARCHER | Dr. Ada Researcher | `ada.researcher@granttrack.local` | `Research@123` | `9010000005` | Materials Science |

> Only the **Admin** may create the Finance Officer and Grant Admin. The Researcher is easiest to
> create via public **Register**. Pick the Institution (below) from the dropdown when registering.

> **Editing users:** the ⋯ menu on each row has **Edit** (name, email, phone, institution, department —
> role stays fixed). The **system administrator's own row shows Edit only** (no Deactivate/Delete) —
> that account is permanent.

---

## 2. Institution

Create under **Funding → Institutions** (or it can be created from the public register page).

| Field | Value |
|---|---|
| name | `Indian Institute of Science` |
| type | `University` |
| country | `India` |
| universityName | `Indian Institute of Science` |
| address | `CV Raman Road, Bengaluru` |
| city | `Bengaluru` |
| state | `Karnataka` |
| pincode | `560012` |
| mobileNumber | `9008000000` |
| email | `office@iisc.ac.in` |

*(Optional second institution for variety)*

| Field | Value |
|---|---|
| name | `Indian Institute of Technology Bombay` |
| type | `University` |
| country | `India` |
| universityName | `IIT Bombay` |
| address | `Powai, Mumbai` |
| city | `Mumbai` |
| state | `Maharashtra` |
| pincode | `400076` |
| mobileNumber | `9008000001` |
| email | `office@iitb.ac.in` |

---

## 3. Sponsor

Create under **Funding → Sponsors**.

| Field | Value |
|---|---|
| name | `Department of Science & Technology` |
| type | `Government` |
| contactEmail | `grants@dst.gov.in` |
| phone | `9011000000` |
| address | `Technology Bhavan, New Mehrauli Road, New Delhi` |
| website | `https://dst.gov.in` |

---

## 4. Funding Scheme

Create under **Funding → Schemes**. Pick the Sponsor above. Set **status = ACTIVE**.
Rule: `toDate ≥ fromDate`; `maxAwardAmount ≥ minAwardAmount` (both ≥ 0).

| Field | Value |
|---|---|
| schemeName | `National Renewable Energy Fellowship 2026` |
| sponsor | *Department of Science & Technology* |
| researchArea | `Renewable Energy` |
| category | `Fellowship` |
| minAwardAmount | `50000` |
| maxAwardAmount (**Award ceiling — "up to" dropdown**) | choose **Up to ₹5,00,000** |
| eligibleApplicants | `Faculty and post-doctoral researchers at recognised institutions` |
| fundingDurationMonths | `24` |
| fromDate (**labelled "Open from"**) | `2026-01-01` |
| toDate (**labelled "Open to"**) | `2027-12-31` |
| description | `Supports early-stage renewable-energy research with national impact.` |
| status | `ACTIVE` |
| Scheme document *(optional)* | choose any PDF/Word/image to test upload |

> The **Award ceiling** is a preset "Up to ₹X" dropdown (₹50k · ₹1L · ₹2.5L · ₹5L · ₹10L · ₹25L · ₹50L).
> Pick **Up to ₹5,00,000** so the researcher's ₹3,00,000 request (below) is within range.

---

## 5. Grant Call

Create under **Funding → Calls** (pick the scheme), then **Open** it.
Rules to Open: parent scheme must be **ACTIVE** and **closeDate must not be in the past**.
`reviewMethod` ∈ { `DOUBLE_BLIND`, `PANEL` }.

| Field | Value |
|---|---|
| scheme | *National Renewable Energy Fellowship 2026* |
| callTitle | `Renewable Energy Innovation Call 2026` |
| openDate | `2026-07-01` |
| closeDate | `2026-12-31` |
| expectedAwards | `4` |
| totalBudgetAllocated | `1000000` |
| reviewMethod | `DOUBLE_BLIND` |

Then click **Open** → call status becomes **OPEN**.

---

## 6. Grant Application (Researcher)

*Log in as Dr. Ada Researcher.* **Open Calls** → apply → the Application Wizard.
The principal investigator is set automatically to the logged-in researcher.
Rule: `requestedAmount > 0`.

| Field | Value |
|---|---|
| call | *Renewable Energy Innovation Call 2026* |
| projectTitle | `Low-Cost Perovskite Solar Cells for Rural Electrification` |
| researchAbstract | `This project develops low-cost, stable perovskite solar cells suitable for decentralised rural electrification, targeting a 30% cost reduction versus silicon modules while maintaining >18% efficiency.` |
| discipline | `Materials Science` |
| requestedAmount | `300000` |
| projectDurationMonths | `24` |
| institution | *Indian Institute of Science* |

**Co-investigators** (optional, add in the wizard):

| Name/User | Role | Contribution |
|---|---|---|
| *(another researcher, if created)* | `Co-Investigator` | `Device characterisation` |

**Budget lines** (optional, add in the wizard — should total ≈ requested amount):

| Budget head | Amount | Justification |
|---|---|---|
| `Equipment` | `150000` | `Spin coater and solar simulator` |
| `Consumables` | `90000` | `Precursor chemicals and substrates` |
| `Travel` | `30000` | `Conference dissemination` |
| `Personnel` | `30000` | `Part-time research assistant` |

Save as **DRAFT**, then open it under **Applications** and **Submit** → status **SUBMITTED**.

---

## 7. Move to Review + Reviewer Assignment (Grant Admin)

*Log in as Grace Grantadmin.*

1. **Applications** → open the submitted application → **Move to Review** → status **UNDER_REVIEW**
   (the call also moves to UNDER_REVIEW).
2. **Reviews → Assignments → Assign Reviewer**:

| Field | Value |
|---|---|
| application | *APP…- Low-Cost Perovskite Solar Cells…* |
| reviewer | *Rob Reviewer* |
| reviewDeadline | `2026-08-31` |

---

## 8. Review Scores (Reviewer)

*Log in as Rob Reviewer.* **Reviews → Queue** → open the assignment → **Accept**, then submit a score
for one or more criteria.
Rules: `score` is an **integer 1–10**; `criterion` and `overallRecommendation` are enums.

`criterion` ∈ { `SCIENTIFIC_MERIT`, `FEASIBILITY`, `TEAM_EXPERTISE`, `IMPACT`, `INNOVATION`, `BUDGET_JUSTIFICATION` }
`overallRecommendation` ∈ { `FUND_AT_FULL_AMOUNT`, `FUND_AT_REDUCED`, `DO_NOT_FUND` }

| criterion | score | comments | overallRecommendation |
|---|---|---|---|
| `SCIENTIFIC_MERIT` | `9` | `Strong scientific rationale and clear novelty.` | `FUND_AT_FULL_AMOUNT` |
| `FEASIBILITY` | `8` | `Methodology is sound; timeline is realistic.` | `FUND_AT_FULL_AMOUNT` |
| `IMPACT` | `9` | `High potential for rural electrification impact.` | `FUND_AT_FULL_AMOUNT` |
| `INNOVATION` | `8` | `Novel low-cost deposition approach.` | `FUND_AT_FULL_AMOUNT` |
| `TEAM_EXPERTISE` | `8` | `PI has relevant publications.` | `FUND_AT_FULL_AMOUNT` |
| `BUDGET_JUSTIFICATION` | `8` | `Budget is well justified.` | `FUND_AT_FULL_AMOUNT` |

Submit the review when done.

---

## 9. Panel Decision (Grant Admin)

*Log in as Grace Grantadmin.* **Reviews → Assignments → Record Panel Decision** (application must be
UNDER_REVIEW with ≥1 submitted review).
Rules: `consensusScore` 0–10; `awardDecision` enum; a favourable decision (FULL/REDUCED) is required
before an award can be created.

`awardDecision` ∈ { `FULL_AWARD`, `REDUCED_AWARD`, `RESERVE_LIST`, `REJECTED` }

When you select the application, the modal lists the **submitted reviews** (scores + recommendation)
so you can decide with the evidence visible. For an **award** decision you must also **assign a Finance
Officer** (the picker lists Finance-Officer users only).

| Field | Value |
|---|---|
| panelDate | `2026-08-15` |
| consensusScore | `8.5` |
| awardDecision | `FULL_AWARD` |
| awardedAmount | `300000` |
| financeOfficer *(required for an award)* | *Fiona Finance* |
| conditionsAttached | `Quarterly progress reporting required; funds released against milestones.` |

Result: application → **AWARDED**, call → **AWARDED**. The recorded decision now appears in the
**Panel Decisions** table (Grant Admin can **Edit** the details later; the outcome itself is final)
and on the **researcher's Application detail** page.

*(Reviewer decline — optional to test: as the Reviewer, on the review gate choose **Decline assignment**
and give a reason. It leaves your queue and the Grant Admin is notified; they can then assign another
reviewer.)*

*(Alternative to test the reduced path: `REDUCED_AWARD` with `awardedAmount = 250000` — then the award
below must be ≤ 250000.)*

---

## 10. Award (Grant Admin)

*Still as Grace Grantadmin.* **Awards → Create Award** (pick the AWARDED application; amount pre-fills
from the panel decision).
Rule: `awardedAmount > 0` **and ≤ the panel-approved amount** (300000 here). Created as **ACTIVE**.

| Field | Value |
|---|---|
| application | *APP… (AWARDED)* |
| awardedAmount | `300000` |
| startDate | `2026-09-01` |
| endDate | `2028-08-31` |
| conditionsRef | `GRANT-COND-2026-001` |
| awardLetterDate | `2026-08-20` |

*(Optional)* **Approve** to issue the award letter.

---

## 10b. Finance Officer accepts the award (Finance Officer)

*Log in as Fiona Finance* (the officer assigned on the panel decision). **Awards** → the assigned
award is listed with finance review **PENDING**. From the ⋯ menu:
- **View application** to see the proposal (and any documents), then
- **Accept** the award → finance review **ACCEPTED** (you can now create milestones), **or**
- **Reject** with a **reason** → finance review **REJECTED**; the Grant Admins are notified.

**Rule:** for an award with an assigned Finance Officer, **milestones cannot be created until it is
ACCEPTED** here.

---

## 11. Disbursement Milestones (Finance Officer)

*Still as Fiona Finance* (award must be finance-**ACCEPTED**). **Disbursements → Milestones** →
**New milestone** → add each. Rules: award must be **ACTIVE**; **sum of milestone amounts ≤ award
amount** (300000); `amount > 0`; `milestoneNumber` positive & unique per award.

| milestoneNumber | description | dueDate | amount | evidenceRequired |
|---|---|---|---|---|
| `1` | `Literature review and baseline material synthesis` | `2026-12-01` | `100000` | `true` |
| `2` | `Prototype cell fabrication and lab testing` | `2027-06-01` | `100000` | `true` |
| `3` | `Field pilot deployment and final report` | `2028-06-01` | `100000` | `true` |

(Sum = 300000 = award amount ✅)

---

## 12. Milestone Evidence → Review → Approve → Release

1. **Researcher (Dr. Ada):** *Disbursements → Milestones* → **Submit Evidence** on Milestone 1.
   The evidence dialog asks for a short note **and a supporting document** (the document is required
   when the milestone has *Evidence required = Yes*).
   → status **EVIDENCE_SUBMITTED**. (Only the owning PI may do this.)

| Field | Value |
|---|---|
| note | `Completed literature review and baseline film synthesis; report and lab logs attached.` |
| supporting document | choose a PDF/Word/Excel/image (e.g. the interim report) |

2. **Finance Officer (Fiona):** *Disbursements → Milestones* → **Review evidence** on Milestone 1:
   read the note, **download** the attached document, then **Approve** (or **Return for revision**
   with a comment, which sends it back to the researcher).
   → status **EVIDENCE_SUBMITTED → APPROVED**. *(audited)*

3. **Finance Officer (Fiona):** *Disbursements → Releases* → **Release Funds** for the approved
   Milestone 1. The release records the beneficiary account, the bank payment reference, and the date:

| Field | Value |
|---|---|
| milestone | *Milestone 1* |
| receivingAccountRef *(Beneficiary account)* | `HDFC0001234 / A/C 50100XXXXXX` |
| paymentReference *(UTR / NEFT)* | `UTR2026120100123` |
| releaseDate | `2026-12-05` |

→ Milestone **DISBURSED**; a fund-release record is created; researcher notified. *(audited)*

Repeat for Milestones 2 and 3 as you like.

---

## 13. Progress Report (Researcher → Compliance)

**Researcher (Dr. Ada):** *Progress → Reports* → create → **Submit**.
Rule: budget utilisation is a percentage **0–100**; award must be ACTIVE; only the owning PI.

| Field | Value |
|---|---|
| award | *Award for APP… (Perovskite Solar Cells)* |
| period | `2026-Q4` |
| summary | `Completed literature review and synthesised first perovskite film batches.` |
| keyAchievements | `Achieved 15% efficiency on initial cells; established stable synthesis protocol.` |
| challenges | `Humidity sensitivity during film deposition; mitigating with glovebox processing.` |
| budgetUtilisationPercent | `25` |
| Report document *(optional but recommended)* | choose a PDF/Word file to attach |

**Compliance (Carl):** *Progress → Reports* → **Review** the submitted report: read the summary,
**download the attached document**, then **Approve** (or **Request Revision** with a comment).
*If revision requested, the researcher edits and re-submits.*

---

## 14. Deliverable (Researcher → Compliance)

**Researcher (Dr. Ada):** *Progress → Deliverables* → create → **Upload**.
`type` ∈ { `REPORT`, `DATASET`, `PROTOTYPE`, `PUBLICATION`, `TRAINING`, `POLICY` }

| Field | Value |
|---|---|
| award | *Award for APP…* |
| title | `Interim Technical Report Q4 2026` |
| type | `REPORT` |
| dueDate | `2026-12-15` |

Then **Upload & Submit** — pick a real file from your computer (this both attaches it and submits
the deliverable for review):

| Field | Value |
|---|---|
| document | choose a file — PDF / Word / Excel / image / ZIP (≤ 10 MB) |

→ status **SUBMITTED**.

**Compliance (Carl):** *Progress → Deliverables* → **Review**: **download the deliverable document**,
then **Accept** (or **Reject** with a comment).
*If rejected, the researcher can re-upload and re-submit.*

---

## 15. Research Output (Researcher)

*Log in as Dr. Ada.* **Outputs → Publications** → add.
`type` ∈ { `JOURNAL_ARTICLE`, `CONFERENCE_PAPER`, `PATENT`, `DATASET`, `SOFTWARE`, `POLICY_BRIEF` }
`status` ∈ { `IN_PREPARATION`, `SUBMITTED`, `PUBLISHED` }

| Field | Value |
|---|---|
| award | *Award for APP…* |
| type | `JOURNAL_ARTICLE` |
| title | `Low-Cost Perovskite Solar Cells for Decentralised Rural Power` |
| authors | `A. Researcher, R. Kumar, S. Nair` |
| publicationVenue | `Journal of Renewable Energy Research` |
| doi | `10.1000/jrer.2027.0142` |
| publishedDate | `2027-03-15` |
| openAccessCompliant | `true` |
| status | `PUBLISHED` |

---

## 16. IP Record (Researcher)

*Still as Dr. Ada.* **Outputs → IP** → add.
`ipType` ∈ { `PATENT`, `COPYRIGHT`, `TRADEMARK`, `TRADE_SECRET` }
`status` ∈ { `FILED`, `GRANTED`, `ABANDONED` }
Rules: `ownershipPercent` 0–100; **grantDate ≥ filingDate**.

| Field | Value |
|---|---|
| award | *Award for APP…* |
| ipType | `PATENT` |
| title | `Method for Low-Cost Perovskite Layer Deposition` |
| inventors | `A. Researcher, R. Kumar` |
| filingDate | `2027-04-01` |
| grantDate | `2028-01-15` |
| ownershipPercent | `100` |
| status | `FILED` |

---

## 17. Completion (Grant Admin)

*Log in as Grace Grantadmin.* **Awards** → open the award → **Change status → COMPLETED**.

---

## 18. Notifications & Audit (optional checks)

- **Notifications** (any role): the **Notification Center** lists your own messages (you'll have
  several by now: reviewer assignment, award, disbursement, progress outcomes). Unread ones carry a
  left accent bar and a **New** badge; filter by status and category, then **Mark read** / **Dismiss**.
- **Admin push notification** (Admin / Grant Admin): **Notifications → Send notification** opens a
  form (there's no need to call the API directly):

| Field | Value |
|---|---|
| userId | *Dr. Ada's user id* |
| category | `APPLICATION` |
| message | `Welcome to GrantTrack — your grant workspace is ready.` |

`category` ∈ { `APPLICATION`, `REVIEW`, `AWARD`, `DISBURSEMENT`, `PROGRESS`, `OUTPUT` }

- **Audit log** (Compliance Officer / Admin, backend only): `GET /api/v1/audit-logs`
  Try filters: `?action=RELEASE_FUNDS`, `?entityType=GrantAward`, `?from=2026-08-01T00:00:00Z`.
  By the end of this run you should see rows for panel decision, award create/approve, milestone
  approve, fund release, progress/deliverable reviews, and user creation.

---

## Amount & date consistency cheat-sheet (why these numbers work)

| Constraint | Value used |
|---|---|
| Scheme award range | 50,000 – 500,000 |
| Panel awarded amount | 300,000 (favourable: FULL_AWARD) |
| Award amount ≤ panel amount | 300,000 ≤ 300,000 ✅ |
| Milestone sum ≤ award | 100,000 × 3 = 300,000 ≤ 300,000 ✅ |
| Call close date in future | 2026-12-31 ✅ |
| Scheme toDate ≥ fromDate | 2027-12-31 ≥ 2026-01-01 ✅ |
| IP grantDate ≥ filingDate | 2028-01-15 ≥ 2027-04-01 ✅ |
| Review score range | 1–10 (used 8–9) ✅ |
| Budget utilisation 0–100 | 25 ✅ |
| Award decision needs a Finance Officer | Fiona Finance assigned ✅ |
| Milestones need finance review ACCEPTED | accept in step 10b first ✅ |

If you change the panel amount, keep **award ≤ panel** and **milestone sum ≤ award**, or the backend
will (correctly) reject the step.
