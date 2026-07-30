import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ProgressService } from '../progress.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { DeliverableResponse } from '../../../core/models/progress.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { AwardService } from '../../awards/awards.service';
import { DeliverableType, Role } from '../../../core/models/enums';
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
  selector: 'gt-deliverables',
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
  templateUrl: './deliverables.component.html',
})
export class DeliverablesComponent implements OnInit {
  private api = inject(ProgressService);
  private awardApi = inject(AwardService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly deliverableTypes: DeliverableType[] = ['REPORT', 'DATASET', 'PROTOTYPE', 'PUBLICATION', 'TRAINING', 'POLICY'];

  readonly rows = signal<DeliverableResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploading = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardOptions = computed<SelectOption[]>(() => this.awards().map(a => ({ value: a.id, label: `Award #${a.id} (App ${a.applicationId})` })));
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });
  readonly awardFilter = signal<number | null>(null);
  readonly modalOpen = signal(false);

  readonly uploadOpen = signal(false);
  readonly uploadTarget = signal<DeliverableResponse | null>(null);
  readonly uploadFile = signal<File | null>(null);

  readonly reviewModalOpen = signal(false);
  readonly reviewTarget = signal<DeliverableResponse | null>(null);

  readonly columns: ColumnDef[] = [
    { key: 'awardId', header: 'Award', type: 'number' },
    { key: 'title', header: 'Title' },
    { key: 'type', header: 'Type', type: 'badge' },
    { key: 'dueDate', header: 'Due', type: 'date' },
    { key: 'submittedDate', header: 'Submitted', type: 'date' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED'].map((s) => ({
        value: s,
        label: s.charAt(0) + s.slice(1).toLowerCase(),
      })),
    },
  ];

  readonly form = this.fb.nonNullable.group({
    awardId: [null as number | null, [Validators.required]],
    title: ['', [Validators.required, Validators.maxLength(250)]],
    type: ['REPORT', [Validators.required]],
    dueDate: [''],
  });

  readonly reviewForm = this.fb.nonNullable.group({
    comment: [''],
  });

  ngOnInit(): void {
    this.awardApi.list({ statuses: 'ACTIVE', size: 100 }).subscribe(r => this.awards.set(r.data.content));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listDeliverables(this.query()).subscribe({
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
    this.form.reset({ awardId: null, type: 'REPORT' });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body = {
      awardId: v.awardId!,
      title: v.title,
      type: v.type as DeliverableType,
      dueDate: v.dueDate || undefined,
    };
    this.saving.set(true);
    this.api.createDeliverable(body).subscribe({
      next: () => {
        this.toast.success('Deliverable created. Upload the document to submit it.');
        this.modalOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  openUpload(d: DeliverableResponse): void {
    this.uploadTarget.set(d);
    this.uploadFile.set(null);
    this.uploadOpen.set(true);
  }

  onUploadFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.uploadFile.set(input.files && input.files.length ? input.files[0] : null);
  }

  upload(): void {
    const d = this.uploadTarget();
    const file = this.uploadFile();
    if (!d) return;
    if (!file) { this.toast.error('Please choose a document to upload.'); return; }
    this.uploading.set(true);
    this.api.uploadDeliverable(d.id, file).subscribe({
      next: () => {
        this.toast.success('Deliverable uploaded and submitted for review.');
        this.uploadOpen.set(false);
        this.uploading.set(false);
        this.load();
      },
      error: () => this.uploading.set(false),
    });
  }

  downloadDeliverable(d: DeliverableResponse): void {
    this.api.downloadDeliverable(d.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = d.fileName || `deliverable-${d.id}`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  openReview(d: DeliverableResponse): void {
    this.reviewTarget.set(d);
    this.reviewForm.reset({ comment: '' });
    this.reviewModalOpen.set(true);
  }

  review(d: DeliverableResponse, decision: 'ACCEPT' | 'REJECT'): void {
    const comment = this.reviewForm.getRawValue().comment;
    if (decision === 'REJECT' && !comment.trim()) {
      this.toast.error('Please add a comment so the researcher knows why it was rejected.');
      return;
    }
    this.api.reviewDeliverable(d.id, decision, comment).subscribe(() => {
      this.toast.success(decision === 'ACCEPT' ? 'Deliverable accepted.' : 'Deliverable rejected.');
      this.reviewModalOpen.set(false);
      this.load();
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
