# GrantTrack — Auth Module Deep-Dive (Interview Study Guide)

Everything you need to explain **authentication & authorization** end-to-end: the login form,
how the request travels to the backend, how credentials are validated, how the JWT is generated,
how every later request carries and validates that token, how authorization is enforced, how the
frontend/backend and microservices communicate, and how exceptions are handled — all with **exact
file locations** and the **real code**.

> Auth lives in three places:
> 1. **Frontend** — `Frontend/src/app` (login form, token storage, interceptors, guards).
> 2. **auth-service** microservice — `Backend/auth-service` (login, JWT issuance, users/roles).
> 3. **common-lib** shared jar — `Backend/common-lib` (the JWT-validation used by *every* service).

---

## 0. The 10-second summary (say this first in the interview)

> "A user submits email + password on the Angular login page. The request goes through the **API
> gateway** to the **auth-service**, which checks the password (BCrypt) via Spring Security and, on
> success, issues a **JWT access token** (signed with a shared secret, containing the user id and
> roles) plus a **refresh token**. The frontend stores them and an **HTTP interceptor** attaches the
> JWT to every subsequent request. Each service validates that token **statelessly from its claims**
> (no session, no DB call) and enforces **role-based access** with `@PreAuthorize`. When the token
> expires, the interceptor silently refreshes it."

---

## 1. The Login form (frontend)

### Files
| File | Job |
|---|---|
| [`features/auth/login/login.component.html`](Frontend/src/app/features/auth/login/login.component.html) | The login page markup (brand panel + form) |
| [`features/auth/login/login.component.ts`](Frontend/src/app/features/auth/login/login.component.ts) | Form state, validation, submit handler |
| [`core/services/auth.service.ts`](Frontend/src/app/core/services/auth.service.ts) | The HTTP calls to the backend auth API |
| [`core/services/token-storage.service.ts`](Frontend/src/app/core/services/token-storage.service.ts) | Persists tokens + user in `localStorage` |

### How it's built
It's an Angular **standalone component** using a **reactive form** (`FormBuilder`) with two
controls, both validated on the client before anything is sent:

```ts
// login.component.ts
readonly form = this.fb.nonNullable.group({
  email: ['', [Validators.required, Validators.email]],
  password: ['', [Validators.required]],
});

submit(): void {
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }   // client-side guard
  this.submitting.set(true);
  this.auth.login(this.form.getRawValue()).subscribe({
    next: (res) => {
      this.toast.success(`Welcome back, ${res.data.user.name.split(' ')[0]}!`);
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
      this.router.navigateByUrl(returnUrl);        // go where they were headed, else dashboard
    },
    error: () => this.submitting.set(false),       // errors are toasted by the error interceptor
  });
}
```

**Talking point:** the component only handles UI concerns (validation, loading state, redirect).
The actual HTTP call lives in `AuthService` — separation of concerns.

---

## 2. From the form to the backend — the login request

### The frontend call
```ts
// core/services/auth.service.ts
private base = `${environment.apiUrl}/auth`;     // e.g. http://localhost:8080/api/v1/auth

login(payload: LoginRequest): Observable<ApiResponse<AuthResponse>> {
  return this.http
    .post<ApiResponse<AuthResponse>>(`${this.base}/login`, payload)
    .pipe(tap((res) => this.handleAuth(res.data)));   // store tokens + user on success
}

private handleAuth(auth: AuthResponse): void {
  this.storage.setTokens(auth.accessToken, auth.refreshToken);  // -> localStorage
  this.storage.setUser(auth.user);
  this.currentUser.set(auth.user);                              // signal drives the UI
}
```
- `environment.apiUrl` ([`environments/environment.development.ts`](Frontend/src/environments/environment.development.ts)) points at the **API gateway**, never a service directly.

