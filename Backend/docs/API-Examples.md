# GrantTrack — API Examples to study

All endpoints are under `/api/v1`. Every response uses the standard envelope:
```json
{ "success": true, "message": "...", "data": { }, "timestamp": "2026-06-09T10:20:00Z" }
```
Authenticated calls require `Authorization: Bearer <accessToken>`.
Full, interactive documentation (with schemas) is at `/swagger-ui.html`.

cd Backend
mvn spring-boot:run          # needs MySQL 8 on localhost:3306
# Swagger: http://localhost:8082/swagger-ui.html
# Login:   admin@granttrack.local / Admin@12345

---

## 1. Authentication — `/auth`

### Register
`POST /api/v1/auth/register`
```json
{
  "name": "Dr. Ada Researcher",
  "email": "ada@uni.edu",
  "password": "Secret@123",
  "phone": "+44 20 7946 0000",
  "institutionId": 1,
  "department": "Computer Science",
  "roles": ["ROLE_RESEARCHER"]
}
```
**201**
```json
{ "success": true, "message": "Registration successful",
  "data": { "id": 5, "name": "Dr. Ada Researcher", "email": "ada@uni.edu",
            "status": "ACTIVE", "roles": ["ROLE_RESEARCHER"] } }
```

### Login
`POST /api/v1/auth/login`
```json
{ "email": "ada@uni.edu", "password": "Secret@123" }
```
**200**
```json
{ "success": true, "message": "Login successful",
  "data": { "accessToken": "eyJhbGci...", "refreshToken": "0c1f...e9",
            "tokenType": "Bearer", "expiresInMs": 900000,
            "user": { "id": 5, "email": "ada@uni.edu", "roles": ["ROLE_RESEARCHER"] } } }
```

### Refresh / Logout / Change password
```
POST /api/v1/auth/refresh           { "refreshToken": "0c1f...e9" }
POST /api/v1/auth/logout            { "refreshToken": "0c1f...e9" }     (Bearer)
POST /api/v1/auth/change-password   { "currentPassword": "...", "newPassword": "..." }  (Bearer)
POST /api/v1/auth/forgot-password   { "email": "ada@uni.edu" }
```

---

## 2. Funding — `/funding`

### Create scheme  *(ROLE_GRANT_ADMIN / ROLE_ADMIN)*
`POST /api/v1/funding/schemes`
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

### Create / open / close a grant call
```
POST /api/v1/funding/calls
POST /api/v1/funding/calls/{id}/open
POST /api/v1/funding/calls/{id}/close
GET  /api/v1/funding/calls?status=OPEN&schemeId=1&page=0&size=20&sort=openDate,desc
```
Create call body:
```json
{
  "schemeId": 1,
  "callTitle": "2026 Frontier AI Call",
  "openDate": "2026-07-01",
  "closeDate": "2026-09-30",
  "expectedAwards": 10,
  "totalBudgetAllocated": 4000000.00,
  "reviewMethod": "DOUBLE_BLIND"
}
```

---

## 3. Grant Application — `/applications`  *(ROLE_RESEARCHER)*

```
POST /api/v1/applications                      create (DRAFT)
PUT  /api/v1/applications/{id}                 update draft
POST /api/v1/applications/{id}/submit          DRAFT -> SUBMITTED
POST /api/v1/applications/{id}/withdraw        -> WITHDRAWN
PATCH /api/v1/applications/{id}/status?status=UNDER_REVIEW   (GRANT_ADMIN)
POST /api/v1/applications/{id}/co-investigators
POST /api/v1/applications/{id}/budgets
GET  /api/v1/applications?status=SUBMITTED&callId=1&q=genomics
```
Create body:
```json
{
  "callId": 1,
  "projectTitle": "Explainable Models for Genomic Medicine",
  "researchAbstract": "We propose ...",
  "discipline": "Bioinformatics",
  "requestedAmount": 320000.00,
  "projectDurationMonths": 24,
  "institutionId": 1
}
```
Budget line:
```json
{ "budgetHead": "PERSONNEL", "amount": 180000.00, "justification": "2 postdocs" }
```

---

## 4. Review — `/reviews`

