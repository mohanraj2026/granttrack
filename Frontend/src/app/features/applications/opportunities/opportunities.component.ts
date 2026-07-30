import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { GrantCallResponse, FundingSchemeResponse } from '../../../core/models/funding.model';
import { ApplicationsService } from '../applications.service';
import { AuthService } from '../../../core/services/auth.service';
import { FundingService } from '../../funding/funding.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { RevealDirective } from '../../../shared/directives/reveal.directive';

@Component({
  selector: 'gt-opportunities',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, PageHeaderComponent, SpinnerComponent, EmptyStateComponent,
    StatusBadgeComponent, SearchFilterBarComponent, IconComponent, RevealDirective,
  ],
  templateUrl: './opportunities.component.html',
})
export class OpportunitiesComponent implements OnInit {
  private funding = inject(FundingService);
  private apps = inject(ApplicationsService);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly calls = signal<GrantCallResponse[]>([]);
  readonly schemes = signal<FundingSchemeResponse[]>([]);
  readonly loading = signal(false);
  readonly downloadingId = signal<number | null>(null);
  readonly appliedCallIds = signal<Set<number>>(new Set());
  private q = signal('');
  private sortCategory = signal('');
  
  readonly isAdmin = computed(() => this.auth.hasAnyRole(['ROLE_ADMIN', 'ROLE_GRANT_ADMIN']));

  readonly categories = ['Basic Research', 'Applied Research', 'Translational', 'Development', 'Others'];
  readonly categoryFilter = [{
    key: 'category',
    label: 'Category',
    options: this.categories.map(c => ({ value: c, label: c }))
  }];

  /** research area per scheme id, for display on call cards. */
  readonly areaBySchemeId = computed(() => {
    const m = new Map<number, string>();
    for (const s of this.schemes()) if (s.researchArea) m.set(s.id, s.researchArea);
    return m;
  });

  readonly filtered = computed(() => {
    const term = this.q().toLowerCase();
    const cat = this.sortCategory();
    let res = this.calls();
    if (term) {
      res = res.filter(
        (c) =>
          c.callTitle.toLowerCase().includes(term) ||
          (c.schemeName ?? '').toLowerCase().includes(term) ||
          (this.areaBySchemeId().get(c.schemeId) ?? '').toLowerCase().includes(term),
      );
    }
    if (cat) {
      res = res.filter(c => {
        const s = this.schemes().find(x => x.id === c.schemeId);
        return s?.category === cat;
      });
    }
    return res;
  });

  ngOnInit(): void {
    this.loading.set(true);
    forkJoin({
      calls: this.funding.listCalls({ status: 'OPEN', size: 100, sort: 'closeDate,asc' }),
      schemes: this.funding.listSchemes({ status: 'ACTIVE', size: 200 }),
      apps: this.apps.list({ size: 100 }),
    }).subscribe({
      next: ({ calls, schemes, apps }) => {
        this.calls.set(calls.data.content);
        this.schemes.set(schemes.data.content);
        if (!this.isAdmin()) {
          this.appliedCallIds.set(new Set(apps.data.content.map(a => a.callId)));
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSearch(params: Record<string, string>): void {
    this.q.set(params['q'] ?? '');
    this.sortCategory.set(params['category'] ?? '');
  }

  area(c: GrantCallResponse): string {
    return this.areaBySchemeId().get(c.schemeId) ?? 'General';
  }

  schemeDescription(c: GrantCallResponse): string {
    const s = this.schemes().find(x => x.id === c.schemeId);
    return s?.description ?? 'No description provided.';
  }

  schemeAwardLimit(c: GrantCallResponse): string {
    const s = this.schemes().find(x => x.id === c.schemeId);
    if (!s) return '—';
    if (s.maxAwardAmount >= 500001) return 'Above 5,00,000';
    return `Up to ${s.maxAwardAmount.toLocaleString()}`;
  }

  daysLeft(c: GrantCallResponse): number {
    const close = new Date(c.closeDate).getTime();
    return Math.max(0, Math.ceil((close - Date.now()) / 86_400_000));
  }

  apply(c: GrantCallResponse): void {
    this.router.navigate(['/applications/new'], { queryParams: { callId: c.id } });
  }

  editCall(c: GrantCallResponse): void {
    this.router.navigate(['/funding']); // Routing to funding list where they can edit
  }

  viewApplications(c: GrantCallResponse): void {
    this.router.navigate(['/applications'], { queryParams: { callId: c.id } });
  }

  downloadDoc(c: GrantCallResponse): void {
    if (!c.schemeDocumentPath) return;
    this.downloadingId.set(c.schemeId);
    this.funding.downloadSchemeDocument(c.schemeId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const parts = c.schemeDocumentPath!.split('/');
        a.download = parts[parts.length - 1] || 'scheme_document';
        a.click();
        window.URL.revokeObjectURL(url);
        this.downloadingId.set(null);
      },
      error: () => this.downloadingId.set(null),
    });
  }
}