### The journey of the request
```
Browser  →  POST /api/v1/auth/login  →  API Gateway (:8080)  →  auth-service (:8081)
```
- **Gateway routing** — [`api-gateway/src/main/resources/application.yml`](Backend/api-gateway/src/main/resources/application.yml):
  ```yaml
  routes:
    - id: auth-service
      uri: lb://auth-service                       # resolved via Eureka (load-balanced)
      predicates: [ Path=/api/v1/auth/**,/api/v1/users/**,/api/v1/audit-logs/** ]
  ```
  The gateway matches the `/api/v1/auth/**` path and forwards to whatever host/port `auth-service`
  registered in Eureka under. (`/auth/login` is public — no token needed yet.)

---

## 3. Inside auth-service — validating the credentials

Request order inside the service: **Controller → Service → Spring Security AuthenticationManager
→ CustomUserDetailsService → password check → JWT issuance.**

### Files and their exact jobs
| File | Job in the login flow |
|---|---|
| [`auth/controller/AuthController.java`](Backend/auth-service/src/main/java/com/granttrack/auth/controller/AuthController.java) | REST endpoint `POST /api/v1/auth/login`; delegates to the service |
| [`auth/service/impl/AuthServiceImpl.java`](Backend/auth-service/src/main/java/com/granttrack/auth/service/impl/AuthServiceImpl.java) | Orchestrates authentication + token issuance |
| [`auth/security/CustomUserDetailsService.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/CustomUserDetailsService.java) | Loads the user from the DB by email |
| [`auth/security/CustomUserDetails.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/CustomUserDetails.java) | Adapts the `User` entity to Spring Security's principal |
| [`auth/security/SecurityConfig.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/SecurityConfig.java) | Declares the `AuthenticationManager`, `PasswordEncoder` (BCrypt), public paths |
| [`auth/repository/UserRepository.java`](Backend/auth-service/src/main/java/com/granttrack/auth/repository/UserRepository.java) | `findByEmail` against the `users` table |
| [`auth/entity/User.java`](Backend/auth-service/src/main/java/com/granttrack/auth/entity/User.java) / `Role.java` | The user + roles model |

### Step 3a — the controller
```java
// AuthController.java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
}
```
`@Valid` triggers bean validation on `LoginRequest`; the body is auto-deserialized from JSON.

### Step 3b — the service authenticates
```java
// AuthServiceImpl.java
public AuthResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
    return issueTokens(principal);
}
```
`authenticationManager.authenticate(...)` is the key line. Spring Security:
1. Calls **`CustomUserDetailsService.loadUserByUsername(email)`**:
   ```java
   return userRepository.findByEmail(email).map(CustomUserDetails::new)
       .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
   ```
2. Compares the submitted password against the stored **BCrypt** hash using the `PasswordEncoder`
   bean (defined in `SecurityConfig`). If it doesn't match → `BadCredentialsException` → **401**.
3. On success returns an authenticated `Authentication` whose principal is our `CustomUserDetails`
   (which exposes the user's id, email and role authorities).

**Talking point — how authentication vs authorization differ here:**
- **Authentication** = "who are you?" → the password check above.
- **Authorization** = "what may you do?" → the roles carried in the token, enforced later by
  `@PreAuthorize` (§7).

---

## 4. Token generation — where and how

```java
// AuthServiceImpl.java  (issueTokens)
private AuthResponse issueTokens(CustomUserDetails principal) {
    Set<String> roleNames = principal.getRoleNames();
    String accessToken  = tokenProvider.generateAccessToken(principal.getId(), principal.getEmail(), roleNames);
    String refreshTokenValue = UUID.randomUUID().toString() + UUID.randomUUID();   // opaque, random
    refreshTokenRepository.save(RefreshToken.builder()
        .userId(principal.getId()).token(refreshTokenValue)
        .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
        .revoked(false).build());
    ...
    return AuthResponse.builder()
        .accessToken(accessToken).refreshToken(refreshTokenValue)
        .tokenType("Bearer").expiresInMs(jwtProperties.getAccessTokenExpirationMs())
        .user(userMapper.toResponse(user)).build();
}
```

### The JWT itself — [`auth/security/JwtTokenProvider.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtTokenProvider.java)
```java
public String generateAccessToken(Long userId, String email, Set<String> roles) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + properties.getAccessTokenExpirationMs());  // 15 min
    return Jwts.builder()
        .issuer(properties.getIssuer())        // "granttrack"
        .subject(String.valueOf(userId))       // WHO  → the user id
        .claim("email", email)
        .claim("roles", roles)                 // WHAT → e.g. ["ROLE_RESEARCHER"]
        .issuedAt(now).expiration(expiry)
        .signWith(key())                       // HMAC-SHA signature using the shared secret
        .compact();
}
```

