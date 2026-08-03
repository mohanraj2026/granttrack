# GrantTrack — Project Study Guide (for the final review)

A structured, code-referenced guide to explain this project confidently. It covers the
**overall architecture**, **frontend ↔ backend communication**, and deep dives into your
allocated modules — **Auth**, **Funding**, **Notification**, **Common** (audit + exception
handling) — plus **microservices** and the **database**.

> **How to read this:** every concept is followed by the **exact file** and the **key code**.
> When you demo/explain, open the referenced file and point at the block.

---

## 0. Tech stack in one breath

- **Frontend:** Angular 20 (standalone components, signals), Bootstrap 5, RxJS, TypeScript.
- **Backend:** Java 21, Spring Boot 3.3, Spring Cloud 2023.0.3 (Eureka + Gateway + OpenFeign),
  Spring Security 6 (JWT), Spring Data JPA / Hibernate, Flyway, MapStruct, Lombok, MySQL 8.
- **Shape:** a **modular monolith** (`core-service`) with **3 microservices carved out**
  (`auth`, `notification`, `finance`), behind an **API gateway** and a **Eureka registry**,
  all sharing **one MySQL database**.

---

## 1. Big-picture architecture

```
                         ┌──────────────────┐
   Angular app (4200) ──►│   api-gateway     │  ← the ONLY backend URL the browser calls
                         └────────┬──────────┘
                                  │ routes by URL path, load-balanced via Eureka
     ┌──────────────┬────────────┼───────────────┬────────────────┐
     ▼              ▼            ▼               ▼                 ▼
 auth-service  notification-svc  finance-service  core-service
  (identity)    (notifications)   (disbursement)  (everything else: funding, application,
                     ▲                 │            review, award, progress, output)
                     │  Feign (best-effort publish)│
   core & finance ───┴─────────────────────────────┘
                                  │
                    shared MySQL `granttrack`  +  Eureka registry (8761)
```

| Component | Type | Port | Owns / does |
|---|---|---|---|
| `eureka-server` | infra | 8761 | Service registry (discovery) |
| `api-gateway` | infra | 8080* | Single entry point, routing, CORS |
| `auth-service` | **microservice** | 8081 | Login, JWT issuance, users, roles, audit-log read API |
| `notification-service` | **microservice** | 8082 | In-app notifications + internal publish API |
| `finance-service` | **microservice** | 8083 | Disbursement milestones + fund releases |
| `core-service` | **monolith** | 8084 | funding, application, review, award, progress, output |
| `common-lib` | shared jar | — | DTOs, exceptions, `BaseEntity`, audit, JWT-validation |

> \* If the gateway port is changed (e.g. `8089` because Jenkins uses 8080), the frontend
> `apiUrl` must match — see §3.

**One-line answer for "which is microservice / which is monolith":**
Auth, Notification, and Finance are microservices; **core-service is the monolith**;
eureka-server and api-gateway are supporting infrastructure.

---

## 2. Microservices — how they work & communicate

Four mechanisms — memorise these four words: **Discovery, Routing, Auth, Feign.**

1. **Discovery (Eureka).** Every service is a Eureka *client* (`@EnableDiscoveryClient`) and
   registers itself at `http://localhost:8761/eureka` on startup. Nobody hard-codes host:port;
   they refer to each other by **service name** (`auth-service`, `notification-service`, …).
   - Registry app: [`Backend/eureka-server/.../EurekaServerApplication.java`](Backend/eureka-server/src/main/java/com/granttrack/eureka/EurekaServerApplication.java) (`@EnableEurekaServer`).

2. **Routing (API Gateway).** The browser only ever calls the gateway. The gateway maps URL
   paths → services and load-balances via Eureka (`lb://<service>`).
   - Config: [`Backend/api-gateway/src/main/resources/application.yml`](Backend/api-gateway/src/main/resources/application.yml)
   ```yaml
   routes:
     - id: auth-service                       # /auth, /users, /audit-logs  → auth-service
       uri: lb://auth-service
       predicates: [ Path=/api/v1/auth/**,/api/v1/users/**,/api/v1/audit-logs/** ]
     - id: notification-service               # /notifications → notification-service
       uri: lb://notification-service
       predicates: [ Path=/api/v1/notifications/** ]
     - id: finance-service                    # /disbursements → finance-service
       uri: lb://finance-service
       predicates: [ Path=/api/v1/disbursements/** ]
     - id: core-service                       # everything else → the monolith
       uri: lb://core-service
       predicates: [ Path=/api/v1/**,/v3/api-docs/**,/swagger-ui/** ]
   ```

