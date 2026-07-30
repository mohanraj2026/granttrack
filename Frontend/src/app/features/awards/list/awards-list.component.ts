import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AwardService } from '../awards.service';
import { ReviewService } from '../../reviews/reviews.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { GrantApplicationResponse } from '../../../core/models/application.model';
import { ApplicationsService } from '../../applications/applications.service';
import { Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { SearchableSelectComponent, SelectOption } from '../../../shared/components/searchable-select/searchable-select.component';

@Component({
  selector: 'gt-awards-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    ModalComponent,
    HasRoleDirective,
    DropdownMenuComponent,
    IconComponent,
    SearchableSelectComponent
  ],
  templateUrl: './awards-list.component.html',
})
export class AwardsListComponent implements OnInit, OnDestroy {
  private api = inject(AwardService);
  private reviewApi = inject(ReviewService);
  private appApi = inject(ApplicationsService);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private destroy$ = new Subject<void>();

  protected readonly Role = Role;

  readonly isFinanceOfficer = computed(() => this.auth.hasAnyRole([Role.FINANCE_OFFICER]));
  readonly financeRejectOpen = signal(false);
  readonly financeRejectTarget = signal<GrantAwardResponse | null>(null);
  readonly financeRejectReason = signal('');

  /**
   * A finance officer may accept/reject an award that is still awaiting finance review.
   * The server authorises the action against the *assigned* officer (returns 403 otherwise),
   * so we surface it whenever the award is ACTIVE + PENDING rather than duplicating (and
   * risking a mismatch on) the assignment check on the client.
   */
  canFinanceReview(a: GrantAwardResponse): boolean {
    return this.isFinanceOfficer()
      && a.status === 'ACTIVE'
      && a.financeReviewStatus === 'PENDING';
  }

  readonly rows = signal<GrantAwardResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);

  readonly apps = signal<GrantApplicationResponse[]>([]);
  readonly appOptions = computed<SelectOption[]>(() => this.apps().map(a => ({ value: a.id, label: `APP${String(a.id).padStart(4, '0')} - ${a.projectTitle}` })));

  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });

  readonly createOpen = signal(false);
  readonly termsOpen = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly columns: ColumnDef[] = [
    { key: 'applicationId', header: 'Application', type: 'appId', sortable: true },
    { key: 'awardedAmount', header: 'Awarded', type: 'money' },
    { key: 'startDate', header: 'Start', type: 'date' },
    { key: 'endDate', header: 'End', type: 'date' },
    { key: 'awardLetterDate', header: 'Award Letter', type: 'date' },
    { key: 'status', header: 'Status', type: 'badge' },
    { key: 'financeReviewStatus', header: 'Finance', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'SUSPENDED', label: 'Suspended' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'TERMINATED', label: 'Terminated' },
      ],
    },
  ];

  readonly form = this.fb.nonNullable.group({
    applicationId: [null as number | null, [Validators.required]],
    awardedAmount: [0, [Validators.required, Validators.min(0)]],
    startDate: [''],
    endDate: [''],
    conditionsRef: [''],
  });

  readonly termsForm = this.fb.nonNullable.group({
    awardedAmount: [0, [Validators.required, Validators.min(0)]],
    startDate: [''],
    endDate: [''],
    conditionsRef: [''],
  });

  ngOnInit(): void {
    this.load();
    this.setupAutoFetch();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupAutoFetch(): void {
    this.form.get('applicationId')?.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe((appId) => {
      if (appId) {
        this.reviewApi.getPanelDecision(appId).subscribe({
          next: (res) => {
            const decision = res.data;
            if (decision && decision.awardedAmount) {
              const today = new Date().toISOString().split('T')[0];
              this.form.patchValue({
                awardedAmount: decision.awardedAmount,
                startDate: today
              });
              this.toast.success('Successfully loaded details from Panel Decision.');
            }
          },
          error: () => {
            // Silently ignore if no panel decision found or error, just don't pre-fill.
          }
        });
      }
    });
  }

  load(): void {
    this.loading.set(true);
    this.api.list(this.query()).subscribe({
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
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  openCreate(): void {
    this.form.reset({ applicationId: null, awardedAmount: 0, startDate: '', endDate: '', conditionsRef: '' });
    this.appApi.list({ statuses: 'AWARDED', size: 100 }).subscribe(r => this.apps.set(r.data.content));
    this.createOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body = {
      applicationId: v.applicationId!,
      awardedAmount: v.awardedAmount,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined,
      conditionsRef: v.conditionsRef || undefined,
    };
    this.saving.set(true);
    this.api.create(body).subscribe({
      next: () => {
        this.toast.success('Award created.');
        this.createOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  openTerms(a: GrantAwardResponse): void {
    this.editingId.set(a.id);
    this.termsForm.reset({
      awardedAmount: a.awardedAmount,
      startDate: a.startDate ?? '',
      endDate: a.endDate ?? '',
      conditionsRef: a.conditionsRef ?? '',
    });
    this.termsOpen.set(true);
  }

  saveTerms(): void {
    const id = this.editingId();
    if (!id || this.termsForm.invalid) { this.termsForm.markAllAsTouched(); return; }
    const v = this.termsForm.getRawValue();
    const body = {
      awardedAmount: v.awardedAmount,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined,
      conditionsRef: v.conditionsRef || undefined,
    };
    this.saving.set(true);
    this.api.updateTerms(id, body).subscribe({
      next: () => {
        this.toast.success('Award terms updated.');
        this.termsOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  approve(a: GrantAwardResponse): void {
    if (!confirm(`Approve award for application #${a.applicationId} and issue the award letter?`)) return;
    this.api.approve(a.id).subscribe(() => {
      this.toast.success('Award approved.');
      this.load();
    });
  }

  changeStatus(a: GrantAwardResponse, status: string): void {
    if (!confirm(`Change award status to ${status}?`)) return;
    this.api.changeStatus(a.id, status).subscribe(() => {
      this.toast.success('Award status updated.');
      this.load();
    });
  }

  acceptAward(a: GrantAwardResponse): void {
    if (!confirm(`Accept the award for application #${a.applicationId}? You'll then be able to set up its milestones.`)) return;
    this.api.financeReview(a.id, 'ACCEPT').subscribe(() => {
      this.toast.success('Award accepted. You can now create disbursement milestones.');
      this.load();
    });
  }

  openRejectAward(a: GrantAwardResponse): void {
    this.financeRejectTarget.set(a);
    this.financeRejectReason.set('');
    this.financeRejectOpen.set(true);
  }

  confirmRejectAward(): void {
    const a = this.financeRejectTarget();
    const reason = this.financeRejectReason().trim();
    if (!a) return;
    if (!reason) { this.toast.warning('Please provide a reason for rejecting.'); return; }
    this.saving.set(true);
    this.api.financeReview(a.id, 'REJECT', reason).subscribe({
      next: () => {
        this.saving.set(false);
        this.financeRejectOpen.set(false);
        this.toast.success('Award rejected. The grant administrator has been notified.');
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
  termsInvalid(ctrl: string): boolean {
    const c = this.termsForm.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
