# GrantTrack — Notification Module Deep-Dive (Interview Study Guide)

Everything about the Notification module end-to-end: how a notification is created (two ways —
system events and the admin form), how it is targeted to one specific user, how each role sees only
their own notifications, how the "Send notification" button is gated by role, how the frontend talks
to the notification **microservice** through the gateway, how the category chips filter/sort the
list, how the unread **badge** stays live, how **Mark read / Dismiss** work, and **how exceptions are
handled** — all with exact file locations and the real code.

> The Notification module spans three places:
> 1. **notification-service** — `Backend/notification-service` (owns the `notifications` table + APIs).
> 2. **Other services (core, finance)** — call notification-service over **Feign** to fire system
>    notifications (`Backend/core-service/.../notification`, same in finance).
> 3. **Frontend** — `Frontend/src/app/features/notifications` (the Notification Center + service) and
>    `Frontend/src/app/layout/main-layout` (the bell badge).

---

## 0. The 10-second summary (say this first)

> "A notification is a row in the `notifications` table with a **target `userId`**, a **message**, a
> **category** and a **status** (UNREAD/READ/DISMISSED). It's created two ways: **system events**
> (e.g. an award approved) — where a business service calls notification-service over **Feign** — and
> **manually** by a Grant Admin via a form. Users only ever read **their own** notifications because
> every read is scoped server-side to the authenticated user id. The Angular shell polls an
> **unread-count** endpoint to keep the **bell badge** live, and the Notification Center lets a user
> filter by category and Mark-read / Dismiss each item."

---

## 1. Data model — what a notification is

- Entity: [`notification-service/.../entity/Notification.java`](Backend/notification-service/src/main/java/com/granttrack/notification/entity/Notification.java)
  ```java
  @Entity @Table(name = "notifications", indexes = {
      @Index(name = "ix_notifications_user", columnList = "user_id"),
      @Index(name = "ix_notifications_user_status", columnList = "user_id,status") })
  public class Notification extends BaseEntity {
      @Column(name = "user_id", nullable = false) private Long userId;      // ← the recipient
      @Column(name = "message", nullable = false, length = 1000) private String message;
      @Enumerated(EnumType.STRING) private NotificationCategory category;   // APPLICATION/REVIEW/AWARD/DISBURSEMENT/PROGRESS/OUTPUT
      @Enumerated(EnumType.STRING) private NotificationStatus status = NotificationStatus.UNREAD;  // UNREAD/READ/DISMISSED
  }
  ```
- Category enum: [`NotificationCategory.java`](Backend/notification-service/src/main/java/com/granttrack/notification/entity/NotificationCategory.java) — `APPLICATION, REVIEW, AWARD, DISBURSEMENT, PROGRESS, OUTPUT`.
- Status enum: `NotificationStatus` — `UNREAD, READ, DISMISSED`.
- The `user_id` + `(user_id,status)` indexes exist because the two hottest queries are "my
  notifications" and "my unread count".

**Talking point:** targeting is dead simple and robust — one column, `user_id`. There's no "roles"
column; a role-wide notification is just N rows, one per user (the *sender* decides who).

---

## 2. How each user (any role) gets only THEIR notifications

Every read is scoped to the **authenticated user id** — never to a role, never trusting the client.

- Endpoints: [`notification-service/.../controller/NotificationController.java`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/NotificationController.java) — base path `/api/v1/notifications`.
- Logic: [`notification-service/.../service/impl/NotificationServiceImpl.java`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)

```java
// listForCurrentUser
Long userId = currentUserId();                                   // from the JWT (SecurityUtils)
Specification<Notification> spec = (root, cq, cb) -> {
    predicates.add(cb.equal(root.get("userId"), userId));        // ← ONLY rows for the caller
    if (hasText(status)) predicates.add(cb.equal(root.get("status"), parseStatus(status)));
    return cb.and(...);
};
return notificationRepository.findAll(spec, pageable).map(mapper::toResponse);

private Long currentUserId() {
    return SecurityUtils.getCurrentUserId().orElseThrow(() -> new BusinessException("No authenticated user"));
}
```
`SecurityUtils.getCurrentUserId()` reads the id the JWT filter put in the security context (see the
Auth guide). So a Researcher sees only researcher-targeted rows, a Finance Officer only theirs, etc.
— purely because their user id differs. **The same code serves every role**; the scoping is the user id.