3. **Auth (shared-secret JWT).** `auth-service` *issues* a JWT. Every other service *validates*
   it locally from the token's claims (no DB call), because all services share the **same HMAC
   secret**. (Details in §4.)

4. **Feign (service-to-service calls).** When core/finance need another service, they call it
   over **OpenFeign** (HTTP, resolved through Eureka). Today the only such call is publishing a
   notification (§6). These calls are **best-effort** — wrapped in try/catch so a downstream
   outage never breaks the business action.

**Shared database:** one MySQL schema `granttrack`. `core-service` owns all Flyway migrations
and is the only service that changes the schema; the others run `flyway.enabled=false` +
`ddl-auto=validate` and map only the tables they use (some as **read-only projections**).

**Start order (fresh DB):** eureka → core (migrates the DB) → auth/notification/finance → gateway.
Use [`Backend/run-all.ps1`](Backend/run-all.ps1) or `mvn -pl <service> spring-boot:run`.

---

## 3. Frontend ↔ Backend — how they connect & communicate

**Base URL → the gateway.** Every Angular service builds its URL from one constant:
- [`Frontend/src/environments/environment.development.ts`](Frontend/src/environments/environment.development.ts)
  ```ts
  export const environment = { production: false, apiUrl: 'http://localhost:8080/api/v1' };
  ```
  So `AuthService` calls `${apiUrl}/auth/...`, `FundingService` calls `${apiUrl}/funding/...`, etc.
  The browser talks **only** to the gateway; the gateway fans out to the services.

**Two HTTP interceptors run on every request** (registered in
[`Frontend/src/app/app.config.ts`](Frontend/src/app/app.config.ts)):

1. **`authInterceptor`** — attaches the JWT and transparently refreshes it on a 401.
   - [`Frontend/src/app/core/interceptors/auth.interceptor.ts`](Frontend/src/app/core/interceptors/auth.interceptor.ts)
   ```ts
   const authReq = token && !isBypass ? addToken(req, token) : req;   // add "Authorization: Bearer <jwt>"
   return next(authReq).pipe(catchError(err => {
     if (err.status === 401 && !isBypass) return handle401(...);      // refresh + replay once
     return throwError(() => err);
   }));
   ```
   On a 401 it calls `/auth/refresh` (rotating the token), then replays the original request.
   Concurrent 401s share a single refresh via a `BehaviorSubject`.

2. **`errorInterceptor`** — turns any backend error into a user toast using the standard envelope.
   - [`Frontend/src/app/core/interceptors/error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts)
   ```ts
   if (error.status === 0) return 'Cannot reach the server. Is the backend running?';
   ```
   > This is exactly the toast you see when the gateway isn't running or the `apiUrl` port is wrong.

**Request lifecycle (end to end):**
```
Component → Angular service (HttpClient) → authInterceptor adds JWT
   → HTTP to gateway (:8080) → gateway routes by path → target microservice
   → Spring Security validates JWT → @PreAuthorize checks role → controller → service → repo → MySQL
   → ApiResponse envelope back → errorInterceptor (toast on failure) → component updates signals → UI
```

**The response envelope** every endpoint returns (so the frontend always reads `.data`):
- [`Backend/common-lib/.../common/dto/ApiResponse.java`](Backend/common-lib/src/main/java/com/granttrack/common/dto/ApiResponse.java)
  ```json
  { "success": true, "message": "Login successful", "data": { ... }, "timestamp": "..." }
  ```

---

## 4. AUTH MODULE — authentication & authorization  ⭐ (your module)

Lives in **`auth-service`** (package `com.granttrack.auth`). It does 4 things: **register**,
**login (issue JWT)**, **validate JWT on every request**, and **authorize by role**.

### 4.1 Files map
| Concern | File |
|---|---|
| Endpoints | [`auth/controller/AuthController.java`](Backend/auth-service/src/main/java/com/granttrack/auth/controller/AuthController.java) |
| Core logic | [`auth/service/impl/AuthServiceImpl.java`](Backend/auth-service/src/main/java/com/granttrack/auth/service/impl/AuthServiceImpl.java) |
| Token creation | [`auth/security/JwtTokenProvider.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtTokenProvider.java) |
| Login DB check | [`auth/security/CustomUserDetailsService.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/CustomUserDetailsService.java) + `CustomUserDetails.java` |
| Auth-service request filter (DB-backed) | [`auth/security/JwtAuthenticationFilter.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtAuthenticationFilter.java) |
| Security rules | [`auth/security/SecurityConfig.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/SecurityConfig.java) |
| **Resource-service** JWT validation (claims-only) | [`common-lib/.../security/JwtClaimsAuthenticationFilter.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/JwtClaimsAuthenticationFilter.java) + `JwtTokenValidator.java` |

