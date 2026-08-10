# GrantTrack — Progress-Report → Notification Flow (Interview Walkthrough)

A single, concrete end-to-end trace to show an evaluator **exactly** how the Notification module
works in a real scenario:

> A **Researcher** submits a progress report for a milestone → the **Compliance Officer** is notified
> automatically → the Compliance Officer approves it with a comment → the **Researcher** receives
> *"Your progress report for 2024-Q4 has been approved. Reviewer comment: Progress reviewed
> successfully."*

Every step below names the **file**, the **code**, where the **JWT token is attached**, how the
request reaches the service, where the **notification logic** lives, how the frontend **gets the
response**, and how **exceptions** are handled.

**Services involved:** `core-service` (owns progress reports + fires the notifications),
`notification-service` (stores/serves notifications), `api-gateway`, and the Angular frontend.

---

## The cast + the one-line flow

- **Researcher (PI)** — submits the progress report from the milestone.
- **Compliance Officer** — reviews it (approve / request revision).
- **notification-service** — persists a `notifications` row per recipient.

```
Milestone → "Submit Progress Report" → report form → SUBMIT
   → core notifies Compliance (Feign → notification-service)
   → Compliance reviews (APPROVE + comment)
   → core notifies Researcher + Finance Officer (Feign → notification-service)
   → recipients' bell badge + Notification Center show the message
```

---

# PART A — Researcher submits the progress report

## Step 1 — Click "Submit Progress Report" on the milestone
**File:** [`Frontend/.../disbursements/milestones/milestone-scheduler.component.ts`](Frontend/src/app/features/disbursements/milestones/milestone-scheduler.component.ts)

The milestone row's button calls:
```ts
submitProgressReport(m: MilestoneResponse): void {
  if (this.isLocked(m)) { this.toast.error('Complete the earlier milestone first …'); return; }
  this.router.navigate(['/progress/reports'], {
    queryParams: { awardId: m.awardId, milestoneId: m.id, new: 1 },   // deep-link, pre-linked to this milestone
  });
}
```
No HTTP yet — it just navigates to the Progress → Reports page carrying the milestone link.

## Step 2 — The report form opens, pre-linked to the milestone
**File:** [`Frontend/.../progress/reports/progress-reports.component.ts`](Frontend/src/app/features/progress/reports/progress-reports.component.ts)
```ts
ngOnInit(): void {
  ...
  this.route.queryParamMap.subscribe((params) => {
    const awardId = params.get('awardId');
    const milestoneId = params.get('milestoneId');
    if (awardId) this.onAwardFilter(Number(awardId));
    if (params.get('new') && awardId) {
      this.openCreate();
      this.pendingMilestoneId.set(milestoneId ? Number(milestoneId) : null);  // remember the milestone link
      this.form.patchValue({ awardId: Number(awardId) });
    }
  });
}
```

## Step 3 — Researcher fills the form and clicks Save (creates a DRAFT report)
**File:** same component — `save()`:
```ts
const body = {
  awardId: v.awardId!,
  milestoneId: id ? undefined : (this.pendingMilestoneId() ?? undefined),   // attach the milestone on create
  period: v.period || undefined,          // e.g. "2024-Q4"
  summary: ..., keyAchievements: ..., challenges: ..., budgetUtilisationPercent: ...,
};
const request$ = id ? this.api.updateReport(id, body) : this.api.createReport(body);
```
**Service:** [`Frontend/.../progress/progress.service.ts`](Frontend/src/app/features/progress/progress.service.ts)
```ts
private base = `${environment.apiUrl}/progress`;    // http://localhost:8080/api/v1/progress
createReport(body) { return this.http.post(`${this.base}/reports`, body); }   // POST /api/v1/progress/reports
```

