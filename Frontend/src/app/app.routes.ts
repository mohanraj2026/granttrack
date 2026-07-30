import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './core/models/enums';

const LIFECYCLE_ROLES = [
  Role.ADMIN,
  Role.GRANT_ADMIN,
  Role.RESEARCHER,
  Role.FINANCE_OFFICER,
  Role.COMPLIANCE_OFFICER,
];

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'staff-login',
    loadComponent: () =>
      import('./features/auth/staff-login/staff-login.component').then((m) => m.StaffLoginComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/auth/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'funding',
        canActivate: [roleGuard],
        data: { roles: [Role.ADMIN, Role.GRANT_ADMIN] },
        loadChildren: () =>
          import('./features/funding/funding.routes').then((m) => m.FUNDING_ROUTES),
      },
      {
        path: 'applications',
        loadChildren: () =>
          import('./features/applications/applications.routes').then(
            (m) => m.APPLICATION_ROUTES,
          ),
      },
      {
        path: 'reviews',
        canActivate: [roleGuard],
        data: { roles: [Role.REVIEWER, Role.GRANT_ADMIN, Role.ADMIN] },
        loadChildren: () =>
          import('./features/reviews/reviews.routes').then((m) => m.REVIEW_ROUTES),
      },
      {
        path: 'awards',
        canActivate: [roleGuard],
        data: { roles: LIFECYCLE_ROLES },
        loadChildren: () => import('./features/awards/awards.routes').then((m) => m.AWARD_ROUTES),
      },
      {
        path: 'disbursements',
        canActivate: [roleGuard],
        data: { roles: LIFECYCLE_ROLES },
        loadChildren: () =>
          import('./features/disbursements/disbursements.routes').then(
            (m) => m.DISBURSEMENT_ROUTES,
          ),
      },
      {
        path: 'progress',
        canActivate: [roleGuard],
        data: { roles: LIFECYCLE_ROLES },
        loadChildren: () =>
          import('./features/progress/progress.routes').then((m) => m.PROGRESS_ROUTES),
      },
      {
        path: 'outputs',
        canActivate: [roleGuard],
        data: { roles: LIFECYCLE_ROLES },
        loadChildren: () =>
          import('./features/outputs/outputs.routes').then((m) => m.OUTPUT_ROUTES),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notification-center.component').then(
            (m) => m.NotificationCenterComponent,
          ),
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: [Role.ADMIN, Role.GRANT_ADMIN] },
        loadComponent: () =>
          import('./features/users/user-admin.component').then((m) => m.UserAdminComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