### 4.2 Registration flow
`POST /api/v1/auth/register` (multipart: JSON + optional college-ID/photo).
- `AuthController.register()` → `AuthServiceImpl.register()`:
  ```java
  if (userRepository.existsByEmail(request.email())) throw new DuplicateResourceException(...);
  Role researcher = roleRepository.findByName(RoleName.ROLE_RESEARCHER.name())...;   // self-signup = researcher
  User user = User.builder().email(...).password(passwordEncoder.encode(request.password()))  // BCrypt hash
                 .status(UserStatus.ACTIVE).roles(Set.of(researcher)).build();
  userRepository.save(user);
  // then store uploaded college-id / profile-photo files, save paths on the user
  ```
Key points to say: **passwords are BCrypt-hashed** (never stored plaintext); self-registration
always yields **ROLE_RESEARCHER**; staff accounts are created by an admin (`user` module).

### 4.3 Login flow (issuing the JWT) — the important one
`POST /api/v1/auth/login` with `{ email, password }`.
- `AuthServiceImpl.login()`:
  ```java
  Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(request.email(), request.password()));
  CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
  return issueTokens(principal);
  ```
  - `authenticationManager.authenticate(...)` triggers Spring Security to call
    `CustomUserDetailsService.loadUserByUsername(email)` → loads the `User` from DB and wraps it as
    `CustomUserDetails`; Spring then compares the submitted password against the stored BCrypt hash.
    If it fails → `BadCredentialsException` → handled as 401.
- `issueTokens()` builds the token pair:
  ```java
  String accessToken  = tokenProvider.generateAccessToken(principal.getId(), principal.getEmail(), roleNames);
  String refreshToken = UUID.randomUUID()... ;                 // opaque, persisted in refresh_tokens
  refreshTokenRepository.save(RefreshToken.builder().userId(...).token(refreshToken).expiryDate(...).build());
  return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)...build();
  ```
- **JWT contents** — [`JwtTokenProvider.generateAccessToken()`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtTokenProvider.java):
  ```java
  return Jwts.builder()
      .issuer(properties.getIssuer())                 // "granttrack"
      .subject(String.valueOf(userId))                // WHO  (the user id)
      .claim("email", email)
      .claim("roles", roles)                          // WHAT they can do (authorities)
      .issuedAt(now).expiration(expiry)               // 15-minute access token
      .signWith(key())                                // HMAC-SHA signed with the shared secret
      .compact();
  ```
The frontend stores the tokens and the user (see `handleAuth()` in
[`auth.service.ts`](Frontend/src/app/core/services/auth.service.ts)).

### 4.4 Authentication on every subsequent request
The JWT already carries `userId + email + roles`, so a service can authenticate **from the token
alone**. There are two filters:

- **Resource services** (core, notification, finance) — **no DB lookup**, claims only:
  [`common-lib/.../security/JwtClaimsAuthenticationFilter.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/JwtClaimsAuthenticationFilter.java)
  ```java
  if (token != null && tokenValidator.isValid(token)) {
      Long userId = tokenValidator.getUserId(token);
      var authorities = tokenValidator.getRoles(token).stream().map(SimpleGrantedAuthority::new).toList();
      var auth = new UsernamePasswordAuthenticationToken(new ResourcePrincipal(userId, email), null, authorities);
      SecurityContextHolder.getContext().setAuthentication(auth);   // now the request is "logged in"
  }
  ```
  `JwtTokenValidator` verifies the **signature + issuer** with the shared secret.

- **auth-service** uses a DB-backed filter ([`JwtAuthenticationFilter.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtAuthenticationFilter.java))
  that additionally checks the account is still enabled (so a token from a just-deactivated user is rejected).

Once the filter sets the `SecurityContext`, `SecurityUtils.getCurrentUserId()` and role checks work
anywhere (see §7.3).

### 4.5 Authorization (who can do what)
Two layers:

1. **URL-level** (coarse) — [`SecurityConfig.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/SecurityConfig.java) / core's `ResourceSecurityConfig`:
   ```java
   .authorizeHttpRequests(auth -> auth
       .requestMatchers(PUBLIC_PATHS).permitAll()         // login, register, swagger, health
       .anyRequest().authenticated())                     // everything else needs a valid JWT
   ```
2. **Method-level (RBAC)** — `@EnableMethodSecurity` + `@PreAuthorize` on controllers/services.
   Example from funding:
   ```java
   @PostMapping
   @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")     // only these roles can create a grant call
   public ResponseEntity<...> create(...) { ... }
   ```
   `hasAnyRole('ADMIN')` matches the authority `ROLE_ADMIN` — that's why role names in the JWT
   are `ROLE_*`. The 6 roles: `ROLE_RESEARCHER, ROLE_REVIEWER, ROLE_GRANT_ADMIN,
   ROLE_FINANCE_OFFICER, ROLE_COMPLIANCE_OFFICER, ROLE_ADMIN` ([`RoleName.java`](Backend/auth-service/src/main/java/com/granttrack/auth/entity/RoleName.java)).
   If the role is missing → `AccessDeniedException` → 403 "Access denied: insufficient privileges".

### 4.6 Token refresh (staying logged in)
Access tokens live 15 min. On expiry the frontend's `authInterceptor` calls
`POST /auth/refresh`; `AuthServiceImpl.refresh()` validates the stored refresh token, **rotates**
it (revokes the old, issues a new pair), and rejects it if the account is no longer ACTIVE.

### 4.7 One-paragraph summary to say aloud
> "A user logs in with email/password. `AuthenticationManager` checks the BCrypt hash via
> `CustomUserDetailsService`. On success, `JwtTokenProvider` signs a JWT containing the user id and
> roles with a shared secret, plus a persisted refresh token. On every later request the gateway
> forwards the `Authorization: Bearer` token; each service's JWT filter verifies the signature and
> rebuilds the security context from the claims — no session, no DB call. Authorization is then
> enforced per-method with `@PreAuthorize("hasAnyRole(...)")`."

---

## 5. FUNDING MODULE — grant calls & "Open Calls"  ⭐ (your module)

Lives in **`core-service`** (`com.granttrack.funding`). It manages **sponsors, institutions,
funding schemes,** and **grant calls**. A **grant call** is the submission window researchers
apply to.

### 5.1 Files map
| Concern | File |
|---|---|
| Endpoints | [`funding/controller/GrantCallController.java`](Backend/core-service/src/main/java/com/granttrack/funding/controller/GrantCallController.java) |
| Logic | [`funding/service/impl/GrantCallServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/GrantCallServiceImpl.java) |
| Frontend (admin manage) | [`Frontend/.../funding/calls/calls-list.component.ts`](Frontend/src/app/features/funding/calls/calls-list.component.ts) |
| Frontend (researcher sees open calls) | [`Frontend/.../applications/opportunities/opportunities.component.ts`](Frontend/src/app/features/applications/opportunities/opportunities.component.ts) |
| Frontend service | [`Frontend/.../funding/funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts) |

### 5.2 Grant-call lifecycle (state machine)
```
UPCOMING ──open()──► OPEN ──close()──► CLOSED
   │                   │
   └──────────── terminate() ──► TERMINATED     (also: → UNDER_REVIEW → AWARDED during review/award)
```
A call is **created as `UPCOMING`**, then an admin **opens** it. `open()` enforces the rules:
- [`GrantCallServiceImpl.open()`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/GrantCallServiceImpl.java)
  ```java
  if (call.getStatus() != CallStatus.UPCOMING) throw ...;                 // only UPCOMING can open
  if (call.getScheme().getStatus() != SchemeStatus.ACTIVE) throw ...;     // scheme must be ACTIVE
  if (call.getCloseDate().isBefore(LocalDate.now())) throw ...;           // window not already past
  call.setStatus(CallStatus.OPEN);
  ```
Only `ADMIN`/`GRANT_ADMIN` can create/open/close (see the `@PreAuthorize` on the controller).

### 5.3 ⭐ "When I click Open Calls, how does it show in the researcher's account?"
This is a **filter-by-status query**, end to end:

1. **Researcher opens the "Open Calls" page.** The component fetches only OPEN calls:
   [`opportunities.component.ts`](Frontend/src/app/features/applications/opportunities/opportunities.component.ts)
   ```ts
   this.funding.listCalls({ status: 'OPEN', size: 100, sort: 'closeDate,asc' })
   ```
2. **`FundingService.listCalls`** issues `GET ${apiUrl}/funding/calls?status=OPEN&size=100&sort=closeDate,asc`
   ([`funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts)).