### 🔑 Where the token is attached (happens on THIS and every later call)
**File:** [`Frontend/.../core/interceptors/auth.interceptor.ts`](Frontend/src/app/core/interceptors/auth.interceptor.ts) — a global HTTP interceptor registered in
[`app.config.ts`](Frontend/src/app/app.config.ts).
```ts
const token = storage.accessToken;                                  // from localStorage (TokenStorageService)
const authReq = token && !isBypass
  ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })  // ← JWT attached automatically
  : req;
return next(authReq)...
```
So `createReport` leaves the browser as `POST /api/v1/progress/reports` with header
`Authorization: Bearer <jwt>`. **You never add the header manually** — the interceptor does it for
every request except the public auth endpoints.

### Where the request goes
```
Browser → API Gateway (:8080) → core-service (:8084)
```
Gateway routing — [`api-gateway/.../application.yml`](Backend/api-gateway/src/main/resources/application.yml): `/api/v1/progress/**` falls under the catch-all
`core-service` route (`lb://core-service`, resolved via Eureka).

### Backend — create the DRAFT
**File:** [`core-service/.../progress/controller/ProgressReportController.java`](Backend/core-service/src/main/java/com/granttrack/progress/controller/ProgressReportController.java)
```java
@PostMapping
@PreAuthorize("hasRole('RESEARCHER')")                      // only a researcher can create
public ResponseEntity<ApiResponse<ProgressReportResponse>> create(@Valid @RequestBody ProgressReportRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Progress report created", reportService.create(request)));
}
```
- Before the controller runs, core-service's **`JwtClaimsAuthenticationFilter`** (from `common-lib`)
  verifies the JWT and builds the security context from its claims (no DB call).
**File:** [`core-service/.../progress/service/impl/ProgressReportServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/progress/service/impl/ProgressReportServiceImpl.java) — `create()` saves a
`ProgressReport` with `status = DRAFT`, `milestoneId` set, `period = "2024-Q4"`. **No notification yet.**

## Step 4 — Researcher clicks Submit (this is what triggers the Compliance notification)
**Frontend:** `progress-reports.component.ts` → `submit(r)` → `progress.service.ts`:
```ts
submitReport(id) { return this.http.post(`${this.base}/reports/${id}/submit`, null); }   // POST /reports/{id}/submit
```
(Token attached again by the interceptor.)

**Backend controller:**
```java
@PostMapping("/{id}/submit")
@PreAuthorize("hasRole('RESEARCHER')")
public ResponseEntity<...> submit(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success("Progress report submitted", reportService.submit(id)));
}
```
**Backend service — `submit()` (this is where the notification logic starts):**
```java
public ProgressReportResponse submit(Long id) {
    ProgressReport report = find(id);
    assertOwningPrincipalInvestigator(awardOf(report.getAwardId()));       // only the owning PI may submit
    if (report.getStatus() != DRAFT && report.getStatus() != REVISION_REQUESTED)
        throw new BusinessException("Only a DRAFT or REVISION_REQUESTED report can be submitted …");
    report.setStatus(ProgressStatus.SUBMITTED);
    report.setSubmittedDate(Instant.now());
    report.setSubmittedById(SecurityUtils.getCurrentUserId().orElse(null));
    ProgressReport saved = reportRepository.save(report);
    // 👉 THE TRIGGER: notify every Compliance Officer that a report is waiting.
    notifyComplianceOfficers("A progress report"
        + (report.getPeriod() != null ? " for " + report.getPeriod() : "")
        + " has been submitted and is awaiting your review.");
    return mapper.toResponse(saved);
}
```

---

# PART B — The Compliance Officer is notified automatically

## Step 5 — Who to notify + firing the notification
**File:** `ProgressReportServiceImpl.java` — the helper:
```java
private void notifyComplianceOfficers(String message) {
    try {
        Specification<User> spec = (root, cq, cb) -> {
            cq.distinct(true);
            return cb.and(
                cb.equal(root.get("status"), UserStatus.ACTIVE),
                root.join("roles").get("name").in(RoleName.ROLE_COMPLIANCE_OFFICER.name()));  // all active compliance officers
        };
        for (User officer : userRepository.findAll(spec)) {
            notificationService.notify(officer.getId(), message, NotificationCategory.PROGRESS);  // one per officer
        }
    } catch (Exception e) {
        log.warn("Failed to notify compliance officers of a submitted progress report", e);       // best-effort
    }
}
```
- It queries the (read-only) `users` table for **ACTIVE users with role `ROLE_COMPLIANCE_OFFICER`**,
  and calls `notificationService.notify(...)` once per officer — so a role-wide notification becomes
  one `notifications` row per officer.

## Step 6 — Cross-service hop (core → notification-service via Feign)
`notificationService` here is core's **Feign-backed** implementation (not a DB write — core doesn't
own the notifications table).

**File:** [`core-service/.../notification/service/impl/NotificationServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
```java
public void notify(Long userId, String message, NotificationCategory category) {
    try {
        notificationClient.publish(internalToken,                                   // service-to-service token
            new NotificationPublishRequest(userId, message, category.name()));
    } catch (Exception ex) {
        log.warn("Failed to publish notification to user {} …", userId, ex);         // best-effort, swallowed
    }
}
```
**Feign client:** [`NotificationClient.java`](Backend/core-service/src/main/java/com/granttrack/notification/client/NotificationClient.java) — `@FeignClient(name = "notification-service")`,
`POST /internal/notifications` (resolved via Eureka, load-balanced).