**Interview line:** *"There's no per-role query. A Finance Officer and a Researcher hit the exact
same endpoint; each just gets the rows whose `user_id` equals their own id from the token."*

---

## 3. How the "Send notification" button is gated by role (two layers)

Only Grant Admin / Admin can send. Enforced on **both** the UI and the server.

### Frontend (UX) — [`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts) template
```html
<button class="btn btn-primary ..." *gtHasRole="[Role.GRANT_ADMIN, Role.ADMIN]" (click)="openCreate()">
  <gt-icon name="plus" [size]="16" /> Send notification
</button>
```
`*gtHasRole` is a structural directive ([`shared/directives/has-role.directive.ts`]) that removes the
element unless the current user holds one of the listed roles (it reads `AuthService.roles()`). So a
Researcher simply never sees the button.

### Backend (the real gate) — [`NotificationController.create`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/NotificationController.java)
```java
@PostMapping
@PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")     // enforced server-side, regardless of UI
public ResponseEntity<ApiResponse<NotificationResponse>> create(@Valid @RequestBody NotificationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Notification created", notificationService.create(request)));
}
```
**Talking point:** the directive is only convenience; even if someone forged the request, the
`@PreAuthorize` rejects it with 403. UI hiding ≠ security.

---

## 4. How the frontend talks to the notification MICROSERVICE

The browser never calls notification-service directly — it calls the **gateway**, which routes
`/api/v1/notifications/**` to notification-service (discovered via Eureka).

### The Angular service — [`notifications.service.ts`](Frontend/src/app/features/notifications/notifications.service.ts)
```ts
private base = `${environment.apiUrl}/notifications`;   // e.g. http://localhost:8080/api/v1/notifications

/** Live unread count shared with the shell bell badge. */
readonly unread = signal(0);

list(query)        { return this.http.get(this.base, { params: toHttpParams(query) }); }   // GET /notifications?status=&page=
unreadCount()      { return this.http.get(`${this.base}/unread-count`); }                    // GET /notifications/unread-count
markRead(id)       { return this.http.patch(`${this.base}/${id}/read`, null); }              // PATCH /notifications/{id}/read
dismiss(id)        { return this.http.patch(`${this.base}/${id}/dismiss`, null); }           // PATCH /notifications/{id}/dismiss
create(body)       { return this.http.post(this.base, body); }                               // POST  /notifications
refreshUnread()    { this.unreadCount().subscribe({ next: r => this.unread.set(r.data ?? 0), error: () => {} }); }
```

### The hop
```
Browser → /api/v1/notifications/... → API Gateway (:8080) → notification-service (:8082)
```
- Gateway route — [`api-gateway/.../application.yml`](Backend/api-gateway/src/main/resources/application.yml):
  ```yaml
  - id: notification-service
    uri: lb://notification-service            # resolved & load-balanced via Eureka
    predicates: [ Path=/api/v1/notifications/** ]
  ```
- The **JWT is attached automatically** by the global `authInterceptor` (see the Auth guide §5) — you
  never add the header in `NotificationService`.

**Talking point (microservice angle):** notification-service is its own Spring Boot app registered in
Eureka; the frontend is decoupled from its host/port because everything goes through `lb://` behind
the gateway.

---

## 5. Category chips — how the list is filtered/sorted by category when you click

This is done **client-side** with an Angular **signal + computed** — no extra server call.