3. **Gateway** matches `/api/v1/funding/**` → routes to **core-service**.
4. **`GrantCallController.search`** receives `status=OPEN` and delegates to the service.
5. **`GrantCallServiceImpl.search`** builds a JPA **Specification** that filters by status:
   ```java
   if (StringUtils.hasText(status))
       predicates.add(cb.equal(root.get("status"), parseStatus(status)));   // status = OPEN
   return callRepository.findAll(spec, pageable).map(mapper::toResponse);
   ```
6. The page of OPEN calls returns in the `ApiResponse` envelope; the component renders them, and
   each shows an **Apply** action that launches the application wizard (which itself also loads
   `listCalls({ status: 'OPEN' })` to pick the call to apply to).

**So there is no special "researcher feed":** researchers simply query calls whose **status =
OPEN**. Admins change a call's status with `open()`/`close()`; the moment it becomes OPEN it
appears in every researcher's Open Calls list.

### 5.4 Other funding logic worth knowing
- **Search is dynamic** (JPA `Specification`): optional filters `q` (title contains), `status`,
  `schemeId` — combined with `AND`. Good example of type-safe dynamic queries.
- **Institutions are public reads**: the security config permits `GET /api/v1/funding/institutions/**`
  anonymously so the **registration page** can populate its institution dropdown before login.
- **Validation guards**: `closeDate` can't precede `openDate`; you can't edit a `CLOSED`/`AWARDED`
  call; a call can only open under an **ACTIVE scheme**.

---

## 6. NOTIFICATION MODULE — per-user notifications  ⭐ (your module)

Lives in **`notification-service`** (`com.granttrack.notification`). It owns the `notifications`
table. Two ways a notification is created, and one way to read your own.

### 6.1 Files map
| Concern | File |
|---|---|
| Read own / mark-read / dismiss / create | [`notification-service/.../controller/NotificationController.java`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/NotificationController.java) |
| **Internal** publish (service-to-service) | [`notification-service/.../controller/InternalNotificationController.java`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/InternalNotificationController.java) |
| Logic (save, scope, count) | [`notification-service/.../service/impl/NotificationServiceImpl.java`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java) |
| Entity | [`notification-service/.../entity/Notification.java`](Backend/notification-service/src/main/java/com/granttrack/notification/entity/Notification.java) |
| Caller side (in core/finance) | [`core-service/.../notification/service/impl/NotificationServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java) + `client/NotificationClient.java` |
| Frontend | [`Frontend/.../notifications/notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts) + `notifications.service.ts` |