```
POST /api/v1/reviews/assignments                          assign reviewer (GRANT_ADMIN)
POST /api/v1/reviews/assignments/{id}/conflict-check?status=CLEAR
POST /api/v1/reviews/assignments/{id}/respond?decision=ACCEPT             (REVIEWER)
POST /api/v1/reviews/assignments/{id}/scores                              (REVIEWER)
POST /api/v1/reviews/assignments/{id}/submit                             (REVIEWER)
POST /api/v1/reviews/applications/{appId}/panel-decision                 (GRANT_ADMIN)
```
Assignment body:
```json
{ "applicationId": 12, "reviewerId": 7, "reviewDeadline": "2026-10-15" }
```
Score body (one per criterion):
```json
{ "criterion": "SCIENTIFIC_MERIT", "score": 8, "comments": "Strong methodology",
  "overallRecommendation": "FUND_AT_FULL_AMOUNT" }
```
Panel decision body:
```json
{ "panelDate": "2026-10-20", "consensusScore": 8.40, "awardDecision": "FULL_AWARD",
  "awardedAmount": 320000.00, "conditionsAttached": "Annual progress reporting" }
```
> **Blind review:** reviewer-facing responses never expose principal-investigator identity.

---

## 5. Award — `/awards`  *(ROLE_GRANT_ADMIN)*
```
POST  /api/v1/awards                          { "applicationId": 12, "awardedAmount": 320000.00,
                                                "startDate": "2026-11-01", "endDate": "2028-10-31" }
POST  /api/v1/awards/{id}/approve             sets award-letter date, activates
PATCH /api/v1/awards/{id}/status?status=SUSPENDED
GET   /api/v1/awards?status=ACTIVE
```

---

## 6. Disbursement — `/disbursements`
```
POST /api/v1/disbursements/milestones                        create (GRANT_ADMIN)
POST /api/v1/disbursements/milestones/{id}/submit-evidence   (RESEARCHER)
POST /api/v1/disbursements/milestones/{id}/approve           (FINANCE_OFFICER)
POST /api/v1/disbursements/milestones/{id}/release           release funds (FINANCE_OFFICER)
GET  /api/v1/disbursements?awardId=3
```
Milestone body:
```json
{ "awardId": 3, "milestoneNumber": 1, "description": "Kick-off & ethics approval",
  "dueDate": "2027-01-31", "amount": 80000.00, "evidenceRequired": true }
```

---

## 7. Progress — `/progress`
```
POST /api/v1/progress/reports                       create (RESEARCHER)
POST /api/v1/progress/reports/{id}/submit
POST /api/v1/progress/reports/{id}/review?decision=APPROVE         (COMPLIANCE_OFFICER)
POST /api/v1/progress/deliverables
POST /api/v1/progress/deliverables/{id}/upload      { "filePath": "s3://bucket/key.pdf" }
POST /api/v1/progress/deliverables/{id}/review?decision=ACCEPT     (COMPLIANCE_OFFICER)
```
Report body:
```json
{ "awardId": 3, "period": "2027-Q1", "summary": "...", "keyAchievements": "...",
  "challenges": "...", "budgetUtilisationPercent": 22.50 }
```

---

## 8. Research Output — `/outputs`  *(ROLE_RESEARCHER)*
```
POST /api/v1/outputs            research output (publication/dataset/...)
POST /api/v1/outputs/ip         IP record / patent
GET  /api/v1/outputs?awardId=3&type=JOURNAL_ARTICLE
GET  /api/v1/outputs/ip?awardId=3
```
Output body:
```json
{ "awardId": 3, "type": "JOURNAL_ARTICLE", "title": "Explainable Genomic Models",
  "authors": "Researcher A, Researcher B", "publicationVenue": "Nature Methods",
  "doi": "10.1000/xyz123", "publishedDate": "2027-06-01", "openAccessCompliant": true,
  "status": "PUBLISHED" }
```

---

## 9. Notifications — `/notifications`
```
GET   /api/v1/notifications?status=UNREAD
GET   /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{id}/read
PATCH /api/v1/notifications/{id}/dismiss
POST  /api/v1/notifications        (GRANT_ADMIN) { "userId": 5, "message": "...", "category": "AWARD" }
```

---

## Error responses
Validation (**400**):
```json
{ "success": false, "message": "Validation failed",
  "data": { "status": 400, "error": "Bad Request", "path": "/api/v1/auth/register",
            "fieldErrors": [ { "field": "email", "message": "must be a well-formed email address",
                              "rejectedValue": "bad" } ] } }
```
Business rule (**409**):
```json
{ "success": false, "message": "Only an UPCOMING call can be opened (current: OPEN)",
  "data": { "status": 409, "error": "Conflict", "path": "/api/v1/funding/calls/1/open" } }
```
Not found (**404**), unauthorized (**401**), forbidden (**403**) follow the same envelope.
