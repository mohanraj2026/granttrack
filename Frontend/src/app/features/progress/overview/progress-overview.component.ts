import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { ProgressService } from '../progress.service';
import { AwardService } from '../../awards/awards.service';
import { DisbursementService } from '../../disbursements/disbursements.service';
import { ProgressReportResponse, DeliverableResponse } from '../../../core/models/progress.model';
import { MilestoneResponse } from '../../../core/models/disbursement.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { humanize } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent, IconName } from '../../../shared/components/icon/icon.component';
import { RingProgressComponent } from '../../../shared/components/ring-progress/ring-progress.component';
import { DonutChartComponent, DonutSegment } from '../../../shared/components/donut-chart/donut-chart.component';
import { RevealDirective } from '../../../shared/directives/reveal.directive';
import { ProgressTabsComponent } from '../progress-tabs.component';

const DELIVERABLE_COLORS: Record<string, string> = {
  PENDING: '#94a3b8', SUBMITTED: '#2563eb', ACCEPTED: '#15803d', REJECTED: '#b91c1c',
};
const REPORT_COLORS: Record<string, string> = {
  DRAFT: '#94a3b8', SUBMITTED: '#2563eb', APPROVED: '#15803d', REVISION_REQUESTED: '#b45309',
};
const MILESTONE_COLORS: Record<string, string> = {
  UPCOMING: '#94a3b8', EVIDENCE_SUBMITTED: '#2563eb', APPROVED: '#0ea5a3', DISBURSED: '#15803d', OVERDUE: '#b91c1c',
};

interface StatTile { icon: IconName; label: string; value: string; }

@Component({
  selector: 'gt-progress-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, PageHeaderComponent, SpinnerComponent, EmptyStateComponent, IconComponent,
    RingProgressComponent, DonutChartComponent, RevealDirective, ProgressTabsComponent,
  ],
  templateUrl: './progress-overview.component.html',
})
export class ProgressOverviewComponent implements OnInit {
  private progress = inject(ProgressService);
  private awardApi = inject(AwardService);
  private disbursement = inject(DisbursementService);

  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly loaded = signal(false);

  readonly reports = signal<ProgressReportResponse[]>([]);
  readonly deliverables = signal<DeliverableResponse[]>([]);
  readonly milestones = signal<MilestoneResponse[]>([]);

  readonly humanize = humanize;
  readonly milestoneColor = (s: string) => MILESTONE_COLORS[s] ?? '#94a3b8';

  /** Most recent report's budget utilisation. */
  readonly budgetUtil = computed(() => {
    const sorted = [...this.reports()].sort((a, b) =>
      (b.submittedDate ?? b.createdAt ?? '').localeCompare(a.submittedDate ?? a.createdAt ?? ''));
    return sorted.find((r) => r.budgetUtilisationPercent != null)?.budgetUtilisationPercent ?? 0;
  });

  readonly deliverableSegments = computed<DonutSegment[]>(() =>
    this.bucketize(this.deliverables().map((d) => d.status), DELIVERABLE_COLORS));
  readonly reportSegments = computed<DonutSegment[]>(() =>
    this.bucketize(this.reports().map((r) => r.status), REPORT_COLORS));

  readonly sortedMilestones = computed(() =>
    [...this.milestones()].sort((a, b) => a.milestoneNumber - b.milestoneNumber));

  readonly milestoneTotal = computed(() => this.milestones().reduce((s, m) => s + Number(m.amount), 0));
  readonly milestoneDisbursed = computed(() =>
    this.milestones().filter((m) => m.status === 'DISBURSED').reduce((s, m) => s + Number(m.amount), 0));
  readonly pctDisbursed = computed(() => {
    const total = this.milestoneTotal();
    return total > 0 ? (this.milestoneDisbursed() / total) * 100 : 0;
  });

  readonly stats = computed<StatTile[]>(() => {
    const accepted = this.deliverables().filter((d) => d.status === 'ACCEPTED').length;
    const disbursed = this.milestones().filter((m) => m.status === 'DISBURSED').length;
    return [
      { icon: 'file-text', label: 'Progress reports', value: String(this.reports().length) },
      { icon: 'book', label: 'Deliverables accepted', value: `${accepted}/${this.deliverables().length}` },
      { icon: 'landmark', label: 'Milestones disbursed', value: `${disbursed}/${this.milestones().length}` },
      { icon: 'wallet', label: 'Funds released', value: this.fmtMoney(this.milestoneDisbursed()) },
    ];
  });

  ngOnInit(): void {
    this.awardApi.list({ size: 100, sort: 'createdAt,desc' }).subscribe((r) => {
      this.awards.set(r.data.content);
      if (r.data.content.length) {
        this.awardId.set(r.data.content[0].id);
        this.load();
      }
    });
  }

  onAwardChange(value: string): void {
    this.awardId.set(value ? Number(value) : null);
    if (this.awardId()) this.load();
  }

  load(): void {
    const id = this.awardId();
    if (!id) return;
    this.loading.set(true);
    forkJoin({
      reports: this.progress.listReports({ awardId: id, size: 100 }),
      deliverables: this.progress.listDeliverables({ awardId: id, size: 100 }),
      milestones: this.disbursement.listMilestones({ awardId: id, size: 100 }),
    }).subscribe({
      next: ({ reports, deliverables, milestones }) => {
        this.reports.set(reports.data.content);
        this.deliverables.set(deliverables.data.content);
        this.milestones.set(milestones.data.content);
        this.loading.set(false);
        this.loaded.set(true);
      },
      error: () => this.loading.set(false),
    });
  }

  private bucketize(values: string[], colors: Record<string, string>): DonutSegment[] {
    const counts = new Map<string, number>();
    for (const v of values) counts.set(v, (counts.get(v) ?? 0) + 1);
    return Object.keys(colors)
      .filter((k) => counts.has(k))
      .map((k) => ({ label: humanize(k), value: counts.get(k)!, color: colors[k] }));
  }

  fmtMoney(v: number): string {
    return v.toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
  }
}
