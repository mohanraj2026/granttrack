# GrantTrack — Software Design Document (SDD)

**Project:** GrantTrack — Research Grant & Academic Output Management System
**Domain:** Education & Learning
**Document type:** Backend Software Design Document
**Architecture:** Modular Monolith (Spring Boot 3 / Java 21)
**Author/Role:** Senior Solution Architect & Enterprise Java Architect
**Status:** Phase 1 — Baseline Design

---

## 1. Introduction

### 1.1 Purpose
This document specifies the backend software design for **GrantTrack**, an enterprise system that manages the full research-grant lifecycle: funding scheme configuration, grant calls, application submission, blind peer review, award decisions, milestone-based disbursement, project progress tracking, research output / IP recording, and in-app notifications.

It is the authoritative engineering reference for the Phase-1 backend and is written to be directly implementable on **Java 21 + Spring Boot 3 + MySQL 8**.

### 1.2 Scope (Phase 1)
In scope: REST API backend, RBAC security with JWT, all 9 functional modules, relational persistence, audit logging, soft delete, and OpenAPI documentation.

Out of scope (deferred), per requirements §9:
- Integration with national research registries, ERP finance systems, and open-access repositories.
- Email / SMS delivery — notifications are **in-app only**.
- Automatic DOI metadata enrichment (CrossRef/Scopus).
- Physical file storage backends — `file_path` columns store a path/key only.

### 1.3 Actors → Roles
| Actor | System role |
|---|---|
| Principal Investigator | `ROLE_RESEARCHER` |
| Peer Reviewer | `ROLE_REVIEWER` |
| Grant Administrator | `ROLE_GRANT_ADMIN` |
| Research Finance Officer | `ROLE_FINANCE_OFFICER` |
| Compliance Officer | `ROLE_COMPLIANCE_OFFICER` |
| Research Admin | `ROLE_ADMIN` |

---

## 2. Technology Stack

| Concern | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3.x |
| Security | Spring Security 6 + JWT (jjwt) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | MySQL 8 |
| Migrations | Flyway |
| Mapping | MapStruct + Lombok |
| Validation | Jakarta Bean Validation |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Logging | SLF4J + Logback |

---

## 3. Architecture Overview

### 3.1 Style — Modular Monolith
A single deployable artifact partitioned into **feature modules** with strict layering. Each module owns its entities, persistence, business logic and API. Cross-module calls go through **service interfaces only** (never repository-to-repository across modules). This keeps the option open to extract a module into a microservice later with minimal churn.

```
                ┌─────────────────────────────────────────────┐
   HTTP/JSON →  │  Controller (REST, @Valid, RBAC @PreAuthorize)│
                ├─────────────────────────────────────────────┤
                │  Service (interface)  →  ServiceImpl (logic)  │
                ├─────────────────────────────────────────────┤
                │  Mapper (MapStruct: Entity ⇄ DTO)             │
                ├─────────────────────────────────────────────┤
                │  Repository (Spring Data JPA, Specifications) │
                ├─────────────────────────────────────────────┤
   MySQL    ←   │  Entity (JPA, extends BaseEntity)             │
                └─────────────────────────────────────────────┘
   Cross-cutting: SecurityFilterChain · GlobalExceptionHandler ·
                  JPA Auditing · AuditLog AOP · OpenAPI · Logging
```

### 3.2 Package Structure
```
com.granttrack
├── GrantTrackApplication.java
├── common
│   ├── entity        (BaseEntity / Auditable @MappedSuperclass)
│   ├── dto           (ApiResponse, PageResponse, ErrorResponse, SearchCriteria)
│   ├── exception     (BusinessException, ResourceNotFoundException, ...)
│   ├── handler       (GlobalExceptionHandler @RestControllerAdvice)
│   ├── audit         (JPA AuditorAware, @Auditable AOP, AuditLog writer)
│   ├── config        (OpenApiConfig, JpaAuditingConfig, AppConfig)
│   └── util          (constants, enums shared)
├── auth              (User, Role, RefreshToken, AuditLog) + security
│   ├── controller / service / service.impl / repository
│   ├── entity / dto.request / dto.response / mapper / exception
│   └── security      (JwtTokenProvider, JwtAuthFilter, SecurityConfig,
│                      CustomUserDetailsService, CustomUserDetails)
├── user
├── funding           (FundingScheme, GrantCall, Sponsor, Institution)
├── application       (GrantApplication, CoInvestigator, ApplicationBudget)
├── review            (ReviewerAssignment, ReviewScore, PanelDecision)
├── award             (GrantAward)
├── disbursement      (DisbursementMilestone, FundDisbursement)
├── progress          (ProgressReport, Deliverable)
├── output            (ResearchOutput, IPRecord)
└── notification      (Notification)
```
Each functional module contains the standard sub-packages: `controller`, `service`, `service.impl`, `repository`, `entity`, `dto.request`, `dto.response`, `mapper`, `exception`.

