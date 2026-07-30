import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { OutputService } from '../outputs.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { ResearchOutputResponse } from '../../../core/models/output.model';
import { GrantAwardResponse } from '../../../core/models/award.model';
import { AwardService } from '../../awards/awards.service';
import { OutputStatus, OutputType, Role } from '../../../core/models/enums';
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
  selector: 'gt-research-outputs',
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
  templateUrl: './research-outputs.component.html',
})
export class ResearchOutputsComponent implements OnInit {
  private api = inject(OutputService);
  private awardApi = inject(AwardService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly rows = signal<ResearchOutputResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awards = signal<GrantAwardResponse[]>([]);
  readonly awardOptions = computed<SelectOption[]>(() => this.awards().map(a => ({ value: a.id, label: `Award #${a.id} (App ${a.applicationId})` })));

  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly modalTitle = computed(() => (this.editingId() ? 'Edit Output' : 'Create Output'));

  readonly columns: ColumnDef[] = [
    { key: 'title', header: 'Title', sortable: true },
    { key: 'type', header: 'Type', type: 'badge' },
    { key: 'authors', header: 'Authors' },
    { key: 'publicationVenue', header: 'Venue' },
    { key: 'doi', header: 'DOI' },
    { key: 'publishedDate', header: 'Published', type: 'date' },
    { key: 'openAccessCompliant', header: 'Open Access', type: 'bool' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly typeFilter = [
    {
      key: 'type',
      label: 'Type',
      options: [
        { value: 'JOURNAL_ARTICLE', label: 'Journal Article' },
        { value: 'CONFERENCE_PAPER', label: 'Conference Paper' },
        { value: 'PATENT', label: 'Patent' },
        { value: 'DATASET', label: 'Dataset' },
        { value: 'SOFTWARE', label: 'Software' },
        { value: 'POLICY_BRIEF', label: 'Policy Brief' },
      ],
    },
  ];

  readonly form = this.fb.nonNullable.group({
    awardId: [null as number | null, [Validators.required]],
    type: ['JOURNAL_ARTICLE' as OutputType, [Validators.required]],
    title: ['', [Validators.required, Validators.maxLength(300)]],
    authors: [''],
    publicationVenue: [''],
    doi: [''],
    publishedDate: [''],
    openAccessCompliant: [false],
    status: ['IN_PREPARATION' as OutputStatus],
  });

  ngOnInit(): void {
    this.awardApi.list({ statuses: 'ACTIVE', size: 100 }).subscribe(r => this.awards.set(r.data.content));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listOutputs(this.query()).subscribe({
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
      type: 'JOURNAL_ARTICLE',
      title: '',
      authors: '',
      publicationVenue: '',
      doi: '',
      publishedDate: '',
      openAccessCompliant: false,
      status: 'IN_PREPARATION',
    });
    this.modalOpen.set(true);
  }

  openEdit(o: ResearchOutputResponse): void {
    this.editingId.set(o.id);
    this.form.reset({
      awardId: o.awardId,
      type: o.type,
      title: o.title,
      authors: o.authors ?? '',
      publicationVenue: o.publicationVenue ?? '',
      doi: o.doi ?? '',
      publishedDate: o.publishedDate ?? '',
      openAccessCompliant: o.openAccessCompliant,
      status: o.status,
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
      type: v.type,
      title: v.title,
      authors: v.authors || undefined,
      publicationVenue: v.publicationVenue || undefined,
      doi: v.doi || undefined,
      publishedDate: v.publishedDate || undefined,
      openAccessCompliant: v.openAccessCompliant,
      status: v.status,
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id ? this.api.updateOutput(id, body) : this.api.createOutput(body);
    req.subscribe({
      next: () => {
        this.toast.success(id ? 'Output updated.' : 'Output created.');
        this.modalOpen.set(false);
        this.saving.set(false);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  remove(o: ResearchOutputResponse): void {
    if (!confirm(`Delete output "${o.title}"?`)) return;
    this.api.deleteOutput(o.id).subscribe(() => {
      this.toast.success('Output deleted.');
      this.load();
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
