import { Routes } from '@angular/router';

export const PROGRESS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  {
    path: 'overview',
    loadComponent: () =>
      import('./overview/progress-overview.component').then((m) => m.ProgressOverviewComponent),
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./reports/progress-reports.component').then((m) => m.ProgressReportsComponent),
  },
  {
    path: 'deliverables',
    loadComponent: () =>
      import('./deliverables/deliverables.component').then((m) => m.DeliverablesComponent),
  },
];
