import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AwardService } from '../awards.service';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-award-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatusBadgeComponent, SpinnerComponent, IconComponent],
  template: `
    <gt-page-header
      [title]="award() ? 'Award #' + award()!.id : 'Award'"
      subtitle="Operational anchor for this grant.">
      <a routerLink="/awards" class="btn btn-outline-secondary d-inline-flex align-items-center gap-2"><gt-icon name="chevron-left" [size]="16" /> Back to awards</a>
    </gt-page-header>

    @if (loading()) { <gt-spinner label="Loading…" /> }
    @if (!loading() && award(); as a) {
      <div class="row g-3 g-xl-4">
        <div class="col-lg-7">
          <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
              <div class="d-flex align-items-center justify-content-between mb-3">
                <h2 class="h5 fw-bold text-dark mb-0">Award terms</h2>
                <gt-status-badge [status]="a.status" />
              </div>
              <dl class="kv mb-0">
                <dt>Application</dt><dd>#{{ a.applicationId }}</dd>
                <dt>Awarded</dt><dd class="money">{{ a.awardedAmount | currency: 'INR' : 'symbol' : '1.0-0' }}</dd>
                <dt>Start</dt><dd>{{ a.startDate ? (a.startDate | date: 'mediumDate') : '—' }}</dd>
                <dt>End</dt><dd>{{ a.endDate ? (a.endDate | date: 'mediumDate') : '—' }}</dd>
                <dt>Award letter</dt><dd>{{ a.awardLetterDate ? (a.awardLetterDate | date: 'mediumDate') : 'Not issued' }}</dd>
                <dt>Conditions</dt><dd>{{ a.conditionsRef || '—' }}</dd>
              </dl>
            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
              <h2 class="h6 fw-bold text-dark mb-1">Related areas</h2>
              <p class="text-secondary small mb-3">Records anchored to this award.</p>
              <div class="d-grid gap-2">
                <a class="btn btn-outline-secondary d-flex align-items-center justify-content-between" [routerLink]="['/disbursements']" [queryParams]="{ awardId: a.id }"><span class="d-inline-flex align-items-center gap-2"><gt-icon name="landmark" [size]="16" /> Disbursements</span> <gt-icon name="chevron-right" [size]="16" /></a>
                <a class="btn btn-outline-secondary d-flex align-items-center justify-content-between" [routerLink]="['/progress/reports']" [queryParams]="{ awardId: a.id }"><span class="d-inline-flex align-items-center gap-2"><gt-icon name="trending-up" [size]="16" /> Progress reports</span> <gt-icon name="chevron-right" [size]="16" /></a>
                <a class="btn btn-outline-secondary d-flex align-items-center justify-content-between" [routerLink]="['/outputs/publications']" [queryParams]="{ awardId: a.id }"><span class="d-inline-flex align-items-center gap-2"><gt-icon name="book" [size]="16" /> Outputs &amp; publications</span> <gt-icon name="chevron-right" [size]="16" /></a>
              </div>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class AwardDetailComponent implements OnInit {
  private api = inject(AwardService);
  private route = inject(ActivatedRoute);

  readonly award = signal<GrantAwardResponse | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.get(id).subscribe({
      next: (r) => {
        this.award.set(r.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
