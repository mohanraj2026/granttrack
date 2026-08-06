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
  template: `
    <gt-funding-tabs />
    <gt-page-header title="Sponsors" subtitle="Funding bodies that own and finance schemes.">
      <button class="btn btn-primary d-inline-flex align-items-center gap-2" (click)="openCreate()"><gt-icon name="plus" [size]="16" /> New sponsor</button>
    </gt-page-header>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
        <gt-search-filter-bar searchPlaceholder="Search sponsors…" (change)="onFilter($event)" />
      </div>
      <div class="card-body p-0">
        <gt-data-table [columns]="columns" [rows]="rows()" [loading]="loading()" [rowActions]="actions"
          emptyTitle="No sponsors" emptySubtitle="Add a funding body to start creating schemes.">
          <ng-template #actions let-row>
            <gt-dropdown-menu>
              <button class="dropdown-item py-2" (click)="openEdit(row)">
                <gt-icon name="edit" [size]="16" class="me-2 text-secondary" /> Edit
              </button>
              <button class="dropdown-item py-2 text-danger" (click)="remove(row)">
                <gt-icon name="trash-2" [size]="16" class="me-2 text-danger" /> Delete
              </button>
            </gt-dropdown-menu>
          </ng-template>
        </gt-data-table>
      </div>
      <div class="card-footer bg-white border-top px-4 py-3">
        <gt-paginator [page]="query().page || 0" [size]="query().size || 20" [totalElements]="total()"
          [totalPages]="totalPages()" (pageChange)="onPage($event)" (sizeChange)="onSize($event)" />
      </div>
    </div>

    <gt-modal [open]="modalOpen()" [title]="modalTitle()" [width]="640" (closed)="modalOpen.set(false)">
      <form [formGroup]="form" id="sponsorForm" (ngSubmit)="save()">
        <div class="mb-3">
          <label class="form-label" for="nameInput">Name <span class="text-danger">*</span></label>
          <div class="input-group gt-input" [class.is-invalid]="invalid('name')">
            <span class="input-group-text"><gt-icon name="landmark" [size]="16" /></span>
            <input class="form-control py-2" formControlName="name" id="nameInput" [class.is-invalid]="invalid('name')" placeholder="e.g. National Science Foundation" />
          </div>
          @if (invalid('name')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Name is required.</div> }
        </div>

        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label" for="typeInput">Type <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('type')">
              <span class="input-group-text"><gt-icon name="layers" [size]="16" /></span>
              <select class="form-select" formControlName="type" id="typeInput" [class.is-invalid]="invalid('type')">
                <option value="" disabled>Select type…</option>
                <option value="Govt">Government</option>
                <option value="Corporate">Corporate</option>
                <option value="Foundation">Foundation</option>
              </select>
            </div>
            @if (invalid('type')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Type is required.</div> }
          </div>
          <div class="col-md-6">
            <label class="form-label" for="emailInput">Contact email <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('contactEmail')">
              <span class="input-group-text"><gt-icon name="mail" [size]="16" /></span>
              <input type="email" class="form-control py-2" formControlName="contactEmail" id="emailInput" [class.is-invalid]="invalid('contactEmail')" placeholder="grants@sponsor.org" />
            </div>
            @if (invalid('contactEmail')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> A valid email is required.</div> }
          </div>
        </div>

        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label" for="phoneInput">Phone <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('phone')">
              <span class="input-group-text"><gt-icon name="bell" [size]="16" /></span>
              <input type="tel" gtPhoneInput class="form-control py-2" formControlName="phone" id="phoneInput" [class.is-invalid]="invalid('phone')" placeholder="9876543210" />
            </div>
            @if (phoneError(); as message) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> {{ message }}</div> }
          </div>
          <div class="col-md-6">
            <label class="form-label" for="websiteInput">Website <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('website')">
              <span class="input-group-text"><gt-icon name="external-link" [size]="16" /></span>
              <input type="url" class="form-control py-2" formControlName="website" id="websiteInput" [class.is-invalid]="invalid('website')" placeholder="https://sponsor.org" />
            </div>
            @if (invalid('website')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Website is required.</div> }
          </div>
        </div>

        <div class="mb-1">
          <label class="form-label" for="addressInput">Address <span class="text-danger">*</span></label>
          <div class="input-group gt-input" [class.is-invalid]="invalid('address')">
            <span class="input-group-text"><gt-icon name="building" [size]="16" /></span>
            <input class="form-control py-2" formControlName="address" id="addressInput" [class.is-invalid]="invalid('address')" placeholder="Street, city, country" />
          </div>
          @if (invalid('address')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Address is required.</div> }
        </div>
      </form>

      <div footer class="d-flex gap-2 justify-content-end">
        <button class="btn btn-light" (click)="modalOpen.set(false)">Cancel</button>
        <button class="btn btn-primary px-4 d-inline-flex align-items-center gap-2" form="sponsorForm" type="submit" [disabled]="saving()">
          @if (saving()) { <span class="spinner-border spinner-border-sm"></span> Saving… } @else { <gt-icon name="check" [size]="16" /> Save sponsor }
        </button>
      </div>
    </gt-modal>
  `,
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