**Two tokens, two purposes (classic interview question):**
| Token | Lifetime | Stored where | Purpose |
|---|---|---|---|
| **Access token (JWT)** | 15 min | client only (localStorage) | Sent on every request; carries identity + roles; stateless |
| **Refresh token** | 7 days | **DB** (`refresh_tokens`) + client | Exchanged for a new access token; can be **revoked** server-side |

- The secret + expirations are config: [`auth/security/JwtProperties.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtProperties.java) binding `granttrack.security.jwt.*`.
- Refresh tokens live in the DB precisely so logout / password-change can **revoke** them — a JWT
  alone can't be revoked before it expires.

---

## 5. The frontend stores the token & attaches it to every request

### Storage — [`core/services/token-storage.service.ts`](Frontend/src/app/core/services/token-storage.service.ts)
```ts
setTokens(accessToken, refreshToken) {
  localStorage.setItem('gt.accessToken', accessToken);
  localStorage.setItem('gt.refreshToken', refreshToken);
}
```

### Attaching the JWT automatically — [`core/interceptors/auth.interceptor.ts`](Frontend/src/app/core/interceptors/auth.interceptor.ts)
Registered globally in [`app.config.ts`](Frontend/src/app/app.config.ts) via
`provideHttpClient(withInterceptors([errorInterceptor, authInterceptor]))`.

```ts
const AUTH_BYPASS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = storage.accessToken;
  const isBypass = AUTH_BYPASS.some((u) => req.url.includes(u));
  const authReq = token && !isBypass
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })   // <-- attaches the JWT
    : req;
  return next(authReq).pipe(
    catchError((err) => (err.status === 401 && !isBypass)
      ? handle401(authReq, next, auth, storage, router)   // silent refresh + replay (see §8)
      : throwError(() => err)),
  );
};
```
**Talking point:** you never manually add the header anywhere — the interceptor does it for *every*
outgoing request, except the public auth endpoints.

---

## 6. Validating the token on every subsequent request

This is the heart of the microservices auth design: **the token is validated locally by each
service from its claims — no session and no call back to auth-service.** All services share the
same signing secret, so any of them can verify a token auth-service signed.

### For resource services (core, notification, finance) — claims only, no DB
| File (in `common-lib`, shared) | Job |
|---|---|
| [`common/security/JwtTokenValidator.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/JwtTokenValidator.java) | Verifies signature + issuer; extracts userId/email/roles |
| [`common/security/JwtClaimsAuthenticationFilter.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/JwtClaimsAuthenticationFilter.java) | Runs per request; builds the security context from claims |
| [`common/security/ResourcePrincipal.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/ResourcePrincipal.java) | Lightweight principal (id + email), no DB row |
| [`common/security/SecurityUtils.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/SecurityUtils.java) | `getCurrentUserId()` / `hasAnyRole()` read from the context |

```java
// JwtClaimsAuthenticationFilter.java  (per-request)
if (token != null && tokenValidator.isValid(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
    Long userId = tokenValidator.getUserId(token);
    String email = tokenValidator.getEmail(token);
    var authorities = tokenValidator.getRoles(token).stream()  // ["ROLE_X"] -> GrantedAuthority
        .map(SimpleGrantedAuthority::new).toList();
    var auth = new UsernamePasswordAuthenticationToken(new ResourcePrincipal(userId, email), null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);   // request is now "authenticated"
}
```
```java
// JwtTokenValidator.java
private Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey)   // same secret auth-service signed with
        .requireIssuer(issuer).build().parseSignedClaims(token).getPayload();
}
```

### For auth-service's own protected endpoints — DB-backed
auth-service uses [`auth/security/JwtAuthenticationFilter.java`](Backend/auth-service/src/main/java/com/granttrack/auth/security/JwtAuthenticationFilter.java), which additionally loads the user
and checks `isEnabled()` — so a token belonging to a just-deactivated account is rejected before
its 15-minute expiry.

**Talking point (why claims-based):** it makes services **stateless and independent** — no shared
session store, no chatter back to auth-service on every call. The trade-off is that a token stays
valid until it expires; we accept that because access tokens are short-lived (15 min) and refresh
tokens are revocable.

---

## 7. Authorization — who can do what

Two layers:

### 7a. URL-level (coarse) — in each service's security config
```java
// auth-service SecurityConfig.java  (resource services have an equivalent ResourceSecurityConfig)
.authorizeHttpRequests(auth -> auth
    .requestMatchers(PUBLIC_PATHS).permitAll()          // login, register, refresh, swagger, health
    .anyRequest().authenticated())                       // everything else needs a valid token
.sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))   // no HTTP session
```

### 7b. Method-level RBAC — `@EnableMethodSecurity` + `@PreAuthorize`
```java
// example from another module's controller
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")     // only these roles may call this
public ResponseEntity<...> create(...) { ... }
```
- `hasAnyRole('ADMIN')` matches the authority `ROLE_ADMIN` — that's why the JWT stores role names
  as `ROLE_*`. If the caller lacks the role → `AccessDeniedException` → **403**.
- The 6 roles: `ROLE_RESEARCHER, ROLE_REVIEWER, ROLE_GRANT_ADMIN, ROLE_FINANCE_OFFICER,
  ROLE_COMPLIANCE_OFFICER, ROLE_ADMIN` ([`auth/entity/RoleName.java`](Backend/auth-service/src/main/java/com/granttrack/auth/entity/RoleName.java)).

### 7c. Frontend authorization (UX only — the backend is the real gate)
| File | Job |
|---|---|
| [`core/guards/auth.guard.ts`](Frontend/src/app/core/guards/auth.guard.ts) | Blocks routes if not logged in; redirects to `/login?returnUrl=…` |
| [`core/guards/role.guard.ts`](Frontend/src/app/core/guards/role.guard.ts) | Allows a route only if the user holds `route.data.roles` |
```ts
// auth.guard.ts
if (auth.isAuthenticated()) return true;
return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
```
**Talking point:** guards only improve UX (hide pages). Security is still enforced server-side —
never trust the client.

---

## 8. Token refresh — staying logged in without re-login

Access tokens expire in 15 min. When a request comes back **401**, the auth interceptor refreshes
transparently and replays the original request — the user never notices.

```ts
// auth.interceptor.ts  (handle401, simplified)
return auth.refresh().pipe(                          // POST /api/v1/auth/refresh
  switchMap((res) => next(addToken(req, res.data.accessToken))),  // replay with NEW token
  catchError((err) => { auth.clearSession(); router.navigate(['/login']); return throwError(() => err); }),
);
```
Concurrent 401s share **one** refresh via a `BehaviorSubject` (so we don't fire N refresh calls).

### Backend refresh — [`AuthServiceImpl.refresh()`](Backend/auth-service/src/main/java/com/granttrack/auth/service/impl/AuthServiceImpl.java)
```java
RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
    .orElseThrow(() -> new BusinessException("Invalid refresh token"));