### 3.3 Separation of Concerns
- **Controller** — transport only: bind & validate request DTOs, enforce `@PreAuthorize` RBAC, delegate to service, wrap result in `ApiResponse`. No business logic.
- **Service** — interface defining the use cases.
- **ServiceImpl** — transactional business logic, state-machine enforcement, orchestration, cross-module calls via other service interfaces.
- **Mapper** — entity ⇄ DTO conversion (MapStruct). Blind-review DTO redaction lives here.
- **Repository** — data access (JPA + JPA Specifications for dynamic search).
- **Entity** — persistence model, never returned over the wire.

---

## 4. Cross-Cutting Design

### 4.1 Standard API Response
All endpoints return a uniform envelope:
```json
{ "success": true, "message": "Operation Successful", "data": { }, "timestamp": "2026-06-09T10:20:00Z" }
```
Paginated endpoints place a `PageResponse<T>` in `data` (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`).

### 4.2 Global Exception Handling
`@RestControllerAdvice` maps exceptions to HTTP + the standard envelope:

| Exception | HTTP | Notes |
|---|---|---|
| `ResourceNotFoundException` | 404 | entity not found |
| `BusinessException` | 409 | invalid state transition / rule violation |
| `DuplicateResourceException` | 409 | unique constraint |
| `MethodArgumentNotValidException` | 400 | field errors list |
| `AccessDeniedException` | 403 | RBAC denial |
| `AuthenticationException` / bad JWT | 401 | |
| `Exception` (fallback) | 500 | logged with correlation id |

### 4.3 Auditing & Soft Delete
- **`BaseEntity` (`@MappedSuperclass`)** supplies `id`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`, `version`.
- JPA auditing populates `created_*`/`updated_*` via `AuditorAware` (resolves current user id from `SecurityContext`).
- `@Version` provides optimistic locking.
- Soft delete via `deleted` flag + Hibernate `@SQLRestriction("deleted = false")` on every entity, so reads transparently exclude soft-deleted rows.
- **Domain `AuditLog`** (separate from JPA auditing) records security/decision-sensitive actions (award decisions, disbursements, review submissions) via an `@Auditable` AOP aspect → persisted to `audit_logs`.

### 4.4 Pagination, Sorting, Search
- Controllers accept Spring `Pageable` (`?page=&size=&sort=field,asc`).
- Dynamic search via **JPA Specifications** (e.g. `GET /grant-applications?status=SUBMITTED&discipline=CS&q=genomics`). A generic `SearchCriteria`/`SpecificationBuilder` powers filterable list endpoints.

### 4.5 API Versioning
URI versioning: all endpoints under `/api/v1/**`. Future breaking changes go to `/api/v2`.

### 4.6 Logging
SLF4J; structured logs with a per-request correlation id (MDC filter). INFO for business events, WARN for handled rule violations, ERROR for 5xx with stack traces. No secrets/PII or JWTs logged.

---

## 5. Security Design

### 5.1 Authentication (JWT)
- **Access token** (short-lived, ~15 min) — stateless, carries `sub` (user id), `email`, `roles`.
- **Refresh token** (long-lived, ~7 days) — persisted in `refresh_tokens` (rotatable, revocable).
- `POST /auth/login` → access + refresh. `POST /auth/refresh` → new access (rotates refresh). `POST /auth/logout` → revokes refresh token.
- Passwords hashed with **BCrypt** (strength 10+).

### 5.2 Filter Chain
```
Request → JwtAuthenticationFilter (validate token, load CustomUserDetails,
          set SecurityContext) → Authorization (URL rules + @PreAuthorize)
          → Controller
```
- `CustomUserDetailsService` loads user + roles from DB.
- `JwtAuthenticationFilter extends OncePerRequestFilter`.
- `SecurityConfig` defines the `SecurityFilterChain`, stateless session, public allowlist (`/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`), and an `AuthenticationEntryPoint`/`AccessDeniedHandler` returning the standard envelope.

### 5.3 RBAC (method-level)
`@EnableMethodSecurity` + `@PreAuthorize` on controller methods. Authority strings match role names (`ROLE_*`). High-level matrix:

| Capability | Roles |
|---|---|
| Configure schemes / calls | `ROLE_ADMIN`, `ROLE_GRANT_ADMIN` |
| Submit/withdraw applications | `ROLE_RESEARCHER` |
| Assign reviewers, panel decision | `ROLE_GRANT_ADMIN` |
| Submit review scores | `ROLE_REVIEWER` |
| Create/approve award | `ROLE_GRANT_ADMIN` |
| Approve milestone / release funds | `ROLE_FINANCE_OFFICER`, `ROLE_GRANT_ADMIN` |
| Review progress / deliverables | `ROLE_COMPLIANCE_OFFICER` |
| Record outputs / IP | `ROLE_RESEARCHER` |
| User administration | `ROLE_ADMIN` |

### 5.4 Blind Review Isolation (NFR §8)
Reviewer-facing application DTOs **omit PI identity** (`principalInvestigatorId`, PI name, institution) for `DOUBLE_BLIND` calls. The mapper produces a redacted `ReviewerApplicationView`; ownership checks ensure reviewers only see assigned applications.

---

## 6. Data Model

### 6.1 Conventions
- Money `DECIMAL(15,2)`; percent `DECIMAL(5,2)`; enums `VARCHAR` via `@Enumerated(STRING)` (UPPER_SNAKE_CASE).
- Calendar dates `DATE`; timestamps `DATETIME(6)`.
- FKs `ON DELETE RESTRICT` (soft delete is the norm). Every FK and every filterable `status`/`category` column is indexed.
- All `@ManyToOne`/`@OneToOne` are `LAZY`.

### 6.2 ER Diagram (textual)
```
sponsors            1 ── N  funding_schemes
funding_schemes     1 ── N  grant_calls
grant_calls         1 ── N  grant_applications
institutions        1 ── N  users
institutions        1 ── N  grant_applications
users               M ── N  roles                 (user_roles)
users               1 ── N  refresh_tokens
users               1 ── N  notifications
grant_applications  1 ── N  co_investigators
grant_applications  1 ── N  application_budgets
grant_applications  1 ── N  reviewer_assignments
grant_applications  1 ── 1  panel_decisions
grant_applications  1 ── 1  grant_awards
reviewer_assignments 1 ── N review_scores
grant_awards        1 ── N  disbursement_milestones
disbursement_milestones 1 ── N fund_disbursements
grant_awards        1 ── N  progress_reports
grant_awards        1 ── N  deliverables
grant_awards        1 ── N  research_outputs
grant_awards        1 ── N  ip_records
```

### 6.3 Tables
Full column-level definitions, enums, constraints and indexes are specified in **`docs/GrantTrack_Database_Structure.md`** (the database structure specification) and realised in the Flyway migration `db/migration/V1__init_schema.sql`. Summary of 21 tables:

`institutions, sponsors, users, roles, user_roles, refresh_tokens, audit_logs, funding_schemes, grant_calls, grant_applications, co_investigators, application_budgets, reviewer_assignments, review_scores, panel_decisions, grant_awards, disbursement_milestones, fund_disbursements, progress_reports, deliverables, research_outputs, ip_records, notifications`.

### 6.4 Key Constraints
- Unique: `users.email`, `roles.name`, `refresh_tokens.token`, `grant_awards.application_id`, `panel_decisions.application_id`, `reviewer_assignments(application_id, reviewer_id)`, `review_scores(assignment_id, criterion)`, `disbursement_milestones(award_id, milestone_number)`.
- Composite index `grant_calls(open_date, close_date)`; `notifications(user_id, status)`.

---

## 7. Functional Modules & State Machines

### 7.1 Auth (Module 1)
Register, Login, Refresh, Logout, Change Password, Forgot-Password (structure). Entities: User, Role, AuditLog, RefreshToken.

### 7.2 Funding (Module 2)
Scheme CRUD; GrantCall CRUD with **Open/Close** transitions.
`GrantCall.status`: `UPCOMING → OPEN → UNDER_REVIEW → AWARDED → CLOSED`.

### 7.3 Application (Module 3)
Create/draft-save, submit, withdraw, manage co-investigators & budget lines.
`GrantApplication.status`: `DRAFT → SUBMITTED → UNDER_REVIEW → {AWARDED | DECLINED}`; `DRAFT/SUBMITTED → WITHDRAWN`. Submission only allowed while parent call is `OPEN`.

### 7.4 Review (Module 4)
Assign reviewer (with **conflict-of-interest** check), accept/decline, submit per-criterion scores, panel consensus decision.
`ReviewerAssignment.status`: `ASSIGNED → {ACCEPTED → SUBMITTED | DECLINED}`. `PanelDecision` is 1:1 with application and drives award eligibility.

### 7.5 Award (Module 5)
Create award from a `FULL_AWARD`/`REDUCED_AWARD` panel decision, approve/activate, track.
`GrantAward.status`: `ACTIVE → {SUSPENDED ↔ ACTIVE} → {COMPLETED | TERMINATED}`.

### 7.6 Disbursement (Module 6)
Create milestones, submit evidence, approve, release funds.
`DisbursementMilestone.status`: `UPCOMING → EVIDENCE_SUBMITTED → APPROVED → DISBURSED` (`→ OVERDUE` if past due). `FundDisbursement.status`: `PENDING → {RELEASED | FAILED}`. Releasing a disbursement is `@Auditable`.

### 7.7 Progress (Module 7)
Submit progress reports, compliance review, deliverable upload (path/key).
`ProgressReport.status`: `DRAFT → SUBMITTED → {APPROVED | REVISION_REQUESTED}`. `Deliverable.status`: `PENDING → SUBMITTED → {ACCEPTED | REJECTED}`.

### 7.8 Output (Module 8)
Record publications/patents/datasets/software and IP records.
`ResearchOutput.status`: `IN_PREPARATION → SUBMITTED → PUBLISHED`. `IPRecord.status`: `FILED → {GRANTED | ABANDONED}`.

### 7.9 Notification (Module 9)
In-app create (internal), list (paged), mark read, dismiss.
`Notification.status`: `UNREAD → {READ | DISMISSED}`.

---

## 8. REST API Surface (selected)

All under `/api/v1`. Standard envelope on every response.

**Auth**
```
POST   /auth/register            POST /auth/login
POST   /auth/refresh             POST /auth/logout
POST   /auth/change-password     POST /auth/forgot-password
```
**Funding**
```
POST   /funding/schemes          GET  /funding/schemes        GET /funding/schemes/{id}
PUT    /funding/schemes/{id}     DELETE /funding/schemes/{id}
POST   /funding/calls            PUT  /funding/calls/{id}     GET /funding/calls
POST   /funding/calls/{id}/open  POST /funding/calls/{id}/close
```
**Application**
```
POST   /applications             PUT  /applications/{id}      GET /applications
POST   /applications/{id}/submit POST /applications/{id}/withdraw
POST   /applications/{id}/co-investigators
POST   /applications/{id}/budgets
```
**Review**
```
POST   /reviews/assignments      POST /reviews/assignments/{id}/conflict-check
POST   /reviews/assignments/{id}/scores
POST   /reviews/applications/{id}/panel-decision
```
**Award / Disbursement / Progress / Output / Notification** follow the same resource + action pattern (see controllers / Swagger UI).

Full request/response examples are published at runtime via **Swagger UI** (`/swagger-ui.html`) and OpenAPI JSON (`/v3/api-docs`), and summarised in `docs/API-Examples.md`.

---

## 9. Non-Functional Design

| NFR | Approach |
|---|---|
| Performance (10k concurrent) | Stateless JWT, connection pooling (HikariCP), indexed queries, pagination everywhere, lazy fetch |
| Security | RBAC, BCrypt, JWT, blind-review redaction, audit trails, soft delete |
| Scalability | Stateless services → horizontal scale; modular monolith → extractable modules |
| Availability | Stateless app behind LB; DB HA; health endpoints (Actuator) |
| Maintainability | Clean layering, DTO pattern, MapStruct, config-driven enums |
| Observability | Correlation-id logging, Actuator metrics, audit logs |

---

## 10. Deployment

- **Local:** Spring Boot fat jar + local MySQL 8; `spring.profiles.active=local`.
- **Production:** containerised, behind reverse proxy / LB; externalised config & secrets; Flyway migrations on startup; stateless replicas.

---

## 11. Phase-1 Design Decisions (confirmed defaults)
1. `@ManyToOne`/`@OneToOne` default to **LAZY**.
2. Shared `BaseEntity @MappedSuperclass` for audit/soft-delete columns.
3. Enums via `@Enumerated(STRING)`.
4. Blind-review redaction enforced in reviewer DTOs/mappers.
5. `file_path` stores a path/key only; storage backend deferred.
