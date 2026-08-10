import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FundingService } from '../funding.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { InstitutionResponse } from '../../../core/models/funding.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { FundingTabsComponent } from '../funding-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PhoneInputDirective } from '../../../shared/directives/phone-input.directive';
import { phoneErrorMessage, phoneValidators } from '../../../core/validators/phone.validators';

@Component({
  selector: 'gt-institutions-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, PageHeaderComponent, DataTableComponent,
    PaginatorComponent, SearchFilterBarComponent, ModalComponent, FundingTabsComponent, DropdownMenuComponent, IconComponent,
    PhoneInputDirective
  ],
  templateUrl: './institutions-list.component.html',
})
export class InstitutionsListComponent implements OnInit {
  private api = inject(FundingService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  readonly rows = signal<InstitutionResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'name,asc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Institution' : 'Create Institution'));

  readonly columns: ColumnDef[] = [
    { key: 'institutionCode', header: 'Code' },
    { key: 'name', header: 'Name' },
    { key: 'type', header: 'Type' },
    { key: 'city', header: 'City' },
  ];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    type: ['', [Validators.required]],
    country: ['', [Validators.required]],
    universityName: ['', [Validators.required]],
    address: ['', [Validators.required]],
    city: ['', [Validators.required]],
    state: ['', [Validators.required]],
    pincode: ['', [Validators.required]],
    mobileNumber: ['', phoneValidators],
    email: ['', [Validators.email]],
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.listInstitutions(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content);
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onFilter(p: Record<string, string>): void { this.query.update((q) => ({ page: 0, size: q.size, sort: q.sort, ...p })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  openCreate(): void { this.editingId.set(null); this.form.reset(); this.modalOpen.set(true); }
  openEdit(i: InstitutionResponse): void {
    this.editingId.set(i.id);
    this.form.reset({ 
      name: i.name, type: i.type ?? '', country: i.country ?? '',
      universityName: i.universityName ?? '', address: i.address ?? '',
      city: i.city ?? '', state: i.state ?? '', pincode: i.pincode ?? '',
      mobileNumber: i.mobileNumber ?? '', email: i.email ?? ''
    });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body = { 
      name: v.name, type: v.type, country: v.country,
      universityName: v.universityName, address: v.address,
      city: v.city, state: v.state, pincode: v.pincode,
      mobileNumber: v.mobileNumber, email: v.email || undefined
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateInstitution(id, body) : this.api.createInstitution(body);
    req.subscribe({
      next: () => { this.toast.success('Saved.'); this.modalOpen.set(false); this.saving.set(false); this.load(); },
      error: () => this.saving.set(false),
    });
  }

  remove(i: InstitutionResponse): void {
    if (!confirm(`Delete institution "${i.name}"?`)) return;
    this.api.deleteInstitution(i.id).subscribe(() => { this.toast.success('Deleted.'); this.load(); });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }

  phoneError(): string | null { return phoneErrorMessage(this.form.get('mobileNumber')); }
}