**Receiver (notification-service):** [`InternalNotificationController.java`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/InternalNotificationController.java)
```java
@PostMapping("/internal/notifications")
public ResponseEntity<Void> publish(@RequestHeader("X-Internal-Token") String token,
                                    @Valid @RequestBody NotificationRequest request) {
    if (!internalToken.equals(token)) return ResponseEntity.status(FORBIDDEN).build();  // only trusted services
    notificationService.create(request);
    return ResponseEntity.status(CREATED).build();
}
```
**The insert:** [`notification-service/.../service/impl/NotificationServiceImpl.java`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
```java
Notification n = Notification.builder()
    .userId(userId)                          // ← the Compliance Officer
    .message(message).category(category)     // category = PROGRESS
    .status(NotificationStatus.UNREAD)
    .build();
notificationRepository.save(n);              // one UNREAD row for that officer
```

> **Note on `/internal`:** this endpoint is authenticated by a **shared internal token**, not a user
> JWT, and is **not** exposed by the gateway — it's only reachable service-to-service.

## Step 7 — How the Compliance Officer's frontend gets the notification
The officer's browser doesn't get a push — it **polls** and **lists**:
- **Bell badge** — [`notifications.service.ts`](Frontend/src/app/features/notifications/notifications.service.ts) `refreshUnread()` → `GET /api/v1/notifications/unread-count`,
  driven by [`main-layout.component.ts`](Frontend/src/app/layout/main-layout/main-layout.component.ts) on navigation + a 60-second `setInterval`. The count feeds the
  bell badge signal (`unread()`), which shows the red pill.
- **Notification Center** — [`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts) `list()` → `GET /api/v1/notifications?status=UNREAD`.
  The server scopes it to the **current user id** (the officer), so they see *"A progress report for
  2024-Q4 has been submitted and is awaiting your review."* They can filter by the **Progress** category chip.

---

# PART C — The Compliance Officer reviews the report

## Step 8 — Approve (with a comment)
**Frontend:** [`progress-reports.component.ts`](Frontend/src/app/features/progress/reports/progress-reports.component.ts) — the compliance officer opens the submitted report and clicks Approve:
```ts
review(r: ProgressReportResponse, decision: 'APPROVE' | 'REQUEST_REVISION'): void {
  const comment = this.reviewForm.getRawValue().comment;                      // "Progress reviewed successfully"
  if (decision === 'REQUEST_REVISION' && !comment.trim()) { this.toast.error('Please add a comment …'); return; }
  this.api.reviewReport(r.id, decision, comment).subscribe(() => {
    this.toast.success(decision === 'APPROVE' ? 'Report approved.' : 'Revision requested.');
    this.reviewModalOpen.set(false); this.load();
  });
}
```
**Service:** [`progress.service.ts`](Frontend/src/app/features/progress/progress.service.ts)
```ts
reviewReport(id, decision, comment) {
  return this.http.post(`${this.base}/reports/${id}/review`, null,
    { params: toHttpParams({ decision, comment }) });   // POST /reports/{id}/review?decision=APPROVE&comment=...
}
```
(Token attached by the interceptor, as always.)

**Backend controller:**
```java
@PostMapping("/{id}/review")
@PreAuthorize("hasRole('COMPLIANCE_OFFICER')")             // only a compliance officer may review
public ResponseEntity<...> review(@PathVariable Long id,
        @RequestParam String decision, @RequestParam(required = false) String comment) {
    return ResponseEntity.ok(ApiResponse.success("Progress report reviewed",
        reportService.review(id, decision, comment)));
}
```

## Step 9 — The review logic + building the EXACT message
**File:** `ProgressReportServiceImpl.java` — `review()`:
```java
@Auditable(action = "REVIEW_PROGRESS", entityType = "ProgressReport")
public ProgressReportResponse review(Long id, String decision, String comment) {
    ProgressReport report = find(id);
    if (report.getStatus() != SUBMITTED)
        throw new BusinessException("Only a SUBMITTED report can be reviewed …");
    String outcome;
    switch (decision.toUpperCase()) {
        case "APPROVE"          -> { report.setStatus(APPROVED);           outcome = "approved"; }
        case "REQUEST_REVISION" -> { report.setStatus(REVISION_REQUESTED); outcome = "sent back for revision"; }
        default -> throw new BusinessException("Invalid decision: " + decision);
    }
    report.setReviewComment(comment);
    ProgressReport saved = reportRepository.save(report);

    String periodText  = report.getPeriod() != null ? " for " + report.getPeriod() : "";   // " for 2024-Q4"
    String commentText = StringUtils.hasText(comment) ? " Reviewer comment: " + comment : ""; // " Reviewer comment: Progress reviewed successfully"

    // 👉 notify the RESEARCHER (owning PI)
    notifyOwningPrincipalInvestigator(report.getAwardId(),
        "Your progress report" + periodText + " has been " + outcome + "." + commentText);
    // 👉 also notify the assigned FINANCE OFFICER (so they can verify the milestone)
    notifyAssignedFinanceOfficer(report.getAwardId(),
        "The Compliance Officer has " + outcome + " a progress report" + periodText
            + ". Please review the outcome and verify the milestone." + commentText);
    return mapper.toResponse(saved);
}
```
**This is where your exact string is built.** With `period = "2024-Q4"`, `decision = APPROVE`
(→ `outcome = "approved"`), `comment = "Progress reviewed successfully"`:
```
"Your progress report" + " for 2024-Q4" + " has been " + "approved" + "." + " Reviewer comment: Progress reviewed successfully"
= "Your progress report for 2024-Q4 has been approved. Reviewer comment: Progress reviewed successfully"
```

---

# PART D — The Researcher receives the "approved" notification

## Step 10 — Firing the researcher notification (same Feign path)
**File:** `ProgressReportServiceImpl.java`:
```java
private void notifyOwningPrincipalInvestigator(Long awardId, String message) {
    try {
        awardRepository.findById(awardId).ifPresent(award ->
            applicationRepository.findById(award.getApplicationId()).ifPresent(app ->
                notificationService.notify(app.getPrincipalInvestigatorId(), message, NotificationCategory.PROGRESS)));
    } catch (Exception e) { log.warn("Failed to send progress report notification", e); }  // best-effort
}
```
It resolves the PI id (award → application → `principalInvestigatorId`) and fires
`notificationService.notify(piId, "Your progress report for 2024-Q4 has been approved. …", PROGRESS)`
→ core Feign impl → `NotificationClient.publish` → notification-service `/internal/notifications`
→ inserts an **UNREAD** `notifications` row with `user_id = <the researcher>`, `category = PROGRESS`.

## Step 11 — How the Researcher's frontend shows it
Exactly like Part B, step 7, but scoped to the researcher's id:
- Their **bell badge** increments on the next `refreshUnread()` (navigation or the 60s poll).
- Their **Notification Center** (`GET /api/v1/notifications?status=UNREAD`) lists
  *"Your progress report for 2024-Q4 has been approved. Reviewer comment: Progress reviewed
  successfully"* under the **Progress** category. **Mark read** / **Dismiss** work per §9 of the
  Notification guide.

---

# PART E — Exception handling across this whole flow

One consistent contract: services throw typed exceptions → one `@RestControllerAdvice` maps them to
an HTTP status + the standard `ApiResponse` envelope → the frontend's one error interceptor toasts
the message. **Crucially, the notification sends themselves are best-effort and never break the
business action.**

| Where | What can go wrong | Handled how | Result |
|---|---|---|---|
| Submit (Step 4) | Not the owning PI | `assertOwningPrincipalInvestigator` → `AccessDeniedException` | **403** toast |
| Submit | Report not DRAFT/REVISION_REQUESTED | `BusinessException` | **409** toast |
| Create (Step 3) | Blank summary / null awardId | `@Valid` bean validation → `MethodArgumentNotValidException` | **400** toast (field errors) |
| Review (Step 8) | Non-compliance role | `@PreAuthorize('COMPLIANCE_OFFICER')` | **403** |
| Review | Report not SUBMITTED, or bad decision | `BusinessException` | **409** toast |
| Any call | Expired/invalid JWT | security filter → `RestAuthenticationEntryPoint` | **401** |
| **Notify (Steps 5/6/10)** | notification-service down / any error | `try/catch` in `notifyComplianceOfficers` / `notifyOwningPrincipalInvestigator` **and** in core's Feign `notify()` | **swallowed + logged** — the submit/review still succeeds |

### Backend translation — [`common-lib/.../handler/GlobalExceptionHandler.java`](Backend/common-lib/src/main/java/com/granttrack/common/handler/GlobalExceptionHandler.java)
```java
@ExceptionHandler(ResourceNotFoundException.class)   // 404
@ExceptionHandler({BusinessException.class, ...})     // 409
@ExceptionHandler(MethodArgumentNotValidException.class)  // 400 + field errors
@ExceptionHandler(AccessDeniedException.class)        // 403
```

### The "best-effort" guarantee (say this — it's a strong point)
Notifications are wrapped in `try/catch` at **two** layers — inside each `notify*` helper in
`ProgressReportServiceImpl`, and again inside core's Feign-backed `NotificationServiceImpl.notify()`.
So if notification-service is unavailable when the researcher submits, **the report still becomes
SUBMITTED** and the review still completes; only the notification is lost (and logged). A messaging
outage never rolls back a real business transaction.

### Frontend — [`error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts)
Turns any backend error into a toast (`body.message`, or "Cannot reach the server…" on a network
error). The Notification Center's own `subscribe` blocks only reset the spinner; the message is shown
centrally. The **bell poll** (`refreshUnread`) deliberately swallows errors so a background failure
never spams a toast.

---

## Sequence at a glance

```
RESEARCHER                       FRONTEND                 GATEWAY        CORE-SERVICE                       NOTIFICATION-SERVICE
   |  click "Submit Progress Report" (milestone)                                                                   |
   |------------------------------------> navigate /progress/reports?awardId&milestoneId&new                       |
   |  fill form → Save                                                                                             |
   |     createReport(body)  --[Bearer JWT via authInterceptor]--> :8080 -> POST /api/v1/progress/reports -> create() DRAFT
   |  click Submit                                                                                                 |
   |     submitReport(id)    --[Bearer JWT]--> :8080 -> POST /reports/{id}/submit -> submit() SUBMITTED            |
   |                                                                 └─ notifyComplianceOfficers()                 |
   |                                                                     core NotificationServiceImpl.notify()     |
   |                                                                     --Feign+X-Internal-Token--> POST /internal/notifications
   |                                                                                                   └─ save row (userId=officer, UNREAD, PROGRESS)
COMPLIANCE OFFICER                                                                                                 |
   |  bell poll GET /unread-count  → badge++     ;  Notification Center GET /notifications?status=UNREAD → sees it |
   |  Approve + comment "Progress reviewed successfully"                                                           |
   |     reviewReport(id,'APPROVE',comment) --[Bearer JWT]--> :8080 -> POST /reports/{id}/review -> review() APPROVED
   |                                                                 ├─ notifyOwningPrincipalInvestigator(PI)  -> Feign -> save row (userId=PI)
   |                                                                 └─ notifyAssignedFinanceOfficer(FO)      -> Feign -> save row (userId=FO)
RESEARCHER                                                                                                         |
   |  bell poll → badge++  ;  Notification Center → "Your progress report for 2024-Q4 has been approved. Reviewer comment: Progress reviewed successfully"
```

---

## Talking points / likely questions

- **"Where does the token get attached?"** In the Angular `authInterceptor` — it clones every
  outgoing request and adds `Authorization: Bearer <jwt>` (except public auth endpoints). Nothing in
  the progress or notification services adds it manually.
- **"Where does the request physically go?"** Frontend → API gateway (`:8080`) → `core-service`
  (progress endpoints are under the catch-all route), resolved via Eureka.
- **"Where's the logic that notifies the compliance officer?"** `ProgressReportServiceImpl.submit()`
  → `notifyComplianceOfficers()` (queries active `ROLE_COMPLIANCE_OFFICER` users, one notification each).
- **"How does it reach notification-service?"** core's Feign-backed `NotificationServiceImpl.notify()`
  → `NotificationClient` → `POST /internal/notifications` (internal-token authenticated) → a
  `notifications` row is inserted.
- **"Where does the exact 'approved' message come from?"** `ProgressReportServiceImpl.review()` builds
  `"Your progress report" + " for " + period + " has been " + outcome + "." + " Reviewer comment: " + comment`.
- **"How does the recipient's UI get it?"** It polls `GET /unread-count` (bell badge) and lists
  `GET /notifications?status=UNREAD` (Notification Center), both scoped server-side to the recipient's user id.
- **"What if notification-service is down?"** The notify calls are best-effort (try/catch at two
  layers) — the submit/review still succeeds; only the notification is skipped and logged.

---

### File cheat-sheet (print this)
- **FE:** `disbursements/milestones/milestone-scheduler.component.ts` (submit button),
  `progress/reports/progress-reports.component.ts` (form + review), `progress/progress.service.ts`,
  `features/notifications/notifications.service.ts` + `notification-center.component.ts`,
  `layout/main-layout/main-layout.component.ts` (bell), `core/interceptors/auth.interceptor.ts` + `error.interceptor.ts`
- **core-service:** `progress/controller/ProgressReportController.java`,
  `progress/service/impl/ProgressReportServiceImpl.java` (submit/review + notify* helpers),
  `notification/service/impl/NotificationServiceImpl.java` + `notification/client/NotificationClient.java`
- **notification-service:** `controller/InternalNotificationController.java`,
  `service/impl/NotificationServiceImpl.java`, `entity/Notification.java`
- **shared:** `common-lib/.../handler/GlobalExceptionHandler.java`