### 6.2 ⭐ How a notification is added for a *specific* user
The heart of it is a single persisted row carrying the **target `userId`**:
- [`Notification.java`](Backend/notification-service/src/main/java/com/granttrack/notification/entity/Notification.java) — `userId`, `message`, `category`, `status`.
- [`NotificationServiceImpl.notify()`](Backend/notification-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java):
  ```java
  Notification n = Notification.builder()
      .userId(userId)                       // ← the specific recipient
      .message(message)
      .category(category)                   // APPLICATION / REVIEW / AWARD / DISBURSEMENT / PROGRESS / OUTPUT
      .status(NotificationStatus.UNREAD)
      .build();
  return mapper.toResponse(notificationRepository.save(n));
  ```

**Where does `userId` come from?** From the business event. Example — when a reviewer is assigned
(in core's review module) it calls `notify(reviewerId, "You've been assigned…", REVIEW)`; when an
award is finance-reviewed it notifies the PI. The calling module knows *who* to notify and passes
that id.

**Cross-service path (core → notification-service).** Because notifications are now a separate
service, core doesn't write the row directly — it calls the notification-service over Feign:
```
review/award/progress code
   → core NotificationServiceImpl.notify(userId, msg, category)     // same method signature as before
      → NotificationClient (Feign) POST lb://notification-service /internal/notifications
         (header X-Internal-Token, body { userId, message, category })
         → InternalNotificationController.publish()  (checks the internal token)
            → notification-service NotificationServiceImpl.create() → save row with userId
```
- Caller: [`core .../notification/service/impl/NotificationServiceImpl.java`](Backend/core-service/src/main/java/com/granttrack/notification/service/impl/NotificationServiceImpl.java)
  ```java
  public void notify(Long userId, String message, NotificationCategory category) {
      try { notificationClient.publish(internalToken, new NotificationPublishRequest(userId, message, category.name())); }
      catch (Exception ex) { log.warn("Failed to publish notification ...", ex); }   // best-effort
  }
  ```
- Receiver guard: [`InternalNotificationController`](Backend/notification-service/src/main/java/com/granttrack/notification/controller/InternalNotificationController.java) rejects the call unless
  `X-Internal-Token` matches the shared internal token (so only trusted services can publish, and
  this endpoint is **not** exposed through the gateway).

### 6.3 How a user reads *only their own* notifications (security scoping)
`GET /api/v1/notifications` → `NotificationServiceImpl.listForCurrentUser()`:
```java
Long userId = currentUserId();                                  // from SecurityUtils / JWT
Specification<Notification> spec = (root, cq, cb) -> {
    predicates.add(cb.equal(root.get("userId"), userId));       // ← only rows for the caller
    if (hasText(status)) predicates.add(cb.equal(root.get("status"), parseStatus(status)));
    ...
};
return notificationRepository.findAll(spec, pageable).map(mapper::toResponse);
```
So the recipient scoping is enforced **server-side** by the authenticated user id — a user can
never read someone else's notifications. `markRead`/`dismiss` re-check ownership (`findOwned`).

### 6.4 ⭐ How notifications are "sorted"/filtered by category
- **Category is assigned at creation** (the `notify(..., NotificationCategory.X)` argument) and
  stored on each row, then returned in `NotificationResponse`.
- **The category filter is applied in the frontend** (client-side) in the Notification Center:
  [`notification-center.component.ts`](Frontend/src/app/features/notifications/notification-center.component.ts)
  ```ts
  readonly categoryFilter = signal<NotificationCategory | 'ALL'>('ALL');
  readonly filtered = computed(() => {
    const cat = this.categoryFilter();
    return cat === 'ALL' ? all : all.filter(n => n.category === cat);   // filter by category chip
  });
  onCategory(value) { this.categoryFilter.set(value); }
  ```
  The UI shows category "chips"; clicking one filters the loaded list by that category, and each
  row shows a category badge. (The backend list orders by `createdAt` newest-first via
  `@PageableDefault(sort = "createdAt")` and can filter by **status**; category grouping is a UI concern.)

### 6.5 Unread count + the bell badge
`GET /notifications/unread-count` → `countByUserIdAndStatus(userId, UNREAD)`. The frontend keeps a
shared signal (`unread`) that drives the red badge on the shell's bell
([`notifications.service.ts`](Frontend/src/app/features/notifications/notifications.service.ts) `refreshUnread()`).

---

## 7. COMMON MODULE — audit logging & exception handling  ⭐ (your module)

Lives in **`common-lib`** (`com.granttrack.common`), a jar every service depends on. Study three
things: the **audit trail**, the **exception handling**, and the **response envelope / base entity**.

### 7.1 ⭐ Audit-log data flow (how sensitive actions are recorded)
An **AOP aspect** writes an `audit_logs` row automatically after any method annotated
`@Auditable` succeeds. Nothing in the business code writes audit logs manually.

**Files & flow:**
1. **Marker annotation** — [`common/audit/Auditable.java`](Backend/common-lib/src/main/java/com/granttrack/common/audit/Auditable.java): `@Auditable(action="RELEASE_FUNDS", entityType="FundDisbursement")`.
2. **The aspect** — [`common/audit/AuditAspect.java`](Backend/common-lib/src/main/java/com/granttrack/common/audit/AuditAspect.java):
   ```java
   @Around("@annotation(auditable)")
   public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
       Object result = pjp.proceed();                             // run the real method first
       Long userId   = SecurityUtils.getCurrentUserId().orElse(null);   // who did it (from JWT)
       Long recordId = extractId(result);                        // what row (result.getId())
       auditRecorder.record(userId, auditable.action(), auditable.entityType(), recordId, null);
       return result;                                            // auditing never breaks the op (try/catch)
   }
   ```
3. **The recorder** — [`common/audit/AuditRecorderImpl.java`](Backend/common-lib/src/main/java/com/granttrack/common/audit/AuditRecorderImpl.java) saves the row in its **own new transaction**
   (`@Transactional(REQUIRES_NEW)`) so an audit write is independent of the business transaction:
   ```java
   auditLogRepository.save(AuditLog.builder()
       .userId(userId).action(action).entityType(entityType).recordId(recordId)
       .details(details).timestamp(Instant.now()).build());
   ```
4. **The table** — [`common/audit/AuditLog.java`](Backend/common-lib/src/main/java/com/granttrack/common/audit/AuditLog.java) maps `audit_logs` (append-only; deliberately NOT
   soft-deletable). It's in `common-lib` so **every service** writes to the one shared `audit_logs` table.
5. **Reading them** — Compliance/Admin read the trail via the auth-service audit-log API
   (`GET /api/v1/audit-logs`), routed by the gateway.

**Say it as:** *"Any method tagged `@Auditable` is intercepted by `AuditAspect`, which — after the
method succeeds — records who (from the JWT), what action, which entity, and the timestamp into the
shared `audit_logs` table, in a separate transaction so it never affects the business operation."*

### 7.2 ⭐ Exception handling (how errors become clean responses)
All exceptions are translated centrally into the standard envelope by one
`@RestControllerAdvice`:
- [`common/handler/GlobalExceptionHandler.java`](Backend/common-lib/src/main/java/com/granttrack/common/handler/GlobalExceptionHandler.java)

| Exception thrown in code | HTTP status | Meaning |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity id not found |
| `BusinessException` / `OptimisticLockingFailureException` | 409 | Rule violation / concurrent edit |
| `DuplicateResourceException` / `DataIntegrityViolationException` | 409 | Already exists / FK violation |
| `MethodArgumentNotValidException` | 400 | `@Valid` bean-validation failed (returns field errors) |
| `AuthenticationException` / `BadCredentialsException` | 401 | Not authenticated / bad login |
| `AccessDeniedException` | 403 | Authenticated but lacks the role |
| any other `Exception` | 500 | Logged, generic message (no stack trace leaked) |

Each handler builds an `ErrorResponse` (status, error, path, optional field errors) wrapped in
`ApiResponse.error(message, error)`:
```java
private ResponseEntity<ApiResponse<ErrorResponse>> build(HttpStatus status, String message, ...) {
    ErrorResponse error = ErrorResponse.builder().status(status.value())
        .error(status.getReasonPhrase()).path(req.getRequestURI()).fieldErrors(fieldErrors).build();
    return ResponseEntity.status(status).body(ApiResponse.error(message, error));
}
```
Custom exceptions live in [`common/exception/`](Backend/common-lib/src/main/java/com/granttrack/common/exception) (`BusinessException`, `ResourceNotFoundException`,
`DuplicateResourceException`). Business code just `throw new BusinessException("...")` and trusts the
handler to shape the HTTP response. **401/403 that occur inside the security filter** (before a
controller) are handled by [`RestAuthenticationEntryPoint`](Backend/common-lib/src/main/java/com/granttrack/common/security/RestAuthenticationEntryPoint.java) / `RestAccessDeniedHandler`, which
write the same envelope.

**How the frontend uses it:** `errorInterceptor` reads `body.message` (and any `fieldErrors`) and
shows a toast — one consistent error contract from backend to UI.

### 7.3 Other common building blocks (quick but important)
- **`ApiResponse<T>`** — the success/message/data/timestamp envelope every endpoint returns.
- **`PageResponse<T>`** — `{ content, totalElements, ... }` for paginated lists.
- **`BaseEntity`** ([`common/entity/BaseEntity.java`](Backend/common-lib/src/main/java/com/granttrack/common/entity/BaseEntity.java)) — every entity extends it: `id`, `createdAt/updatedAt`,
  `createdBy/updatedBy` (JPA auditing), `@Version` (optimistic locking), `deleted` flag
  (**soft delete** via `@SQLRestriction("deleted = false")`).
- **`SecurityUtils`** ([`common/security/SecurityUtils.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/SecurityUtils.java)) — `getCurrentUserId()` and
  `hasAnyRole(...)`, read from the `SecurityContext` the JWT filter populated.

---

## 8. Database (shared schema)

- One MySQL DB **`granttrack`**; **`core-service` owns all Flyway migrations** (`V1`–`V11` in
  [`core-service/.../resources/db/migration`](Backend/core-service/src/main/resources/db/migration)). Other services `flyway.enabled=false`, `ddl-auto=validate`.
- **Table ownership:** auth → `users, roles, user_roles, refresh_tokens`; common → `audit_logs`
  (shared); notification → `notifications`; finance → `disbursement_milestones, fund_disbursements`;
  core → `sponsors, institutions, funding_schemes, grant_calls, grant_applications, …, grant_awards`.
- **Read-only projections:** finance maps `grant_awards`/`grant_applications` read-only to gate its
  logic; core keeps read-only `User`/`Role` copies so award/review can resolve users. This is the
  standard **shared-database microservices** tradeoff (clean service code, one database).

---

## 9. Likely review questions — crisp answers

- **"Why microservices here, and why only three?"** Auth (identity), Notification (already
  fire-and-forget), Finance (well-bounded) are natural, low-coupling boundaries; the rest is still
  cohesive so it stays a monolith. We used the shared-DB pattern to avoid a risky data split.
- **"How do services find each other?"** Eureka service registry + client-side load balancing
  (`lb://service-name`); no hard-coded hosts.
- **"How is a request authenticated across services?"** A signed JWT with a shared secret; each
  service validates it locally from the claims (stateless, no session, no DB round-trip).
- **"How is a notification targeted to one user?"** The `notifications` row stores a `userId`;
  reads are scoped to the authenticated user's id server-side.
- **"How are audit logs written?"** `@Auditable` + an AOP aspect that records who/what/when into
  `audit_logs` after the method succeeds, in a separate transaction.
- **"How are errors handled?"** One `@RestControllerAdvice` maps each exception type to an HTTP
  status + the standard `ApiResponse` envelope; the Angular error interceptor turns that into a toast.
- **"How does the frontend reach the backend?"** Angular → API gateway (`apiUrl`) → routed to the
  owning service; a JWT interceptor attaches the token and auto-refreshes on 401.

---

## 10. Suggested study order
1. This guide top-to-bottom once. 2. Open each ⭐ file and re-read the highlighted block.
3. [`Backend/README.md`](Backend/README.md) + [`Backend/docs/Microservices-Architecture-Guide.md`](Backend/docs/Microservices-Architecture-Guide.md) for run/architecture.
4. Trace one full path live: log in → open "Open Calls" → watch the network tab hit the gateway.
