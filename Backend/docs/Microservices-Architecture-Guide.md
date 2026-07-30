# GrantTrack — Microservices Architecture Guide

A clear, complete guide to how the GrantTrack backend is structured: which parts are
**microservices**, which part stays **monolithic**, how they talk to each other, how to
run the whole thing, and what every backend module does.

---

## 1. The big picture

GrantTrack started as a **modular monolith** (one Spring Boot app, one package per feature).
Three bounded contexts were carved out into their own independently-deployable
**microservices**; everything else stays together in one **core** service. A **service
registry** and an **API gateway** sit in front.

All services share **one MySQL database** (`granttrack`). This is the *shared-database*
microservices pattern — clean service boundaries and independent deployables, without the
cost/complexity of splitting the data.

```
                          ┌──────────────────┐
   Angular app (4200) ───►│   api-gateway    │  :8080   ← the ONLY backend URL the UI calls
                          └────────┬─────────┘
                                   │  (routes by URL path, load-balanced via Eureka)
        ┌─────────────────┬────────┴────────┬──────────────────┐
        ▼                 ▼                 ▼                  ▼
  auth-service     notification-service  finance-service    core-service
     :8081              :8082               :8083              :8084
   (microservice)     (microservice)      (microservice)     (MONOLITH: the rest)
        │                 ▲                 │                  │
        │                 │  Feign (publish notifications)     │
        │                 └───────────────┬───────────────────┘
        └──────────────────┬──────────────┴──────────────┐
                           ▼                              ▼
                   shared MySQL `granttrack`      Eureka registry :8761
                                                  (every service registers here)
```

---

## 2. What is a microservice here, and what is monolithic?

| # | Component | Type | Port | Responsibility |
|---|---|---|---|---|
| 1 | **eureka-server** | Infrastructure | 8761 | Service registry — every service registers and discovers the others here |
| 2 | **api-gateway** | Infrastructure | 8080 | Single entry point; routes each request to the owning service; central CORS |
| 3 | **auth-service** | **Microservice** | 8081 | Identity & access: login, JWT issuance, users, roles, audit-log read API |
| 4 | **notification-service** | **Microservice** | 8082 | In-app notifications + an internal publish API for other services |
| 5 | **finance-service** | **Microservice** | 8083 | Disbursements: milestones, evidence review, fund releases |
| 6 | **core-service** | **Monolith** | 8084 | Everything else: applications, funding, review, award, progress, outputs |
| 7 | **common-lib** | Shared library | — | Reused code (DTOs, exceptions, base entity, audit, JWT-validation security) |

**The three microservices** are `auth-service`, `notification-service`, `finance-service`.
**The monolith** is `core-service` — it still contains the majority of the domain.
`eureka-server` and `api-gateway` are supporting infrastructure; `common-lib` is a plain jar
(not a running service) that the others depend on.

### Why these three were chosen
- **Auth** — the classic identity service; a natural, self-contained boundary.
- **Notification** — already fire-and-forget (`notify(userId, message, category)`), so it has
  clean, one-directional coupling — an ideal extraction.
- **Finance/Disbursement** — a well-bounded context (milestones + fund releases) with clear
  ownership of its own tables.

---

## 3. How the services communicate

| Concern | Mechanism |
|---|---|
| **Discovery** | Netflix **Eureka** — services register at `:8761`; the gateway and Feign clients resolve each other by service name (`lb://auth-service`), never by hard-coded host/port. |
| **Routing** | **Spring Cloud Gateway** maps URL paths to services (see routes below). The browser only ever calls the gateway. |
| **Authentication** | **JWT** issued by auth-service. The token carries `userId`, `email`, `roles`. Resource services validate the signature **from the claims** — no per-request user lookup. All services share one HMAC secret. |
| **Service-to-service calls** | **OpenFeign** over Eureka. Today: `core-service` and `finance-service` publish notifications to `notification-service`. These calls are **best-effort** — a notification outage never breaks the business action. Internal endpoints (`/internal/**`) use a shared token, not user JWTs, and are not exposed by the gateway. |
| **Data** | One shared MySQL schema. `core-service` **owns all Flyway migrations** and is the only one that changes the schema; the others run `flyway.enabled=false` + `ddl-auto=validate` and map only the tables they use (including read-only projections of tables they only read). |

