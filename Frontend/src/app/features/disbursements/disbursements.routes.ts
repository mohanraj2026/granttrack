import { Routes } from '@angular/router';

export const DISBURSEMENT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'milestones' },
  {
    path: 'milestones',
    loadComponent: () =>
      import('./milestones/milestone-scheduler.component').then(
        (m) => m.MilestoneSchedulerComponent,
      ),
  },
  {
    path: 'releases',
    loadComponent: () =>
      import('./releases/fund-releases.component').then((m) => m.FundReleasesComponent),
  },
];
