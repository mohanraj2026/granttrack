# GrantTrack Backend (Microservices)

Backend for the **Research Grant & Academic Output Management System**, built on **Java 21 + Spring Boot 3.3 + Spring Cloud 2023.0.3 + MySQL 8**.

Three bounded contexts — **Auth**, **Notification**, **Finance/Disbursement** — run as independent microservices behind a **Spring Cloud Gateway** and a **Netflix Eureka** registry. The remaining modules stay in a single **core** service. All services share one MySQL database (`granttrack`).

## Topology

| Module | Artifact | Port | Role |
|---|---|---|---|
| Service registry | `eureka-server` | **8761** | Netflix Eureka — all services register/discover here |
| API gateway | `api-gateway` | **8080** | Single entry point; the frontend talks **only** to this |
| Identity & access | `auth-service` | **8081** | Login, JWT issuance, users, roles, audit-log read API |
| Notifications | `notification-service` | **8082** | In-app notifications + internal publish API |
| Finance | `finance-service` | **8083** | Disbursement milestones, evidence review, fund releases |
| Core (the rest) | `core-service` | **8084** | applications, funding, review, award, progress, outputs |
| Shared library | `common-lib` | — | DTOs, exceptions, `BaseEntity`, audit, JWT-validation security |

```
                       ┌──────────────┐
   Angular (4200) ───► │ api-gateway  │ :8080
                       └──────┬───────┘
              ┌───────────────┼───────────────┬───────────────┐
              ▼               ▼               ▼               ▼
        auth-service   notification-svc   finance-service  core-service
           :8081           :8082             :8083            :8084
              └───────────────┴───────┬───────┴───────────────┘
                                      ▼
                            shared MySQL `granttrack`
   (core & finance publish notifications → notification-service via OpenFeign)
   (all services register with Eureka :8761)
```

### Gateway routes
| Path | → Service |
|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/audit-logs/**` | auth-service |
| `/api/v1/notifications/**` | notification-service |
| `/api/v1/disbursements/**` | finance-service |
| everything else (`/api/v1/**`) + Swagger | core-service |

## Design notes (shared-database microservices)
- **One database, one migration owner.** `core-service` owns all Flyway migrations (`V1`–`V11`). The other services run `spring.flyway.enabled=false` + `ddl-auto=validate` and map only their own tables (plus read-only projections of tables they need to read, e.g. finance reads `grant_awards`).
- **Stateless auth.** `auth-service` issues JWTs; the access token carries `userId`, `email`, `roles`. Resource services validate the signature and build the principal **from claims** — no per-request user lookup. All services share one HMAC secret.
- **Service-to-service calls** use **OpenFeign** over Eureka load-balancing. Internal endpoints (`/internal/**`) are authenticated by a shared internal token (not user JWTs) and are not routed by the gateway. Notification publishing is **best-effort** — a notification outage never breaks the calling business transaction.

## Prerequisites
- JDK 21, Maven 3.9+
- MySQL 8 running locally (database `granttrack` is auto-created on first connect)

## Configuration (shared across services)
Local defaults live in each service's `application-local.yml` (active by default). Override via environment variables for non-local runs:

| Variable | Purpose |
|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | MySQL credentials (default `root`/`root`) |
| `GRANTTRACK_JWT_SECRET` | Base64 256-bit JWT signing key — **must be identical for every service** |
| `GRANTTRACK_INTERNAL_TOKEN` | Shared service-to-service token — **must match** across core/finance/notification |
| `EUREKA_URL` | Eureka registry URL (default `http://localhost:8761/eureka/`) |
| `GRANTTRACK_BOOTSTRAP_ADMIN_PASSWORD` | First-run admin password (auth-service) |

> The `local` profile ships well-known dev values for the JWT secret (`Y3Jhbnr...`) and internal token (`gt-internal-dev-token`); both are rejected/insecure outside `local`.

## Build
```bash
mvn clean install            # builds common-lib + all services (run tests too)
mvn -pl core-service -am test   # build/test a single service and its deps
```

## Run (start order matters on a fresh DB)
Start the registry first, then **core-service** (it runs the Flyway migrations and seeds roles), then the other services, then the gateway last.

```bash
# 1) registry
mvn -pl eureka-server spring-boot:run
# 2) core — migrates the schema + seeds roles
mvn -pl core-service spring-boot:run
# 3) the three microservices (any order)
mvn -pl auth-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl finance-service spring-boot:run
# 4) gateway
mvn -pl api-gateway spring-boot:run
```
On Windows you can launch them all in separate terminals with [`run-all.ps1`](run-all.ps1).

> auth-service bootstraps the default admin (`admin@granttrack.local` / `Admin@12345`) on first start. Roles are seeded by core-service's Flyway migration `V2`.

## Verify it's up
- Eureka dashboard: `http://localhost:8761` (should list AUTH-SERVICE, NOTIFICATION-SERVICE, FINANCE-SERVICE, CORE-SERVICE, API-GATEWAY)
- Log in through the gateway:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@granttrack.local","password":"Admin@12345"}'
# use the returned accessToken as: Authorization: Bearer <token>
```

## API documentation (Swagger, per service)
- auth-service: `http://localhost:8081/swagger-ui.html`
- notification-service: `http://localhost:8082/swagger-ui.html`
- finance-service: `http://localhost:8083/swagger-ui.html`
- core-service: `http://localhost:8084/swagger-ui.html`

## Security & roles
JWT access tokens (15 min) + persisted, rotatable refresh tokens (7 days). BCrypt hashing (auth-service). Method-level RBAC via `@PreAuthorize`. Roles:
`ROLE_RESEARCHER`, `ROLE_REVIEWER`, `ROLE_GRANT_ADMIN`, `ROLE_FINANCE_OFFICER`, `ROLE_COMPLIANCE_OFFICER`, `ROLE_ADMIN`.

## Standard response envelope
```json
{ "success": true, "message": "Operation Successful", "data": { }, "timestamp": "2026-06-09T10:20:00Z" }
```
