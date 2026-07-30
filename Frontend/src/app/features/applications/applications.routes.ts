import { Routes } from '@angular/router';

export const APPLICATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./portal/application-portal.component').then((m) => m.ApplicationPortalComponent),
  },
  {
    path: 'opportunities',
    loadComponent: () =>
      import('./opportunities/opportunities.component').then((m) => m.OpportunitiesComponent),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./wizard/application-wizard.component').then((m) => m.ApplicationWizardComponent),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./wizard/application-wizard.component').then((m) => m.ApplicationWizardComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./detail/application-detail.component').then((m) => m.ApplicationDetailComponent),
  },
];
