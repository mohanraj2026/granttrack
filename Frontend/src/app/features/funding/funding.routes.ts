import { Routes } from '@angular/router';

export const FUNDING_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'schemes' },
  {
    path: 'schemes',
    loadComponent: () =>
      import('./schemes/schemes-list.component').then((m) => m.SchemesListComponent),
  },
  {
    path: 'calls',
    loadComponent: () => import('./calls/calls-list.component').then((m) => m.CallsListComponent),
  },
  {
    path: 'sponsors',
    loadComponent: () =>
      import('./sponsors/sponsors-list.component').then((m) => m.SponsorsListComponent),
  },
  {
    path: 'institutions',
    loadComponent: () =>
      import('./institutions/institutions-list.component').then(
        (m) => m.InstitutionsListComponent,
      ),
  },
];
