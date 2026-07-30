import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { DisbursementService } from '../disbursements.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { MilestoneResponse } from '../../../core/models/disbursement.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { AwardService } from '../../awards/awards.service';
import { Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { DisbursementTabsComponent } from '../disbursement-tabs.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { SearchableSelectComponent, SelectOption } from '../../../shared/components/searchable-select/searchable-select.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-milestone-scheduler',
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
    DisbursementTabsComponent,
    DropdownMenuComponent,
    SearchableSelectComponent,
    FormsModule,
    IconComponent
  ],
  templateUrl: './milestone-scheduler.component.html',
})
export class MilestoneSchedulerComponent implements OnInit {
  private api = inject(DisbursementService);
  private awardApi = inject(AwardService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly rows = signal<MilestoneResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly releasing = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awardId = signal<number | null>(null);
  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardOptions = computed<SelectOption[]>(() => this.awards().map(a => ({ value: a.id, label: `Award #${a.id} (App ${a.applicationId})` })));
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'dueDate,asc' });

  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Milestone' : 'Create Milestone'));
  readonly releaseModalOpen = signal(false);
  readonly releaseTarget = signal<MilestoneResponse | null>(null);

  // Evidence submission (researcher) + finance review
  readonly evidenceModalOpen = signal(false);
  readonly evidenceTarget = signal<MilestoneResponse | null>(null);
  readonly evidenceFile = signal<File | null>(null);
  readonly reviewModalOpen = signal(false);
  readonly reviewTarget = signal<MilestoneResponse | null>(null);

  readonly timelineMode = computed(() => this.awardId() !== null && this.awardId()! > 0);

  readonly columns: ColumnDef[] = [
    { key: 'awardId', header: 'Award', type: 'number' },
    { key: 'milestoneNumber', header: '#', type: 'number' },
    { key: 'description', header: 'Description' },
    { key: 'dueDate', header: 'Due', type: 'date' },
    { key: 'amount', header: 'Amount', type: 'money' },
    { key: 'evidenceRequired', header: 'Evidence', type: 'bool' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['UPCOMING', 'EVIDENCE_SUBMITTED', 'APPROVED', 'DISBURSED', 'OVERDUE'].map((s) => ({
        value: s,
        label: s.replace(/_/g, ' '),
      })),
    },
  ];

  readonly form = this.fb.nonNullable.group({
    awardId: [null as number | null, [Validators.required]],
    milestoneNumber: [null as number | null, [Validators.required]],
    description: [''],
    dueDate: [''],
    amount: [null as number | null, [Validators.required]],
    evidenceRequired: [true],
  });

  readonly releaseForm = this.fb.nonNullable.group({
    receivingAccountRef: ['', [Validators.required]],
    paymentReference: ['', [Validators.required]],
    releaseDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
  });

  readonly evidenceForm = this.fb.nonNullable.group({
    note: ['', [Validators.required]],
  });

  readonly reviewForm = this.fb.nonNullable.group({
    comment: [''],
  });

  ngOnInit(): void {
    this.awardApi.list({ size: 100 }).subscribe(r => this.awards.set(r.data.content));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listMilestones(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content);
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onAwardId(value: any): void {
    const id = value === null || value === '' ? null : Number(value);
    this.awardId.set(id);
    this.query.update((q) => {
      const next: PageQuery = { ...q, page: 0 };
      if (id === null || Number.isNaN(id)) {
        delete next['awardId'];
      } else {
        next['awardId'] = id;
      }
      return next;
    });
    this.load();
  }

  onFilter(params: Record<string, string>): void {
    this.query.update((q) => {
      const next: PageQuery = { page: 0, size: q.size, sort: q.sort, ...params };
      const id = this.awardId();
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
    const defaultAwardId = this.awardId() || null;
    this.form.reset({ awardId: defaultAwardId, milestoneNumber: null, description: '', dueDate: '', amount: null, evidenceRequired: true });
    this.form.get('awardId')?.enable();
    if (defaultAwardId) {
      this.form.get('awardId')?.disable();
    }
    this.form.get('milestoneNumber')?.enable();
    this.modalOpen.set(true);
  }

  openEdit(m: MilestoneResponse): void {
    this.editingId.set(m.id);
    this.form.reset({
      awardId: m.awardId,
      milestoneNumber: m.milestoneNumber,
      description: m.description ?? '',
      dueDate: m.dueDate ?? '',
      amount: m.amount,
      evidenceRequired: m.evidenceRequired,
    });
    // Award & milestone number are immutable after creation.
    this.form.get('awardId')?.disable();
    this.form.get('milestoneNumber')?.disable();
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.saving.set(true);
    const id = this.editingId();
    if (id) {
      this.api.updateMilestone(id, {
        description: v.description || undefined,
        dueDate: v.dueDate || undefined,
        amount: v.amount!,
        evidenceRequired: v.evidenceRequired,
      }).subscribe({
        next: () => { this.toast.success('Milestone updated.'); this.modalOpen.set(false); this.saving.set(false); this.load(); },
        error: () => this.saving.set(false),
      });
    } else {
      this.api.createMilestone({
        awardId: v.awardId!,
        milestoneNumber: v.milestoneNumber!,
        description: v.description || undefined,
        dueDate: v.dueDate || undefined,
        amount: v.amount!,
        evidenceRequired: v.evidenceRequired,
      }).subscribe({
        next: () => { this.toast.success('Milestone created.'); this.modalOpen.set(false); this.saving.set(false); this.load(); },
        error: () => this.saving.set(false),
      });
    }
  }

  // ---- Evidence submission (researcher) ----
  openEvidence(m: MilestoneResponse): void {
    this.evidenceTarget.set(m);
    this.evidenceFile.set(null);
    this.evidenceForm.reset({ note: '' });
    this.evidenceModalOpen.set(true);
  }

  onEvidenceFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.evidenceFile.set(input.files && input.files.length ? input.files[0] : null);
  }

  confirmSubmitEvidence(): void {
    const m = this.evidenceTarget();
    if (!m) return;
    if (this.evidenceForm.invalid) { this.evidenceForm.markAllAsTouched(); return; }
    if (m.evidenceRequired && !this.evidenceFile()) {
      this.toast.error('This milestone requires a supporting document.');
      return;
    }
    this.saving.set(true);
    this.api.submitEvidence(m.id, this.evidenceForm.getRawValue().note, this.evidenceFile()).subscribe({
      next: () => {
        this.toast.success('Evidence submitted for finance approval.');
        this.evidenceModalOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  // ---- Finance review (approve / return) ----
  openReview(m: MilestoneResponse): void {
    this.reviewTarget.set(m);
    this.reviewForm.reset({ comment: '' });
    this.reviewModalOpen.set(true);
  }

  approve(m: MilestoneResponse): void {
    this.api.approveMilestone(m.id).subscribe(() => {
      this.toast.success('Milestone approved. It can now be released.');
      this.reviewModalOpen.set(false);
      this.load();
    });
  }

  rejectEvidence(m: MilestoneResponse): void {
    const reason = this.reviewForm.getRawValue().comment;
    if (!reason || !reason.trim()) {
      this.toast.error('Please give a reason so the researcher knows what to fix.');
      return;
    }
    this.api.rejectEvidence(m.id, reason).subscribe(() => {
      this.toast.success('Evidence returned to the researcher for resubmission.');
      this.reviewModalOpen.set(false);
      this.load();
    });
  }

  downloadEvidence(m: MilestoneResponse): void {
    this.api.downloadEvidence(m.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = m.evidenceDocName || `milestone-${m.milestoneNumber}-evidence`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  openRelease(m: MilestoneResponse): void {
    this.releaseTarget.set(m);
    this.releaseForm.reset({ receivingAccountRef: '', paymentReference: '', releaseDate: new Date().toISOString().slice(0, 10) });
    this.releaseModalOpen.set(true);
  }

  confirmRelease(): void {
    const m = this.releaseTarget();
    if (!m) return;
    if (this.releaseForm.invalid) { this.releaseForm.markAllAsTouched(); return; }
    const v = this.releaseForm.getRawValue();
    this.releasing.set(true);
    this.api.release(m.id, {
      receivingAccountRef: v.receivingAccountRef,
      paymentReference: v.paymentReference,
      releaseDate: v.releaseDate,
    }).subscribe({
      next: () => {
        this.toast.success('Funds released.');
        this.releaseModalOpen.set(false);
        this.releasing.set(false);
        this.load();
      },
      error: () => this.releasing.set(false),
    });
  }

  releaseInvalid(ctrl: string): boolean {
    const c = this.releaseForm.get(ctrl);
    return !!c && c.invalid && c.touched;
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
