import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FundingService } from '../funding.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { SponsorResponse } from '../../../core/models/funding.model';
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
  selector: 'gt-sponsors-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, PageHeaderComponent, DataTableComponent,
    PaginatorComponent, SearchFilterBarComponent, ModalComponent, FundingTabsComponent, DropdownMenuComponent, IconComponent,
    PhoneInputDirective
  ],
  templateUrl: './sponsors-list.component.html',
})
export class SponsorsListComponent implements OnInit {
  private api = inject(FundingService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  readonly rows = signal<SponsorResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'name,asc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Sponsor' : 'Create Sponsor'));

  readonly columns: ColumnDef[] = [
    { key: 'sponsorCode', header: 'Code' },
    { key: 'name', header: 'Name' },
    { key: 'type', header: 'Type' },
    { key: 'contactEmail', header: 'Email' },
    { key: 'phone', header: 'Phone' },
  ];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    type: ['', [Validators.required]],
    contactEmail: ['', [Validators.required, Validators.email]],
    phone: ['', phoneValidators],
    address: ['', [Validators.required]],
    website: ['', [Validators.required]],
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.listSponsors(this.query()).subscribe({
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
  openEdit(s: SponsorResponse): void {
    this.editingId.set(s.id);
    this.form.reset({ 
      name: s.name, type: s.type ?? '', contactEmail: s.contactEmail ?? '',
      phone: s.phone ?? '', address: s.address ?? '', website: s.website ?? ''
    });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body = { 
      name: v.name, type: v.type, contactEmail: v.contactEmail,
      phone: v.phone, address: v.address, website: v.website 
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateSponsor(id, body) : this.api.createSponsor(body);
    req.subscribe({
      next: () => { this.toast.success('Saved.'); this.modalOpen.set(false); this.saving.set(false); this.load(); },
      error: () => this.saving.set(false),
    });
  }

  remove(s: SponsorResponse): void {
    if (!confirm(`Delete sponsor "${s.name}"?`)) return;
    this.api.deleteSponsor(s.id).subscribe(() => { this.toast.success('Deleted.'); this.load(); });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }

  phoneError(): string | null { return phoneErrorMessage(this.form.get('phone')); }
}
