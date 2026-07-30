import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FundingService } from '../funding.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { FundingSchemeResponse, SponsorResponse } from '../../../core/models/funding.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { FundingTabsComponent } from '../funding-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-schemes-list',
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
  templateUrl: './schemes-list.component.html',
})
export class SchemesListComponent implements OnInit {
  private api = inject(FundingService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  readonly rows = signal<FundingSchemeResponse[]>([]);
  readonly sponsors = signal<SponsorResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);

  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Funding Scheme' : 'Create Funding Scheme'));

  readonly columns: ColumnDef[] = [
    { key: 'schemeCode', header: 'ID', sortable: true },
    { key: 'schemeName', header: 'Scheme', sortable: true },
    { key: 'sponsorName', header: 'Sponsor' },
    { key: 'researchArea', header: 'Research Area' },
    { key: 'minAwardAmount', header: 'Min', type: 'money' },
    { key: 'maxAwardAmount', header: 'Max', type: 'money' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'CLOSED', label: 'Closed' },
        { value: 'SUSPENDED', label: 'Suspended' },
      ],
    },
  ];

  readonly form = this.fb.nonNullable.group({
    schemeName: ['', [Validators.required, Validators.maxLength(200)]],
    sponsorId: [null as number | null, [Validators.required]],
    researchArea: ['', [Validators.required, Validators.maxLength(200)]],
    researchAreaOther: [''],
    category: ['', [Validators.required, Validators.maxLength(100)]],
    categoryOther: [''],
    minAwardAmount: [null as number | null, [Validators.required, Validators.min(0)]],
    maxAwardAmount: [null as number | null, [Validators.required, Validators.min(1)]],
    eligibleApplicants: ['', [Validators.required, Validators.maxLength(500)]],
    eligibleApplicantsOther: [''],
    fromDate: [''],
    toDate: [''],
    fundingDurationMonths: [null as number | null],
    description: ['', [Validators.required]],
    status: ['ACTIVE', [Validators.required]],
  });

  /** Preset award ceilings offered as "Up to ₹X" in the scheme form. */
  readonly awardCeilings = [50000, 100000, 250000, 500000, 1000000, 2500000, 5000000];
  readonly categories = ['Basic Research', 'Applied Research', 'Translational', 'Development', 'Others'];
  readonly researchAreas = ['Engineering', 'Medical', 'Physical Sciences', 'Social Sciences', 'Others'];
  readonly applicantTypes = ['M.E.', 'M.Tech', 'M.Sc.', 'Ph.D.', 'Others'];

  ngOnInit(): void {
    this.load();
    this.api.listSponsors({ size: 200 }).subscribe((r) => this.sponsors.set(r.data.content));
  }

  load(): void {
    this.loading.set(true);
    this.api.listSchemes(this.query()).subscribe({
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
    this.selectedFile.set(null);
    const today = new Date().toISOString().split('T')[0];
    this.form.reset({ minAwardAmount: null, maxAwardAmount: null, status: 'ACTIVE', sponsorId: null, fundingDurationMonths: null, fromDate: today, toDate: '', description: '', category: '', categoryOther: '', researchArea: '', researchAreaOther: '', eligibleApplicants: '', eligibleApplicantsOther: '' });
    this.modalOpen.set(true);
  }

  openEdit(s: FundingSchemeResponse): void {
    this.editingId.set(s.id);
    this.selectedFile.set(null);
    
    let cat = s.category ?? '';
    let catOther = '';
    if (cat && !this.categories.includes(cat)) {
      catOther = cat;
      cat = 'Others';
    }

    let area = s.researchArea ?? '';
    let areaOther = '';
    if (area && !this.researchAreas.includes(area)) {
      areaOther = area;
      area = 'Others';
    }

    let app = s.eligibleApplicants ?? '';
    let appOther = '';
    if (app && !this.applicantTypes.includes(app)) {
      appOther = app;
      app = 'Others';
    }

    this.form.reset({
      schemeName: s.schemeName,
      sponsorId: s.sponsorId,
      researchArea: area,
      researchAreaOther: areaOther,
      category: cat,
      categoryOther: catOther,
      minAwardAmount: s.minAwardAmount,
      maxAwardAmount: s.maxAwardAmount,
      eligibleApplicants: app,
      eligibleApplicantsOther: appOther,
      fromDate: s.fromDate ?? '',
      toDate: s.toDate ?? '',
      description: s.description ?? '',
      fundingDurationMonths: s.fundingDurationMonths ?? null,
      status: s.status,
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
      schemeName: v.schemeName,
      sponsorId: v.sponsorId!,
      researchArea: v.researchArea === 'Others' ? v.researchAreaOther : v.researchArea,
      category: v.category === 'Others' ? v.categoryOther : v.category,
      minAwardAmount: v.minAwardAmount!,
      maxAwardAmount: v.maxAwardAmount!,
      eligibleApplicants: v.eligibleApplicants === 'Others' ? v.eligibleApplicantsOther : v.eligibleApplicants,
      fromDate: v.fromDate || undefined,
      toDate: v.toDate || undefined,
      description: v.description || undefined,
      fundingDurationMonths: v.fundingDurationMonths ?? undefined,
      status: v.status as 'ACTIVE' | 'CLOSED' | 'SUSPENDED',
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateScheme(id, body) : this.api.createScheme(body);
    req.subscribe({
      next: (res) => {
        const file = this.selectedFile();
        if (file) {
          this.api.uploadSchemeDocument(res.data.id, file).subscribe({
            next: () => {
              this.toast.success(id ? 'Scheme updated & document uploaded.' : 'Scheme created & document uploaded.');
              this.finishSave();
            },
            error: () => this.saving.set(false),
          });
        } else {
          this.toast.success(id ? 'Scheme updated.' : 'Scheme created.');
          this.finishSave();
        }
      },
      error: () => this.saving.set(false),
    });
  }

  private finishSave(): void {
    this.modalOpen.set(false);
    this.saving.set(false);
    this.load();
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile.set(file);
    }
  }

  remove(s: FundingSchemeResponse): void {
    if (!confirm(`Delete scheme "${s.schemeName}"?`)) return;
    this.api.deleteScheme(s.id).subscribe(() => {
      this.toast.success('Scheme deleted.');
      this.load();
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
