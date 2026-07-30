import { Routes } from '@angular/router';

export const OUTPUT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'publications' },
  {
    path: 'publications',
    loadComponent: () =>
      import('./publications/research-outputs.component').then((m) => m.ResearchOutputsComponent),
  },
  {
    path: 'ip',
    loadComponent: () =>
      import('./ip/ip-records.component').then((m) => m.IpRecordsComponent),
  },
];