- [`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts)
```ts
readonly categoryFilter = signal<NotificationCategory | 'ALL'>('ALL');

readonly filteredRows = computed(() => {              // recomputes whenever rows() or categoryFilter() change
  const cat = this.categoryFilter();
  const all = this.rows();
  return cat === 'ALL' ? all : all.filter((n) => n.category === cat);
});

onCategory(value: NotificationCategory | 'ALL'): void {
  this.categoryFilter.set(value);                     // clicking a chip just sets the signal
}
```
The chips row (template):
```html
@for (c of categoryChips; track c.value) {
  <button class="btn btn-sm rounded-pill ..."
    [class.btn-primary]="categoryFilter() === c.value"          // active chip highlighted
    [class.btn-outline-secondary]="categoryFilter() !== c.value"
    (click)="onCategory(c.value)">{{ c.label }}</button>
}
```
And the list renders `filteredRows()`, not `rows()`:
```html
@for (n of filteredRows(); track n.id) { ... <gt-status-badge [status]="n.category" /> ... }
```

**How "sorting/grouping by category" happens:** clicking a chip → `onCategory` sets `categoryFilter`
→ the `filteredRows` **computed** re-runs and returns only that category's rows → Angular re-renders
the list. It's instant because the page already has the rows in memory; the category filter is a pure
in-memory `.filter()`. (The category itself is stamped on each notification when it's created — §7 —
so the chip can group by it.)

> **Why client-side?** The list is already loaded and paginated; category is a display facet. The
> server-side filter that *does* hit the DB is **status** (Unread/Read/Dismissed, §8), because that
> changes which rows are relevant.

---

## 6. The admin "Send notification" form — full path, auth, token, storage

### Step 1 — the form ([`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts))
```ts
readonly form = this.fb.nonNullable.group({
  userId:   [null as number | null, [Validators.required]],   // WHO to notify
  message:  ['', [Validators.required]],
  category: ['' as NotificationCategory | '', [Validators.required]],
});

save(): void {
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }
  const v = this.form.getRawValue();
  const body = { userId: v.userId!, message: v.message, category: v.category as NotificationCategory };
  this.saving.set(true);
  this.api.create(body).subscribe({
    next: () => { this.toast.success('Notification sent.'); this.modalOpen.set(false);
                  this.saving.set(false); this.refreshUnread(); this.load(); },
    error: () => this.saving.set(false),
  });
}
```

### Step 2 — how it authenticates & attaches the token
`this.api.create(body)` → `POST http://localhost:8080/api/v1/notifications`. On the way out, the
global **`authInterceptor`** ([`core/interceptors/auth.interceptor.ts`](Frontend/src/app/core/interceptors/auth.interceptor.ts)) clones the request and adds:
```ts
req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });   // token from TokenStorageService
```

### Step 3 — where it goes and how it's stored
```
POST /api/v1/notifications → Gateway → notification-service
  → JwtClaimsAuthenticationFilter validates the token, builds the security context
  → @PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')") authorizes the caller
  → NotificationController.create() → NotificationServiceImpl.create()
```
```java
// NotificationServiceImpl
public NotificationResponse create(NotificationRequest request) {
    return notify(request.userId(), request.message(), parseCategory(request.category()));
}
public NotificationResponse notify(Long userId, String message, NotificationCategory category) {
    Notification n = Notification.builder()
        .userId(userId)                          // ← the admin-chosen recipient
        .message(message).category(category)
        .status(NotificationStatus.UNREAD)
        .build();
    return mapper.toResponse(notificationRepository.save(n));   // one row inserted
}
```
So the admin's form → one `notifications` row with `user_id = <chosen user>`, `status = UNREAD`. Next
time that user opens the Notification Center (or the badge polls), they see it (§2).

**Talking point (validation + envelope):** `@Valid NotificationRequest` enforces `userId`/`message`/
`category` server-side; the response comes back in the standard `ApiResponse` envelope.

---

## 7. The OTHER way notifications are created — system events (cross-service, Feign)

Most notifications aren't manual — they're fired by business events (award approved, reviewer
assigned, milestone disbursed, co-investigator invited…). Because those live in **other services**,
they call notification-service over **OpenFeign**.

