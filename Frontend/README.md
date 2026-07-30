# GrantTrack — Frontend

Angular 20 single-page application for the **GrantTrack** Research Grant & Academic Output Management System. It is a role-based, JWT-secured client for the GrantTrack REST backend, covering all nine functional modules.

## Tech stack
Angular 20 (standalone components, signals) · TypeScript (strict) · Reactive Forms · RxJS · SCSS design system · functional HTTP interceptors & route guards. No heavyweight UI dependency — a small custom component kit keeps the bundle lean (~108 kB gzipped initial).

## Architecture
```
src/app/
├── core/                       cross-cutting, app-wide singletons
│   ├── models/                 TS interfaces + enums mirroring the backend DTOs
│   ├── services/               AuthService (signals), TokenStorage, Toast
│   ├── interceptors/           auth (Bearer + refresh-on-401), error (toasts)
│   ├── guards/                 authGuard, roleGuard
│   └── utils/                  toHttpParams
├── shared/                     reusable presentational kit
│   ├── components/             data-table, paginator, search-filter-bar, modal,
│   │                           status-badge, toast-container, spinner, empty-state, page-header
│   └── directives/             *gtHasRole (RBAC UI binding)
├── layout/                     role-aware shell (sidebar + topbar)
└── features/                   one folder per module (lazy-loaded)
    ├── auth/ funding/ applications/ reviews/ awards/
    ├── disbursements/ progress/ outputs/ notifications/ users/ dashboard/
```

### Security & auth cycle
- **JWT**: short-lived access token attached as `Authorization: Bearer …` by `authInterceptor`.
- **Refresh rotation**: on a `401`, the interceptor calls `/auth/refresh` once, queues concurrent requests, replays them with the new token, and signs the user out if refresh fails.
- **Storage**: access + refresh tokens and the current user in `localStorage`; cleared on logout.
- **RBAC**: `roleGuard` protects routes via `data.roles`; `*gtHasRole` hides/show UI per role. Six roles: `ROLE_RESEARCHER, ROLE_REVIEWER, ROLE_GRANT_ADMIN, ROLE_FINANCE_OFFICER, ROLE_COMPLIANCE_OFFICER, ROLE_ADMIN`.

### Data tables
Every list view uses the shared `DataTableComponent` + `PaginatorComponent` + `SearchFilterBarComponent` for **server-side pagination, sorting and dynamic filtering** (Spring `?page=&size=&sort=field,dir` plus per-field filters).

## Module → page map
| Module | Key pages |
|---|---|
| Auth | Login (split-screen), Register, Forgot password, Profile + change password |
| Dashboard | Role-aware quick links + lifecycle overview |
| Funding *(admin)* | Schemes, Grant Calls (open/close), Sponsors, Institutions |
| Applications *(researcher)* | Portal (by status) + 4-step submission wizard + detail |
| Reviews | Reviewer queue, blind scoring card, admin assignment + panel-decision panel |
| Awards | Grants master dashboard + award detail |
| Disbursements *(finance)* | Milestone scheduler / approval queue, fund releases |
| Progress | Progress reports + deliverables (compliance review desk) |
| Outputs *(researcher)* | Research outputs hub + IP records |
| Notifications | Notification center (filter, mark read, dismiss) |
| Users *(admin)* | User administration (search, activate/deactivate) |

## Prerequisites
- Node.js 20 LTS or 22 LTS recommended. (Built & verified here on Node 25 — newer than Angular 20's officially supported range; if `npm install`/`ng serve` complains about the Node version, use an LTS via `nvm`.)
- The **GrantTrack backend** running (default `http://localhost:8088`).

## Configure
API base URL lives in `src/environments/environment.ts` (and `environment.development.ts`):
```ts
apiUrl: 'http://localhost:8088/api/v1'
```
Change it if your backend runs elsewhere. CORS is already permitted by the backend.

## Run
```bash
npm install
npm start          # ng serve → http://localhost:4200
# or
npm run build      # production build → dist/granttrack-frontend
```

## Try it
1. Start the backend, then `npm start`.
2. Log in with the bootstrapped admin: `admin@granttrack.local` / `Admin@12345`, or register a new Researcher/Reviewer.
3. The sidebar adapts to your role; action buttons are gated by `*gtHasRole`.

> Note: the in-app **Register** screen takes an optional numeric Institution ID (the institutions list endpoint is authenticated, so it can't be fetched pre-login). Admins manage institutions under **Funding → Institutions**.
