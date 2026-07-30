import { Routes } from '@angular/router';

export const REVIEW_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'queue' },
  {
    path: 'queue',
    loadComponent: () =>
      import('./queue/reviewer-queue.component').then((m) => m.ReviewerQueueComponent),
  },
  {
    path: 'assignments',
    loadComponent: () =>
      import('./panel/assignment-panel.component').then((m) => m.AssignmentPanelComponent),
  },
  {
    path: 'assignments/:id',
    loadComponent: () =>
      import('./scoring/review-scoring.component').then((m) => m.ReviewScoringComponent),
  },
];