### Caller side (e.g. core-service) — [`core-service/.../notification/service/impl/NotificationServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
```java
public void notify(Long userId, String message, NotificationCategory category) {
    try {
        notificationClient.publish(internalToken,                       // Feign call
            new NotificationPublishRequest(userId, message, category.name()));
    } catch (Exception ex) {
        log.warn("Failed to publish notification ...", ex);             // best-effort: never breaks the business op
    }
}
```
- Feign client: [`NotificationClient.java`](Backend/core-service/src/main/java/com/granttrack/notification/client/NotificationClient.java) → `@FeignClient(name = "notification-service")`, `POST /internal/notifications`.
- Business code just calls `notificationService.notify(piId, "Your award was approved.", AWARD)` — it
  doesn't know or care that a network hop happens. (Example: the award status-change notifies the PI.)

### Receiver side — [`InternalNotificationController.java`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/InternalNotificationController.java)
```java
@PostMapping("/internal/notifications")
public ResponseEntity<Void> publish(@RequestHeader("X-Internal-Token") String token,
                                    @Valid @RequestBody NotificationRequest request) {
    if (!internalToken.equals(token)) return ResponseEntity.status(FORBIDDEN).build();  // service-to-service auth
    notificationService.create(request);                                                // same create() as §6
    return ResponseEntity.status(CREATED).build();
}
```
**Two things to stress:**
- This `/internal/**` endpoint is authenticated by a **shared internal token** (not a user JWT), and
  is **not routed by the gateway**, so it's only reachable service-to-service.
- Delivery is **best-effort** — if notification-service is down, the award/milestone action still
  succeeds; the notification is just logged as failed. (Resilience.)

**So the two creation paths converge on the same `create()`:** manual admin form (user JWT, gateway,
`/api/v1/notifications`) and system events (internal token, Feign, `/internal/notifications`).

---

## 8. The unread badge — how the count updates and shows

The bell in the shell shows a live unread count, kept fresh three ways: on navigation, on a 60-second
poll, and after any read/dismiss/send.

### The shared signal — [`notifications.service.ts`](Frontend/src/app/features/notifications/notifications.service.ts)
```ts
readonly unread = signal(0);
refreshUnread(): void {
  this.unreadCount().subscribe({ next: r => this.unread.set(r.data ?? 0), error: () => {} });
}
```
`unreadCount()` → `GET /api/v1/notifications/unread-count` → server:
```java
// NotificationServiceImpl
public long unreadCountForCurrentUser() {
    return notificationRepository.countByUserIdAndStatus(currentUserId(), NotificationStatus.UNREAD);
}
```

### The shell keeps it fresh — [`layout/main-layout/main-layout.component.ts`](Frontend/src/app/layout/main-layout/main-layout.component.ts)
```ts
readonly unread = this.notifications.unread;              // the same signal
...
this.router.events...subscribe(() => { ...; this.notifications.refreshUnread(); });  // on navigation
this.notifications.refreshUnread();                                                  // initial
setInterval(() => this.notifications.refreshUnread(), 60_000);                       // light 60s poll
```

### The badge — [`main-layout.component.html`](Frontend/src/app/layout/main-layout/main-layout.component.html)
```html
<a routerLink="/notifications" class="btn btn-ghost p-2 position-relative" ...>
  <gt-icon name="bell" [size]="19" />
  @if (unread() > 0) {
    <span class="position-absolute badge rounded-pill bg-danger ...">{{ unread() > 99 ? '99+' : unread() }}</span>
  }
</a>
```
**Talking point:** the count is a **signal** shared between the Notification Center and the shell, so
when you mark something read the badge updates immediately (the center calls `refreshUnread()`), and
the 60-second poll surfaces newly-arrived notifications without a manual refresh. The badge caps at
"99+".

---

## 9. Mark read / Dismiss — how they work

Each acts on one notification, re-checks ownership server-side, flips the status, then the UI
refreshes the list + the unread count.

