import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FundingService } from '../funding.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { FundingSchemeResponse, GrantCallResponse } from '../../../core/models/funding.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { FundingTabsComponent } from '../funding-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-calls-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    ModalComponent,
    FundingTabsComponent,
    DropdownMenuComponent,
    IconComponent
  ],
  templateUrl: './calls-list.component.html',
})
export class CallsListComponent implements OnInit {
  private api = inject(FundingService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  readonly rows = signal<GrantCallResponse[]>([]);
  readonly schemes = signal<FundingSchemeResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'openDate,desc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Grant Call' : 'Create Grant Call'));

  readonly columns: ColumnDef[] = [
    { key: 'callTitle', header: 'Call Title', sortable: true },
    { key: 'schemeName', header: 'Scheme' },
    { key: 'openDate', header: 'Opens', type: 'date' },
    { key: 'closeDate', header: 'Closes', type: 'date' },
    { key: 'reviewMethod', header: 'Review', type: 'badge' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['UPCOMING', 'OPEN', 'UNDER_REVIEW', 'AWARDED', 'CLOSED', 'TERMINATED'].map((s) => ({
        value: s,
        label: s.replace('_', ' '),
      })),
    },
  ];

  readonly form = this.fb.nonNullable.group({
    schemeId: [null as number | null, [Validators.required]],
    callTitle: ['', [Validators.required, Validators.maxLength(250)]],
    openDate: ['', [Validators.required]],
    closeDate: ['', [Validators.required]],
    expectedAwards: [null as number | null, [Validators.min(1)]],
    totalBudgetAllocated: [null as number | null, [Validators.min(0)]],
    reviewMethod: ['DOUBLE_BLIND', [Validators.required]],
  });

  ngOnInit(): void {
    this.load();
    this.api.listSchemes({ size: 200, status: 'ACTIVE' }).subscribe((r) => this.schemes.set(r.data.content));
  }

  load(): void {
    this.loading.set(true);
    this.api.listCalls(this.query()).subscribe({
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
    this.editingId.set(null);
    const today = new Date().toISOString().split('T')[0];
    this.form.reset({ reviewMethod: 'DOUBLE_BLIND', schemeId: null, openDate: today });
    this.modalOpen.set(true);
  }
  openEdit(c: GrantCallResponse): void {
    this.editingId.set(c.id);
    this.form.reset({
      schemeId: c.schemeId,
      callTitle: c.callTitle,
      openDate: c.openDate,
      closeDate: c.closeDate,
      expectedAwards: c.expectedAwards ?? null,
      totalBudgetAllocated: c.totalBudgetAllocated ?? null,
      reviewMethod: c.reviewMethod,
    });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    const body = {
      schemeId: v.schemeId!,
      callTitle: v.callTitle,
      openDate: v.openDate,
      closeDate: v.closeDate,
      expectedAwards: v.expectedAwards ?? undefined,
      totalBudgetAllocated: v.totalBudgetAllocated ?? undefined,
      reviewMethod: v.reviewMethod as 'DOUBLE_BLIND' | 'PANEL',
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateCall(id, body) : this.api.createCall(body);
    req.subscribe({
      next: () => {
        this.toast.success(id ? 'Call updated.' : 'Call created.');
        this.modalOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  open(c: GrantCallResponse): void {
    this.api.openCall(c.id).subscribe(() => { this.toast.success('Call opened for submissions.'); this.load(); });
  }
  close(c: GrantCallResponse): void {
    if (!confirm(`Close call "${c.callTitle}"?`)) return;
    this.api.closeCall(c.id).subscribe(() => { this.toast.success('Call closed.'); this.load(); });
  }
  terminate(c: GrantCallResponse): void {
    if (!confirm(`Are you sure you want to TERMINATE call "${c.callTitle}"? This will cancel all pending applications.`)) return;
    this.api.terminateCall(c.id).subscribe(() => { this.toast.success('Call terminated.'); this.load(); });
  }
  remove(c: GrantCallResponse): void {
    if (!confirm(`Delete call "${c.callTitle}"?`)) return;
    this.api.deleteCall(c.id).subscribe(() => { this.toast.success('Call deleted.'); this.load(); });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
