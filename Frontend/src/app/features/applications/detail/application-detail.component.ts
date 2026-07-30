import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApplicationsService } from '../applications.service';
import { ReviewService } from '../../reviews/reviews.service';
import { PanelDecisionResponse } from '../../../core/models/review.model';
import {
  ApplicationBudgetResponse,
  CoInvestigatorResponse,
  GrantApplicationResponse,
} from '../../../core/models/application.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'gt-application-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatusBadgeComponent, SpinnerComponent, IconComponent],
  template: `
    <gt-page-header [title]="app()?.projectTitle || 'Application'" subtitle="Application detail">
      <a routerLink="/applications" class="btn btn-outline-secondary d-inline-flex align-items-center gap-2"><gt-icon name="chevron-left" [size]="16" /> Back to portal</a>
    </gt-page-header>

    @if (loading()) { <gt-spinner label="Loading…" /> }
    @if (!loading() && app(); as a) {
      <div class="row g-3 g-xl-4">
        <!-- Proposal -->
        <div class="col-lg-7">
          <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
              <div class="d-flex align-items-center justify-content-between mb-3">
                <h2 class="h5 fw-bold text-dark mb-0">Proposal</h2>
                <gt-status-badge [status]="a.status" />
              </div>

              @if (auth.hasAnyRole(['ROLE_ADMIN', 'ROLE_GRANT_ADMIN'])) {
                <div class="border rounded-3 p-3 mb-3 bg-body-tertiary">
                  <div class="fw-semibold text-dark small mb-2">Administrative actions</div>
                  <div class="d-flex flex-wrap gap-2">
                    @if (a.status === 'SUBMITTED') {
                      <button class="btn btn-sm btn-primary" (click)="markUnderReview(a.id)">Mark as under review</button>
                    }
                    @if (['SUBMITTED', 'UNDER_REVIEW'].includes(a.status)) {
                      <a [routerLink]="['/reviews/panel', a.id]" class="btn btn-sm btn-outline-secondary">Review assignment panel</a>
                    }
                  </div>
                </div>
              }

              <dl class="kv mb-0">
                <dt>Discipline</dt><dd>{{ a.discipline || '—' }}</dd>
                <dt>Requested</dt><dd class="money">{{ a.requestedAmount | currency: 'INR' : 'symbol' : '1.0-0' }}</dd>
                <dt>Duration</dt><dd>{{ a.projectDurationMonths || '—' }} months</dd>
                <dt>Submitted</dt><dd>{{ a.submissionDate ? (a.submissionDate | date: 'medium') : 'Not submitted' }}</dd>
                <dt>Abstract doc</dt>
                <dd>
                  @if (a.abstractDocName) {
                    <button class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1" (click)="download(a.id, a.abstractDocName)" [disabled]="downloading()">
                      <gt-icon name="file-text" [size]="14" /> {{ downloading() ? 'Downloading…' : a.abstractDocName }}
                    </button>
                  } @else { — }
                </dd>
              </dl>

              <h3 class="h6 fw-bold text-dark mt-4 mb-2">Abstract</h3>
              <p class="text-secondary mb-0">{{ a.researchAbstract || 'No abstract provided.' }}</p>
            </div>
          </div>
        </div>

        <!-- Panel decision (visible to the researcher once recorded) -->
        @if (panelDecision(); as pd) {
          <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4"
                 [class.border-start]="true" [class.border-4]="true"
                 [class.border-success]="isAward(pd.awardDecision)" [class.border-secondary]="!isAward(pd.awardDecision)">
              <div class="card-body p-4">
                <div class="d-flex align-items-center gap-2 mb-3">
                  <span class="d-flex align-items-center justify-content-center rounded-3"
                        [class.bg-success]="isAward(pd.awardDecision)" [class.bg-secondary]="!isAward(pd.awardDecision)"
                        style="width:38px;height:38px;color:#fff;"><gt-icon name="scale" [size]="18" /></span>
                  <div>
                    <h2 class="h6 fw-bold text-dark mb-0">Panel decision</h2>
                    <div class="text-secondary small">The review panel's outcome for your application.</div>
                  </div>
                  <span class="badge rounded-pill ms-auto"
                        [class.bg-success-subtle]="isAward(pd.awardDecision)" [class.text-success]="isAward(pd.awardDecision)"
                        [class.bg-secondary-subtle]="!isAward(pd.awardDecision)" [class.text-secondary]="!isAward(pd.awardDecision)">
                    {{ pd.awardDecision }}
                  </span>
                </div>
                <dl class="kv mb-0">
                  @if (isAward(pd.awardDecision)) {
                    <dt>Awarded amount</dt><dd class="money text-success">{{ pd.awardedAmount ? (pd.awardedAmount | currency: 'INR' : 'symbol' : '1.0-0') : '—' }}</dd>
                  }
                  <dt>Panel date</dt><dd>{{ pd.panelDate ? (pd.panelDate | date: 'mediumDate') : '—' }}</dd>
                  <dt>Consensus score</dt><dd>{{ pd.consensusScore ?? '—' }}<span class="text-secondary"> / 10</span></dd>
                  @if (pd.conditionsAttached) { <dt>Conditions</dt><dd>{{ pd.conditionsAttached }}</dd> }
                </dl>
              </div>
            </div>
          </div>
        }

        <!-- Team & budget -->
        <div class="col-lg-5">
          <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
              <h2 class="h6 fw-bold text-dark mb-3">Team</h2>
              @for (c of cois(); track c.id) {
                <div class="d-flex align-items-center justify-content-between gap-3 py-2 border-bottom">
                  <span class="fw-semibold text-dark">{{ c.role }}</span>
                  <span class="text-secondary small flex-grow-1 text-truncate">{{ c.contribution || '—' }}</span>
                  <gt-status-badge [status]="c.status" />
                </div>
              } @empty { <p class="text-secondary mb-0">No co-investigators.</p> }

              <h2 class="h6 fw-bold text-dark mt-4 mb-3">Budget</h2>
              @for (b of budgets(); track b.id) {
                <div class="d-flex align-items-center justify-content-between gap-3 py-2 border-bottom">
                  <span class="text-dark">{{ b.budgetHead }}</span>
                  <span class="money">{{ b.amount | currency: 'INR' : 'symbol' : '1.0-0' }}</span>
                </div>
              } @empty { <p class="text-secondary mb-0">No budget lines.</p> }
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class ApplicationDetailComponent implements OnInit {
  private api = inject(ApplicationsService);
  private reviewApi = inject(ReviewService);
  private route = inject(ActivatedRoute);
  protected auth = inject(AuthService);
  private toast = inject(ToastService);

  readonly app = signal<GrantApplicationResponse | null>(null);
  readonly cois = signal<CoInvestigatorResponse[]>([]);
  readonly budgets = signal<ApplicationBudgetResponse[]>([]);
  readonly panelDecision = signal<PanelDecisionResponse | null>(null);
  readonly loading = signal(true);
  readonly downloading = signal(false);

  isAward(d?: string): boolean {
    return d === 'FULL_AWARD' || d === 'REDUCED_AWARD';
  }

  download(id: number, name?: string): void {
    this.downloading.set(true);
    this.api.downloadAbstract(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name ?? 'abstract';
        a.click();
        URL.revokeObjectURL(url);
        this.downloading.set(false);
      },
      error: () => this.downloading.set(false),
    });
  }

  markUnderReview(id: number): void {
    if (!confirm('Mark application as Under Review?')) return;
    this.api.changeStatus(id, 'UNDER_REVIEW').subscribe((res) => {
      this.toast.success('Status updated to Under Review.');
      this.app.set(res.data);
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      app: this.api.get(id),
      cois: this.api.listCoInvestigators(id),
      budgets: this.api.listBudgets(id),
      // The panel decision may not exist yet (404) — tolerate it.
      panel: this.reviewApi.getPanelDecision(id).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ app, cois, budgets, panel }) => {
        this.app.set(app.data);
        this.cois.set(cois.data);
        this.budgets.set(budgets.data);
        this.panelDecision.set(panel?.data ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
