import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ProgressService } from '../progress.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { ProgressReportResponse } from '../../../core/models/progress.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { AwardService } from '../../awards/awards.service';
import { Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { ProgressTabsComponent } from '../progress-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { SearchableSelectComponent, SelectOption } from '../../../shared/components/searchable-select/searchable-select.component';

@Component({
  selector: 'gt-progress-reports',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    ModalComponent,
    HasRoleDirective,
    ProgressTabsComponent,
    DropdownMenuComponent,
    IconComponent,
    SearchableSelectComponent
  ],
  templateUrl: './progress-reports.component.html',
})
export class ProgressReportsComponent implements OnInit {
  private api = inject(ProgressService);
  private awardApi = inject(AwardService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private route = inject(ActivatedRoute);

  protected readonly Role = Role;

  /** When arriving from a milestone's "Submit Progress Report" action, the report is linked to it. */
  readonly pendingMilestoneId = signal<number | null>(null);

  readonly rows = signal<ProgressReportResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardOptions = computed<SelectOption[]>(() => this.awards().map(a => ({ value: a.id, label: `Award #${a.id} (App ${a.applicationId})` })));
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });
  readonly awardFilter = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Progress Report' : 'New Progress Report'));
  readonly reportFile = signal<File | null>(null);

  readonly reviewModalOpen = signal(false);
  readonly reviewTarget = signal<ProgressReportResponse | null>(null);

  /** When editing a returned report, surface the reviewer's comment so the PI knows what to fix. */
  readonly editingReviewComment = computed(() => {
    const id = this.editingId();
    const r = this.rows().find((x) => x.id === id);
    return r && r.status === 'REVISION_REQUESTED' ? r.reviewComment : null;
  });

  readonly columns: ColumnDef[] = [
    { key: 'awardId', header: 'Award', type: 'number' },
    { key: 'period', header: 'Period' },
    { key: 'budgetUtilisationPercent', header: 'Utilisation', type: 'progress' },
    { key: 'submittedDate', header: 'Submitted', type: 'date' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['DRAFT', 'SUBMITTED', 'APPROVED', 'REVISION_REQUESTED'].map((s) => ({
        value: s,
        label: s.replace('_', ' '),
      })),
    },
  ];

  readonly form = this.fb.nonNullable.group({
    awardId: [null as number | null, [Validators.required]],
    period: [''],
    summary: [''],
    keyAchievements: [''],
    challenges: [''],
    budgetUtilisationPercent: [null as number | null, [Validators.min(0), Validators.max(100)]],
  });

  readonly reviewForm = this.fb.nonNullable.group({
    comment: [''],
  });

  ngOnInit(): void {
    this.awardApi.list({ statuses: 'ACTIVE', size: 100 }).subscribe(r => this.awards.set(r.data.content));
    this.load();
    // Deep-link from a milestone's "Submit Progress Report" action: open a create form pre-linked
    // to that milestone (awardId + milestoneId), and scope the list to that award.
    this.route.queryParamMap.subscribe((params) => {
      const awardId = params.get('awardId');
      const milestoneId = params.get('milestoneId');
      const isNew = params.get('new');
      if (awardId) {
        this.onAwardFilter(Number(awardId));
      }
      if (isNew && awardId) {
        this.openCreate();
        this.pendingMilestoneId.set(milestoneId ? Number(milestoneId) : null);
        this.form.patchValue({ awardId: Number(awardId) });
      }
    });
  }

  load(): void {
    this.loading.set(true);
    this.api.listReports(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content);
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onAwardFilter(value: any): void {
    const id = value === null || value === '' ? null : Number(value);
    this.awardFilter.set(id);
    this.query.update((q) => {
      const next: PageQuery = { ...q, page: 0 };
      if (id === null || Number.isNaN(id)) { delete next['awardId']; } else { next['awardId'] = id; }
      return next;
    });
    this.load();
  }

  onFilter(params: Record<string, string>): void {
    this.query.update((q) => {
      const next: PageQuery = { page: 0, size: q.size, sort: q.sort, ...params };
      const id = this.awardFilter();
      if (id !== null && !Number.isNaN(id)) next['awardId'] = id;
      return next;
    });
    this.load();
  }
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  openCreate(): void {
    this.editingId.set(null);
    this.pendingMilestoneId.set(null);
    this.reportFile.set(null);
    this.form.reset({ awardId: null, budgetUtilisationPercent: null });
    this.modalOpen.set(true);
  }

  /** Edit a DRAFT or REVISION_REQUESTED report (the researcher can rework a returned report). */
  openEdit(r: ProgressReportResponse): void {
    this.editingId.set(r.id);
    this.reportFile.set(null);
    this.form.reset({
      awardId: r.awardId,
      period: r.period ?? '',
      summary: r.summary ?? '',
      keyAchievements: r.keyAchievements ?? '',
      challenges: r.challenges ?? '',
      budgetUtilisationPercent: r.budgetUtilisationPercent ?? null,
    });
    this.modalOpen.set(true);
  }

  onReportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.reportFile.set(input.files && input.files.length ? input.files[0] : null);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const id = this.editingId();
    const body = {
      awardId: v.awardId!,
      // Attach the milestone link only when creating a milestone-linked report.
      milestoneId: id ? undefined : (this.pendingMilestoneId() ?? undefined),
      period: v.period || undefined,
      summary: v.summary || undefined,
      keyAchievements: v.keyAchievements || undefined,
      challenges: v.challenges || undefined,
      budgetUtilisationPercent: v.budgetUtilisationPercent ?? undefined,
    };
    this.saving.set(true);
    const request$ = id ? this.api.updateReport(id, body) : this.api.createReport(body);
    request$.subscribe({
      next: (res) => {
        const reportId = res.data.id;
        const file = this.reportFile();
        if (file) {
          this.api.uploadReportDocument(reportId, file).subscribe({
            next: () => this.finishSave(id != null),
            error: () => this.saving.set(false),
          });
        } else {
          this.finishSave(id != null);
        }
      },
      error: () => this.saving.set(false),
    });
  }

  private finishSave(edited: boolean): void {
    this.toast.success(edited
      ? 'Progress report updated.'
      : (this.pendingMilestoneId()
          ? 'Progress report created for the milestone. Submit it to send for review.'
          : 'Progress report created.'));
    this.pendingMilestoneId.set(null);
    this.modalOpen.set(false);
    this.saving.set(false);
    this.load();
  }

  submit(r: ProgressReportResponse): void {
    this.api.submitReport(r.id).subscribe(() => {
      this.toast.success('Report submitted for review.');
      this.load();
    });
  }

  openReview(r: ProgressReportResponse): void {
    this.reviewTarget.set(r);
    this.reviewForm.reset({ comment: '' });
    this.reviewModalOpen.set(true);
  }

  review(r: ProgressReportResponse, decision: 'APPROVE' | 'REQUEST_REVISION'): void {
    const comment = this.reviewForm.getRawValue().comment;
    if (decision === 'REQUEST_REVISION' && !comment.trim()) {
      this.toast.error('Please add a comment so the researcher knows what to revise.');
      return;
    }
    this.api.reviewReport(r.id, decision, comment).subscribe(() => {
      this.toast.success(decision === 'APPROVE' ? 'Report approved.' : 'Revision requested.');
      this.reviewModalOpen.set(false);
      this.load();
    });
  }

  downloadReport(r: ProgressReportResponse): void {
    this.api.downloadReportDocument(r.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = r.reportDocName || `progress-report-${r.id}`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
