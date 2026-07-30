import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ReviewService } from '../reviews.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { ReviewerAssignmentResponse, ReviewScoreResponse, PanelDecisionResponse } from '../../../core/models/review.model';
import { Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { ReviewsTabsComponent } from '../reviews-tabs.component';
import { ApplicationsService } from '../../applications/applications.service';
import { UserAdminService } from '../../users/users.service';
import { GrantApplicationResponse } from '../../../core/models/application.model';
import { UserResponse } from '../../../core/models/user.model';
import { SearchableSelectComponent, SelectOption } from '../../../shared/components/searchable-select/searchable-select.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-assignment-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    ModalComponent,
    HasRoleDirective,
    ReviewsTabsComponent,
    SearchableSelectComponent,
    IconComponent
  ],
  templateUrl: './assignment-panel.component.html',
})
export class AssignmentPanelComponent implements OnInit {
  private api = inject(ReviewService);
  private appApi = inject(ApplicationsService);
  private userApi = inject(UserAdminService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly assignableApps = signal<GrantApplicationResponse[]>([]);
  readonly panelApps = signal<GrantApplicationResponse[]>([]);
  readonly reviewers = signal<UserResponse[]>([]);
  readonly financeOfficers = signal<UserResponse[]>([]);

  readonly appOptions = computed<SelectOption[]>(() => this.assignableApps().map(a => ({ value: a.id, label: `APP${String(a.id).padStart(4, '0')} - ${a.projectTitle}` })));
  readonly panelAppOptions = computed<SelectOption[]>(() => this.panelApps().map(a => ({ value: a.id, label: `APP${String(a.id).padStart(4, '0')} - ${a.projectTitle}` })));
  readonly reviewerOptions = computed<SelectOption[]>(() => this.reviewers().map(u => ({ value: u.id, label: `GTU${String(u.id).padStart(4, '0')} - ${u.name}` })));
  readonly financeOfficerOptions = computed<SelectOption[]>(() => this.financeOfficers().map(u => ({ value: u.id, label: `GTU${String(u.id).padStart(4, '0')} - ${u.name}` })));

  // Recorded panel decisions (Grant Admin can review & edit).
  readonly panelDecisions = signal<PanelDecisionResponse[]>([]);
  readonly editPanelOpen = signal(false);
  readonly editingPanel = signal<PanelDecisionResponse | null>(null);
  readonly savingEditPanel = signal(false);

  readonly rows = signal<ReviewerAssignmentResponse[]>([]);
  readonly loading = signal(false);
  readonly savingAssign = signal(false);
  readonly savingPanel = signal(false);

  // Submitted reviews for the application selected in the panel modal (read before deciding).
  readonly panelReviews = signal<ReviewScoreResponse[]>([]);
  readonly loadingReviews = signal(false);
  readonly reviewsByAssignment = computed(() => {
    const map = new Map<number, ReviewScoreResponse[]>();
    for (const s of this.panelReviews()) {
      const arr = map.get(s.assignmentId) ?? [];
      arr.push(s);
      map.set(s.assignmentId, arr);
    }
    return Array.from(map.entries()).map(([assignmentId, scores], i) => ({
      assignmentId, label: `Review ${i + 1}`, scores,
    }));
  });
  readonly total = signal(0);
  readonly totalPages = signal(0);

  appIdFilter = '';
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'assignedDate,desc' });

  readonly assignOpen = signal(false);
  readonly panelOpen = signal(false);

  readonly columns: ColumnDef[] = [
    { key: 'applicationId', header: 'Application', type: 'appId', sortable: true },
    { key: 'reviewerId', header: 'Reviewer', type: 'userId' },
    { key: 'assignedDate', header: 'Assigned', type: 'date' },
    { key: 'status', header: 'Status', type: 'badge' },
    { key: 'conflictScreeningStatus', header: 'Conflict', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['ASSIGNED', 'ACCEPTED', 'DECLINED', 'SUBMITTED'].map((s) => ({
        value: s,
        label: s.charAt(0) + s.slice(1).toLowerCase(),
      })),
    },
  ];

  readonly assignForm = this.fb.nonNullable.group({
    applicationId: [null as number | null, [Validators.required]],
    reviewerId: [null as number | null, [Validators.required]],
    reviewDeadline: [''],
  });

  readonly panelForm = this.fb.nonNullable.group({
    applicationId: [null as number | null, [Validators.required]],
    panelDate: [''],
    consensusScore: [null as number | null],
    awardDecision: ['FULL_AWARD', [Validators.required]],
    awardedAmount: [null as number | null],
    conditionsAttached: [''],
    financeOfficerId: [null as number | null],
  });

  // Edit form for an existing panel decision (award outcome is read-only).
  readonly editPanelForm = this.fb.nonNullable.group({
    panelDate: [''],
    consensusScore: [null as number | null],
    awardedAmount: [null as number | null],
    conditionsAttached: [''],
    financeOfficerId: [null as number | null],
  });

  isAwardDecision(d: string | null | undefined): boolean {
    return d === 'FULL_AWARD' || d === 'REDUCED_AWARD';
  }

  ngOnInit(): void {
    this.load();
    this.loadPanelDecisions();
    // When the panel application changes, load its submitted reviews for the panel to read.
    this.panelForm.get('applicationId')!.valueChanges.subscribe((appId) => {
      this.panelReviews.set([]);
      if (appId) this.loadReviews(appId);
    });
  }

  loadPanelDecisions(): void {
    this.api.listPanelDecisions({ page: 0, size: 100, sort: 'createdAt,desc' }).subscribe((r) => {
      this.panelDecisions.set(r.data.content);
    });
  }

  private loadReviews(appId: number): void {
    this.loadingReviews.set(true);
    this.api.listApplicationReviews(appId).subscribe({
      next: (r) => { this.panelReviews.set(r.data); this.loadingReviews.set(false); },
      error: () => this.loadingReviews.set(false),
    });
  }

  load(): void {
    this.loading.set(true);
    this.api.listAssignments(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content);
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onFilter(params: Record<string, string>): void {
    this.query.update((q) => ({ page: 0, size: q.size, sort: q.sort, ...params }));
    this.load();
  }
  applyAppIdFilter(): void {
    const id = this.appIdFilter.trim();
    this.query.update((q) => ({ ...q, page: 0, applicationId: id || undefined }));
    this.load();
  }
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  openAssign(): void {
    this.assignForm.reset({ applicationId: null, reviewerId: null, reviewDeadline: '' });
    
    // Fetch apps and reviewers for dropdowns
    this.appApi.list({ statuses: 'SUBMITTED,UNDER_REVIEW', size: 100 }).subscribe(r => this.assignableApps.set(r.data.content));
    this.userApi.list({ role: 'ROLE_REVIEWER', size: 100 }).subscribe(r => this.reviewers.set(r.data.content));

    this.assignOpen.set(true);
  }

  saveAssign(): void {
    if (this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      return;
    }
    const v = this.assignForm.getRawValue();
    this.savingAssign.set(true);
    this.api
      .createAssignment({
        applicationId: v.applicationId!,
        reviewerId: v.reviewerId!,
        reviewDeadline: v.reviewDeadline || undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Reviewer assigned.');
          this.assignOpen.set(false);
          this.savingAssign.set(false);
          this.load();
        },
        error: () => this.savingAssign.set(false),
      });
  }

  openPanel(): void {
    this.panelForm.reset({
      applicationId: null,
      panelDate: '',
      consensusScore: null,
      awardDecision: 'FULL_AWARD',
      awardedAmount: null,
      conditionsAttached: '',
      financeOfficerId: null,
    });

    // Fetch apps under review for panel decisions, and finance officers to assign.
    this.appApi.list({ statuses: 'UNDER_REVIEW', size: 100 }).subscribe(r => this.panelApps.set(r.data.content));
    this.userApi.list({ role: 'ROLE_FINANCE_OFFICER', size: 100 }).subscribe(r => this.financeOfficers.set(r.data.content));

    this.panelOpen.set(true);
  }

  savePanel(): void {
    if (this.panelForm.invalid) {
      this.panelForm.markAllAsTouched();
      return;
    }
    const v = this.panelForm.getRawValue();
    // An award decision must name a finance officer.
    if (this.isAwardDecision(v.awardDecision) && !v.financeOfficerId) {
      this.toast.warning('Please assign a finance officer for an award decision.');
      return;
    }
    this.savingPanel.set(true);
    this.api
      .createPanelDecision(v.applicationId!, {
        panelDate: v.panelDate || undefined,
        consensusScore: v.consensusScore ?? undefined,
        awardDecision: v.awardDecision as 'FULL_AWARD' | 'REDUCED_AWARD' | 'RESERVE_LIST' | 'REJECTED',
        awardedAmount: v.awardedAmount ?? undefined,
        conditionsAttached: v.conditionsAttached || undefined,
        financeOfficerId: this.isAwardDecision(v.awardDecision) ? v.financeOfficerId : undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Panel decision recorded.');
          this.panelOpen.set(false);
          this.savingPanel.set(false);
          this.loadPanelDecisions();
        },
        error: () => this.savingPanel.set(false),
      });
  }

  openEditPanel(pd: PanelDecisionResponse): void {
    this.editingPanel.set(pd);
    this.editPanelForm.reset({
      panelDate: pd.panelDate ?? '',
      consensusScore: pd.consensusScore ?? null,
      awardedAmount: pd.awardedAmount ?? null,
      conditionsAttached: pd.conditionsAttached ?? '',
      financeOfficerId: pd.financeOfficerId ?? null,
    });
    this.userApi.list({ role: 'ROLE_FINANCE_OFFICER', size: 100 }).subscribe(r => this.financeOfficers.set(r.data.content));
    this.editPanelOpen.set(true);
  }

  saveEditPanel(): void {
    const pd = this.editingPanel();
    if (!pd) return;
    const v = this.editPanelForm.getRawValue();
    if (this.isAwardDecision(pd.awardDecision) && !v.financeOfficerId) {
      this.toast.warning('Please assign a finance officer for an award decision.');
      return;
    }
    this.savingEditPanel.set(true);
    this.api
      .updatePanelDecision(pd.applicationId, {
        panelDate: v.panelDate || undefined,
        consensusScore: v.consensusScore ?? undefined,
        awardDecision: pd.awardDecision, // outcome is immutable
        awardedAmount: v.awardedAmount ?? undefined,
        conditionsAttached: v.conditionsAttached || undefined,
        financeOfficerId: this.isAwardDecision(pd.awardDecision) ? v.financeOfficerId : undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Panel decision updated.');
          this.editPanelOpen.set(false);
          this.savingEditPanel.set(false);
          this.loadPanelDecisions();
        },
        error: () => this.savingEditPanel.set(false),
      });
  }

  invalid(form: 'assign' | 'panel', ctrl: string): boolean {
    const group: FormGroup = form === 'assign' ? this.assignForm : this.panelForm;
    const c = group.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
