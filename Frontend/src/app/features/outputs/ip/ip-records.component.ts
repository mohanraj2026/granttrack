import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { OutputService } from '../outputs.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { IpRecordResponse } from '../../../core/models/output.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { AwardService } from '../../awards/awards.service';
import { IpStatus, IpType, Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { OutputsTabsComponent } from '../outputs-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { SearchableSelectComponent, SelectOption } from '../../../shared/components/searchable-select/searchable-select.component';

@Component({
  selector: 'gt-ip-records',
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
    HasRoleDirective,
    OutputsTabsComponent,
    DropdownMenuComponent,
    IconComponent,
    SearchableSelectComponent,
    FormsModule
  ],
  templateUrl: './ip-records.component.html',
})
export class IpRecordsComponent implements OnInit {
  private api = inject(OutputService);
  private awardApi = inject(AwardService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly rows = signal<IpRecordResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardOptions = computed<SelectOption[]>(() => this.awards().map(a => ({ value: a.id, label: `Award #${a.id} (App ${a.applicationId})` })));

  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit IP Record' : 'Create IP Record'));

  readonly columns: ColumnDef[] = [
    { key: 'title', header: 'Title', sortable: true },
    { key: 'ipType', header: 'Type', type: 'badge' },
    { key: 'inventors', header: 'Inventors' },
    { key: 'filingDate', header: 'Filed', type: 'date' },
    { key: 'grantDate', header: 'Granted', type: 'date' },
    { key: 'ownershipPercent', header: 'Ownership %', type: 'number' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: [
        { value: 'FILED', label: 'Filed' },
        { value: 'GRANTED', label: 'Granted' },
        { value: 'ABANDONED', label: 'Abandoned' },
      ],
    },
  ];

  readonly form = this.fb.nonNullable.group({
    awardId: [null as number | null, [Validators.required]],
    ipType: ['PATENT' as IpType, [Validators.required]],
    title: ['', [Validators.required, Validators.maxLength(300)]],
    inventors: [''],
    filingDate: [''],
    grantDate: [''],
    ownershipPercent: [null as number | null, [Validators.min(0), Validators.max(100)]],
    status: ['FILED' as IpStatus],
  });

  ngOnInit(): void {
    this.awardApi.list({ statuses: 'ACTIVE', size: 100 }).subscribe(r => this.awards.set(r.data.content));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listIp(this.query()).subscribe({
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
  onSort(sort: string): void {
    this.query.update((q) => ({ ...q, sort }));
    this.load();
  }
  onPage(page: number): void {
    this.query.update((q) => ({ ...q, page }));
    this.load();
  }
  onSize(size: number): void {
    this.query.update((q) => ({ ...q, size, page: 0 }));
    this.load();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      awardId: null,
      ipType: 'PATENT',
      title: '',
      inventors: '',
      filingDate: '',
      grantDate: '',
      ownershipPercent: null,
      status: 'FILED',
    });
    this.modalOpen.set(true);
  }

  openEdit(r: IpRecordResponse): void {
    this.editingId.set(r.id);
    this.form.reset({
      awardId: r.awardId,
      ipType: r.ipType,
      title: r.title,
      inventors: r.inventors ?? '',
      filingDate: r.filingDate ?? '',
      grantDate: r.grantDate ?? '',
      ownershipPercent: r.ownershipPercent ?? null,
      status: r.status,
    });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = {
      awardId: v.awardId!,
      ipType: v.ipType,
      title: v.title,
      inventors: v.inventors || undefined,
      filingDate: v.filingDate || undefined,
      grantDate: v.grantDate || undefined,
      ownershipPercent: v.ownershipPercent ?? undefined,
      status: v.status,
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateIp(id, body) : this.api.createIp(body);
    req.subscribe({
      next: () => {
        this.toast.success(id ? 'IP record updated.' : 'IP record created.');
        this.modalOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  remove(r: IpRecordResponse): void {
    if (!confirm(`Delete IP record "${r.title}"?`)) return;
    this.api.deleteIp(r.id).subscribe(() => {
      this.toast.success('IP record deleted.');
      this.load();
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