if (!stored.isActive()) throw new BusinessException("Refresh token expired or revoked");
// rotate: revoke the used one, issue a fresh pair
stored.setRevoked(true); refreshTokenRepository.save(stored);
return issueTokens(new CustomUserDetails(user));
```
**Talking point — token rotation:** each refresh **revokes the old refresh token** and issues a new
one, limiting the damage if a refresh token leaks.

---

## 9. How frontend & backend communicate (where the "glue" lives)

| Concern | Where |
|---|---|
| Base URL → gateway | [`environments/environment*.ts`](Frontend/src/environments/environment.development.ts) (`apiUrl`) |
| Attach JWT | [`auth.interceptor.ts`](Frontend/src/app/core/interceptors/auth.interceptor.ts) |
| Turn errors into toasts | [`error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts) |
| Standard response shape | [`common/dto/ApiResponse.java`](Backend/common-lib/src/main/java/com/granttrack/common/dto/ApiResponse.java) |
| CORS (allow the Angular origin) | gateway `application.yml` `globalcors` |

Every response uses one envelope so the frontend always reads `res.data`:
```json
{ "success": true, "message": "Login successful", "data": { "accessToken": "...", "user": {...} }, "timestamp": "..." }
```

**Request lifecycle in one line:**
```
Component → AuthService(HttpClient) → authInterceptor adds JWT → Gateway → auth-service
→ Security filter validates → controller → service → repo → MySQL → ApiResponse → errorInterceptor → UI
```

---

## 10. How the microservices communicate

| Mechanism | Detail |
|---|---|
| **Discovery** | **Eureka** registry (`:8761`); services register and find each other by name (`lb://auth-service`) — no hard-coded hosts. auth-service is a Eureka client (`@EnableDiscoveryClient` on [`AuthServiceApplication.java`](Backend/auth-service/src/main/java/com/granttrack/AuthServiceApplication.java)). |
| **Routing** | **Spring Cloud Gateway** is the single entry point; the browser only calls it. |
| **Cross-service auth** | The **shared JWT secret** — auth-service *signs*, every other service *verifies* locally. No auth call-outs. |
| **Service-to-service calls** | **OpenFeign** (used elsewhere, e.g. core → notification). Auth isn't called synchronously by others thanks to the stateless-token design. |
| **Shared DB** | One MySQL schema; auth-service owns `users`, `roles`, `refresh_tokens`. |

**Talking point (why auth was a clean microservice):** authentication is a self-contained bounded
context, and because it hands out a self-verifying token, no other service needs to call it at
request time — ideal for extraction.

---

## 11. Exception handling — frontend and backend

### Backend — one central handler
[`common/handler/GlobalExceptionHandler.java`](Backend/common-lib/src/main/java/com/granttrack/common/handler/GlobalExceptionHandler.java) (`@RestControllerAdvice`) maps each exception type to an HTTP
status + the standard envelope:

| Exception | HTTP | Auth example |
|---|---|---|
| `BadCredentialsException` / `AuthenticationException` | **401** | wrong password on login |
| `AccessDeniedException` | **403** | valid token but missing role |
| `BusinessException` | 409 | "Invalid refresh token" |
| `ResourceNotFoundException` | 404 | user id not found |
| `MethodArgumentNotValidException` | 400 | `@Valid` failed (bad email format) |

Security-filter failures (before a controller runs) are handled by
[`common/security/RestAuthenticationEntryPoint.java`](Backend/common-lib/src/main/java/com/granttrack/common/security/RestAuthenticationEntryPoint.java) (401) and `RestAccessDeniedHandler.java` (403),
which write the **same** JSON envelope so the frontend handles them uniformly.