### Gateway routes
| URL path | → Service |
|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/audit-logs/**` | auth-service |
| `/api/v1/notifications/**` | notification-service |
| `/api/v1/disbursements/**` | finance-service |
| everything else (`/api/v1/**`) + Swagger | core-service |

---

## 4. Every backend module explained

### Infrastructure & shared

**`eureka-server`** — A standalone Netflix Eureka registry (`@EnableEurekaServer`). It holds the
live list of service instances so the gateway and Feign clients can find them by name. No
business logic, no database.

**`api-gateway`** — Spring Cloud Gateway (reactive). Defines the path→service routes above,
applies global CORS for the Angular origin, and de-duplicates CORS headers. It is the single
public surface of the backend.

**`common-lib`** — A shared jar (not a running app) that every service depends on. Contains:
- `common.dto` — the standard API response envelope (`ApiResponse`, `PageResponse`, `ErrorResponse`)
- `common.entity.BaseEntity` — id, audit timestamps, optimistic-lock version, soft-delete flag
- `common.exception` + `common.handler` — business/duplicate/not-found exceptions and the global handler
- `common.audit` — the `@Auditable` aspect, the `AuditLog` entity/repository, and the recorder
  (so **every** service writes to the one shared `audit_logs` table)
- `common.security` — `SecurityUtils`, the shared REST 401/403 handlers, and the **claims-based
  JWT validation** used by resource services (`JwtTokenValidator`, `JwtClaimsAuthenticationFilter`,
  `ResourcePrincipal`)

### The microservices

**`auth-service`** (identity & access) — Owns the `users`, `roles`, `refresh_tokens` and
`audit_logs` tables. Responsibilities:
- `auth` module — register, login, refresh-token rotation, change/forgot password, **JWT issuance**
  (`JwtTokenProvider`), the DB-backed security filter (which also rejects tokens of deactivated
  users), and the audit-log **read** API for Compliance/Admin
- `user` module — admin user management (create staff accounts, edit/deactivate users, guard the
  system admin)
- Seeds the default admin account on first start (`DataInitializer`)
- Its own small document storage for registration uploads (college/staff ID, profile photo)

**`notification-service`** (notifications) — Owns the `notifications` table. Responsibilities:
- List / unread-count / mark-read / dismiss the **current user's** notifications
- An **internal publish API** (`/internal/notifications`, shared-token authenticated) that other
  services call to create a notification for a user

**`finance-service`** (disbursements) — Owns the `disbursement_milestones` and `fund_disbursements`
tables. Responsibilities:
- Create/update milestones against an **ACTIVE** award (respecting the award amount cap)
- Researcher submits milestone **evidence** (note + document); finance approves or returns it
- **Release funds** for an approved milestone and record the disbursement
- Reads `grant_awards` / `grant_applications` **read-only** from the shared DB (to check award
  status and resolve the owning researcher), and publishes notifications via Feign

### The monolith — `core-service`

Everything not extracted. Layered per module (`controller → service → service.impl → mapper →
repository → entity`, with `dto.request`/`dto.response`):

| Module | What it does |
|---|---|
| `funding` | Sponsors, institutions, funding schemes, and **grant calls** (the calls researchers apply to) |
| `application` | Grant applications: the application wizard, co-investigators, budgets, abstract uploads, submission |
| `review` | Peer review: reviewer assignment, blind scoring, panel decisions, assigning a finance officer |
| `award` | **Grant awards** issued from a successful application; award terms; the finance-officer accept/reject of an award |
| `progress` | Project execution: periodic **progress reports** and **deliverables** (submit → Compliance approves/rejects) |
| `output` | Research **outputs** (publications) and **IP records** produced under an award |

Core also keeps **read-only copies** of the `User`/`Role` entities (so award/review can resolve
users via the shared DB) and a Feign-backed `NotificationService` (so its code calls
`notify(...)` exactly as before, but the call now goes to notification-service).

> Note: the finance officer **accepts/rejects the award itself** in `core-service`'s `award`
> module, while the downstream **disbursement** work (milestones, releases) lives in
> `finance-service`. The two are linked by the award id.

---

## 5. How to run the backend

### Prerequisites
- **JDK 21** and **Maven 3.9+**
- **MySQL 8** running on `localhost:3306` (the `granttrack` database is auto-created on first connect)

### Build everything
```bash
cd Backend
mvn clean install      # builds common-lib + all services and runs the tests
```

### Start the services (order matters on a fresh database)
Start the registry first, then **core-service** (it runs the Flyway migrations and seeds the six
roles), then the three microservices, then the gateway last.

**Windows (one command):**
```powershell
cd Backend
./run-all.ps1          # opens each service in its own window, in the right order
```

**Manual (any OS), one terminal each:**
```bash
mvn -pl eureka-server        spring-boot:run    # 1) registry        :8761
mvn -pl core-service         spring-boot:run    # 2) migrates the DB  :8084
mvn -pl auth-service         spring-boot:run    # 3) identity         :8081
mvn -pl notification-service spring-boot:run    # 4) notifications    :8082
mvn -pl finance-service      spring-boot:run    # 5) finance          :8083
mvn -pl api-gateway          spring-boot:run    # 6) gateway          :8080
```

### Verify it's up
1. Open the **Eureka dashboard** at `http://localhost:8761` — you should see
   `API-GATEWAY`, `AUTH-SERVICE`, `NOTIFICATION-SERVICE`, `FINANCE-SERVICE`, `CORE-SERVICE`.
2. Log in **through the gateway** (the default admin is seeded on first start):
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@granttrack.local","password":"Admin@12345"}'
```
3. Start the frontend (`cd Frontend && npm start`) — it is already pointed at the gateway
   (`http://localhost:8080/api/v1`).

### Swagger (per service)
`http://localhost:8081/swagger-ui.html` (auth), `:8082` (notification), `:8083` (finance),
`:8084` (core).

---

## 6. Configuration & shared secrets

Local development uses the `local` profile (active by default) with well-known dev values baked
into each service's `application-local.yml`. For any non-local run, provide these via environment
variables — and note the two that **must be identical across services**:

| Variable | Purpose | Must match across services? |
|---|---|---|
| `GRANTTRACK_JWT_SECRET` | Base64 256-bit JWT signing key | **Yes** — all services verify the same token |
| `GRANTTRACK_INTERNAL_TOKEN` | Shared token for `/internal/**` service-to-service calls | **Yes** — core/finance ↔ notification |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL credentials | Yes (same DB) |
| `EUREKA_URL` | Registry location (default `http://localhost:8761/eureka/`) | Yes |
| `GRANTTRACK_BOOTSTRAP_ADMIN_PASSWORD` | First-run admin password (auth-service only) | n/a |

---

## 7. Quick reference

- **Microservices:** auth-service (8081), notification-service (8082), finance-service (8083)
- **Monolith:** core-service (8084)
- **Infrastructure:** eureka-server (8761), api-gateway (8080); shared lib: common-lib
- **Frontend talks to:** the gateway only — `http://localhost:8080`
- **Database:** one shared MySQL schema `granttrack`; core-service owns the migrations
- **Auth:** JWT (claims-based validation), one shared secret
- **Inter-service calls:** OpenFeign over Eureka (core/finance → notification, best-effort)

See also: [`../README.md`](../README.md) (build/run cheat-sheet) and the module design in
[`Software-Design-Document.md`](Software-Design-Document.md).
