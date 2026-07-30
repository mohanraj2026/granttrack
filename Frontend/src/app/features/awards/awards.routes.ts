import { Routes } from '@angular/router';

export const AWARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/awards-list.component').then((m) => m.AwardsListComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./detail/award-detail.component').then((m) => m.AwardDetailComponent),
  },
];