### Frontend — one interceptor
[`error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts) reads `body.message` and shows a toast:
```ts
if (error.status === 0) return 'Cannot reach the server. Is the backend running?';   // network down
// otherwise show the backend's message, e.g. "Authentication failed: Bad credentials"
```
It stays silent on the `/auth/refresh` 401 (that path is handled by the auth interceptor's refresh
logic, not surfaced as an error).

**Talking point:** one backend contract (`ApiResponse` + status codes) → one frontend handler. A
wrong password surfaces as a clean "Authentication failed" toast, not a stack trace.

---

## 12. End-to-end walkthrough (rehearse this out loud)

1. **User types email/password** → `login.component.ts` validates the form client-side.
2. **`AuthService.login()`** POSTs `/api/v1/auth/login` to the **gateway**, which routes to
   **auth-service**.
3. **`AuthController.login()`** → **`AuthServiceImpl.login()`** → `AuthenticationManager` →
   **`CustomUserDetailsService`** loads the user → **BCrypt** password check.
4. On success, **`JwtTokenProvider`** signs a **JWT** (userId + roles) and a **refresh token** is
   saved to the DB. Both are returned in an **`ApiResponse`**.
5. The frontend stores them (`TokenStorageService`) and redirects to the dashboard.
6. For **every later request**, **`authInterceptor`** attaches `Authorization: Bearer <jwt>`.
7. The gateway routes it; the target service's **`JwtClaimsAuthenticationFilter`** verifies the
   signature and rebuilds the security context **from the claims** — no DB, no session.
8. **`@PreAuthorize`** checks the caller's role for that endpoint (authorization).
9. When the access token **expires (401)**, `authInterceptor` calls **`/auth/refresh`**, which
   **rotates** the refresh token and returns a new pair; the original request is **replayed**.
10. Any failure is mapped by **`GlobalExceptionHandler`** to a status + envelope and shown by the
    frontend **`errorInterceptor`** as a toast.

---

## 13. Likely interview questions (crisp answers)

- **"Where is the token generated?"** In auth-service, `JwtTokenProvider.generateAccessToken()`,
  called from `AuthServiceImpl.issueTokens()` after a successful password check.
- **"How is the password stored/checked?"** BCrypt-hashed in the `users` table; checked by Spring
  Security's `AuthenticationManager` via `CustomUserDetailsService` + the `PasswordEncoder` bean.
- **"How does every request carry the token?"** The Angular `authInterceptor` clones each outgoing
  request and adds `Authorization: Bearer <jwt>` (except public auth endpoints).
- **"How is it validated without calling auth-service?"** Every service shares the signing secret;
  `JwtTokenValidator` verifies the signature + issuer locally and the filter builds the security
  context from the claims — stateless.
- **"Authentication vs authorization here?"** Authentication = the login password check; authorization
  = role authorities in the JWT enforced by `@PreAuthorize`.
- **"Why both a JWT and a refresh token?"** The JWT is short-lived and stateless (can't be revoked);
  the refresh token is DB-stored and revocable, enabling logout / rotation.
- **"What happens on logout / password change?"** All refresh tokens for the user are revoked
  (`refreshTokenRepository.revokeAllForUser`), so no new access tokens can be minted.
- **"How do you stop a deactivated user mid-session?"** auth-service's DB-backed filter rejects the
  token if `isEnabled()` is false, and refresh is refused for a non-ACTIVE account.

---

### File cheat-sheet (print this)
- **FE:** `login.component.ts/html`, `core/services/auth.service.ts`, `token-storage.service.ts`,
  `core/interceptors/auth.interceptor.ts` + `error.interceptor.ts`, `core/guards/auth.guard.ts` + `role.guard.ts`
- **auth-service:** `auth/controller/AuthController.java`, `auth/service/impl/AuthServiceImpl.java`,
  `auth/security/{JwtTokenProvider,CustomUserDetailsService,CustomUserDetails,JwtAuthenticationFilter,SecurityConfig,JwtProperties}.java`
- **common-lib (shared):** `common/security/{JwtTokenValidator,JwtClaimsAuthenticationFilter,ResourcePrincipal,SecurityUtils,RestAuthenticationEntryPoint,RestAccessDeniedHandler}.java`,
  `common/handler/GlobalExceptionHandler.java`, `common/dto/ApiResponse.java`