### Frontend — [`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts)
```ts
markRead(n) { this.api.markRead(n.id).subscribe(() => { this.toast.success('...'); this.refreshUnread(); this.load(); }); }
dismiss(n)  { this.api.dismiss(n.id).subscribe(()  => { this.toast.success('...'); this.refreshUnread(); this.load(); }); }
```
Template (buttons appear per row; "Mark read" only when still UNREAD):
```html
@if (n.status === 'UNREAD') { <button (click)="markRead(n)"> Mark read </button> }
<button (click)="dismiss(n)"> Dismiss </button>
```

### Backend — [`NotificationServiceImpl`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
```java
public NotificationResponse markRead(Long id) {
    Notification n = findOwned(id);                 // 404 if not found, 403 if not yours
    n.setStatus(NotificationStatus.READ);
    return mapper.toResponse(notificationRepository.save(n));
}
public NotificationResponse dismiss(Long id) {
    Notification n = findOwned(id);
    n.setStatus(NotificationStatus.DISMISSED);
    return mapper.toResponse(notificationRepository.save(n));
}
private Notification findOwned(Long id) {
    Notification n = notificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    if (!n.getUserId().equals(currentUserId())) throw new AccessDeniedException("Not your notification");
    return n;
}
```
**Talking point (security):** `findOwned` guarantees you can only mark-read/dismiss **your own**
notifications — a user can't touch someone else's by guessing an id (they'd get 403). The status
filter (§2) then hides READ/DISMISSED items from the default "Unread" view.

---

## 10. Exception handling in the Notification module (frontend + backend)

One consistent contract: the backend throws typed exceptions → one central handler turns them into an
HTTP status + the standard `ApiResponse` envelope → the frontend's one error interceptor turns that
into a toast. Here's exactly how it plays out **for this module**.

### 10a. Where the notification code *throws*
- [`notification-service/.../service/impl/NotificationServiceImpl.java`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
```java
private Long currentUserId() {
    return SecurityUtils.getCurrentUserId()
        .orElseThrow(() -> new BusinessException("No authenticated user"));      // → 409
}
private Notification findOwned(Long id) {
    Notification n = notificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Notification", id));    // → 404 (bad id)
    if (!n.getUserId().equals(currentUserId()))
        throw new AccessDeniedException("Not your notification");                 // → 403 (someone else's)
    return n;
}
private NotificationStatus parseStatus(String raw) {
    try { return NotificationStatus.valueOf(raw.toUpperCase()); }
    catch (IllegalArgumentException ex) { throw new BusinessException("Invalid status: " + raw); }  // → 409
}
private NotificationCategory parseCategory(String raw) {
    try { return NotificationCategory.valueOf(raw.toUpperCase()); }
    catch (IllegalArgumentException ex) { throw new BusinessException("Invalid category: " + raw); } // → 409
}
```
Plus bean-validation on the request: `@Valid NotificationRequest` (`@NotNull userId`, `@NotBlank
@Size(max=1000) message`, `@NotNull category`) → a bad body is a **400** before the service runs.
And `@PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")` on `create` → **403** for other roles.

These exception types are shared (`common-lib`): `BusinessException`, `ResourceNotFoundException`,
`AccessDeniedException`.

### 10b. Where they're *translated* — the one central handler (backend)
[`common-lib/.../handler/GlobalExceptionHandler.java`](Backend/common-lib/src/main/java/com/granttrack/common/handler/GlobalExceptionHandler.java) (`@RestControllerAdvice`, loaded by every service incl.
notification-service):
```java
@ExceptionHandler(ResourceNotFoundException.class)          // 404
@ExceptionHandler({BusinessException.class, OptimisticLockingFailureException.class})  // 409
@ExceptionHandler(MethodArgumentNotValidException.class)    // 400 (+ field errors)
@ExceptionHandler(AccessDeniedException.class)              // 403 "Access denied: insufficient privileges"
@ExceptionHandler(Exception.class)                          // 500 (logged, generic message)
```
Every one builds the same envelope: `ApiResponse.error(message, errorDetails)` with the right HTTP
status. So a "not your notification" comes back as **403** with `{ success:false, message:"Access
denied: insufficient privileges", ... }`.

Exceptions that happen **inside the security filter** (an expired/invalid JWT before the controller
runs) are written by [`RestAuthenticationEntryPoint`](Backend/common-lib/src/main/java/com/granttrack/common/security/RestAuthenticationEntryPoint.java) (401) and `RestAccessDeniedHandler` (403) — the
**same** JSON envelope, so the frontend treats them uniformly.

| Notification action gone wrong | Exception | HTTP |
|---|---|---|
| Mark-read/dismiss someone else's notification | `AccessDeniedException` | 403 |
| Mark-read/dismiss a non-existent id | `ResourceNotFoundException` | 404 |
| Admin sends with a blank message / null userId | `MethodArgumentNotValidException` | 400 |
| Admin sends an unknown category | `BusinessException` | 409 |
| Non-admin calls the create endpoint | `@PreAuthorize` denial | 403 |
| Expired/invalid token on any call | (security filter) | 401 |

### 10c. The cross-service (Feign) exception path — best-effort, swallowed
When a business event fires a notification and notification-service is unreachable, the failure must
**not** break the business action. The caller wraps the Feign call in try/catch:
```java
// core-service NotificationServiceImpl (the Feign-backed impl)
public void notify(Long userId, String message, NotificationCategory category) {
    try { notificationClient.publish(internalToken, new NotificationPublishRequest(userId, message, category.name())); }
    catch (Exception ex) { log.warn("Failed to publish notification to user {} ...", userId, ex); }  // swallow
}
```
So if notification-service is down while approving an award, the award still approves; only the
notification is lost (and logged). **Interview line:** *"Notifications are best-effort by design —
a notification outage is never allowed to roll back a real business transaction."*

### 10d. Where they're *shown* — the frontend
1. **Global error interceptor** — [`core/interceptors/error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts) turns any backend error
   into a toast, reading the backend's `message`:
   ```ts
   function extractMessage(error: HttpErrorResponse): string {
     const body = error.error;
     if (body?.message) { ...return body.message; }               // e.g. "Access denied: insufficient privileges"
     if (error.status === 0) return 'Cannot reach the server. Is the backend running?';
     return `Request failed (${error.status})`;
   }
   ```
   So a failed **Send** / **Mark read** / **Dismiss** shows a clear toast (the interceptor is global,
   so the Notification Center's own `subscribe` blocks don't need per-call error handling — they just
   reset `saving`/`loading`).
2. **The bell poll stays silent on purpose** — [`notifications.service.ts`](Frontend/src/app/features/notifications/notifications.service.ts):
   ```ts
   refreshUnread(): void {
     this.unreadCount().subscribe({ next: r => this.unread.set(r.data ?? 0), error: () => {} });  // swallow
   }
   ```
   The unread-count is a **background** call (runs on a 60-second timer and on navigation); if it
   fails transiently we must **not** spam the user with a toast, so its error is deliberately swallowed
   (`error: () => {}`) while the visible actions (send/mark/dismiss) still surface errors via the
   interceptor.
3. **Component-level error state** — e.g. `create`/`markRead`/`dismiss` `subscribe({ error: ... })`
   just clears the `saving()`/`loading()` spinner; the *message* is handled centrally by the
   interceptor, so there's no duplicated error text.

**One-line summary to say:** *"Backend: typed exceptions → one `@RestControllerAdvice` → standard
envelope + right status. Frontend: one error interceptor → toast. Cross-service notifications are
best-effort (swallowed). Background polls stay silent so they never spam toasts."*

---

## 11. End-to-end walkthrough (rehearse out loud)

**A. System notification (the common case)**
1. A business event happens (e.g. Grant Admin changes an award status in core-service).
2. Core calls `notificationService.notify(piId, "...", AWARD)` → **Feign** `NotificationClient.publish`
   with the internal token → notification-service `/internal/notifications`.
3. `InternalNotificationController` checks the token → `create()` → inserts a `notifications` row with
   `user_id = piId`, `status = UNREAD`.
4. The PI's shell polls `unread-count` (or navigates) → the **bell badge** increments.
5. The PI opens the Notification Center → `list(status=UNREAD)` returns their rows → they filter by
   the **Award** chip → click **Mark read** → row goes READ, badge decrements.

**B. Manual notification (admin form)**
1. Grant Admin sees the **Send notification** button (`*gtHasRole`); a Researcher doesn't.
2. They fill userId + category + message → `create(body)` → `authInterceptor` attaches the JWT →
   `POST /api/v1/notifications` → gateway → notification-service.
3. `@PreAuthorize('GRANT_ADMIN','ADMIN')` authorizes → `create()` inserts the row for that user.
4. That user then sees it exactly like case A.

---

## 12. Likely interview questions (crisp answers)

- **"How does a notification reach one specific user?"** Each row stores a `user_id`; reads are
  scoped to the authenticated user's id server-side, so users only ever see their own.
- **"How do different roles get their notifications?"** Same endpoint for everyone — the scoping is
  the user id from the JWT, not the role. A role-wide message is just multiple rows.
- **"Who can send, and how is that enforced?"** Grant Admin / Admin. UI hides the button with
  `*gtHasRole`; the server enforces it with `@PreAuthorize("hasAnyRole('GRANT_ADMIN','ADMIN')")`.
- **"How does the frontend reach the notification service?"** Through the gateway
  (`environment.apiUrl` → `/api/v1/notifications`), routed by path to notification-service via Eureka;
  the JWT interceptor attaches the token.
- **"How do system notifications work across services?"** The business service calls notification-
  service over OpenFeign to `/internal/notifications`, authenticated by a shared internal token, and
  it's best-effort so it never breaks the business transaction.
- **"How does category filtering work?"** Client-side: a `categoryFilter` signal + a `filteredRows`
  computed that filters the in-memory list; clicking a chip sets the signal.
- **"How is the unread badge kept live?"** A shared `unread` signal fed by `GET /unread-count`,
  refreshed on navigation, on a 60-second poll, and after every read/dismiss/send.
- **"How do Mark read / Dismiss stay secure?"** `findOwned` re-checks the notification belongs to the
  caller (403 otherwise) before flipping the status.
- **"How are exceptions handled?"** Typed exceptions → one `@RestControllerAdvice` → standard
  envelope + HTTP status; the frontend's one error interceptor toasts the message; cross-service
  notifications and the background unread poll swallow errors so they never break work or spam toasts.
- **"Why is category client-side but status server-side?"** Status decides which rows are relevant
  (a DB query); category is a display facet on already-loaded rows.

---

### File cheat-sheet (print this)
- **notification-service:** `controller/NotificationController.java` (+ `InternalNotificationController.java`),
  `service/impl/NotificationServiceImpl.java`, `service/NotificationService.java`,
  `entity/{Notification,NotificationCategory,NotificationStatus}.java`,
  `dto/request/NotificationRequest.java`, `dto/response/NotificationResponse.java`, `mapper/NotificationMapper.java`,
  `config/NotificationSecurityConfig.java`
- **caller side (core/finance):** `notification/service/impl/NotificationServiceImpl.java`,
  `notification/client/NotificationClient.java`, `notification/client/NotificationPublishRequest.java`,
  `notification/service/NotificationService.java`, `notification/entity/NotificationCategory.java`
- **frontend:** `features/notifications/notifications.service.ts`,
  `features/notifications/notification-center.component.ts`,
  `layout/main-layout/main-layout.component.ts` + `.html` (bell badge),
  `core/models/notification.model.ts`, `core/interceptors/auth.interceptor.ts` + `error.interceptor.ts`
- **shared (exceptions):** `common-lib/.../handler/GlobalExceptionHandler.java`,
  `common-lib/.../security/{RestAuthenticationEntryPoint,RestAccessDeniedHandler}.java`,
  `common-lib/.../exception/{BusinessException,ResourceNotFoundException}.java`
