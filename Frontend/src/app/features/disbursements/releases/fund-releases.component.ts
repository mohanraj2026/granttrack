import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DisbursementService } from '../disbursements.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { FundDisbursementResponse } from '../../../core/models/disbursement.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { DisbursementTabsComponent } from '../disbursement-tabs.component';

@Component({
  selector: 'gt-fund-releases',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    DisbursementTabsComponent,
  ],
  template: `
    <gt-disbursement-tabs />

    <gt-page-header title="Fund Releases" subtitle="Disbursements released against approved milestones." />

    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
        <gt-search-filter-bar
          [showSearch]="false"
          [filters]="statusFilter"
          (change)="onFilter($event)">
          <input
            type="number"
            class="form-control"
            style="width: 200px;"
            placeholder="Award ID"
            [value]="awardId() ?? ''"
            (change)="onAwardId($any($event.target).value)" />
        </gt-search-filter-bar>
      </div>

      <div class="card-body p-0">
        <gt-data-table
          [columns]="columns"
          [rows]="rows()"
          [loading]="loading()"
          emptyTitle="No releases"
          emptySubtitle="Released disbursements will appear here." />
      </div>

      <div class="card-footer bg-white border-top px-4 py-3">
        <gt-paginator
          [page]="query().page || 0" [size]="query().size || 20"
          [totalElements]="total()" [totalPages]="totalPages()"
          (pageChange)="onPage($event)" (sizeChange)="onSize($event)" />
      </div>
    </div>
  `,
})
export class FundReleasesComponent implements OnInit {
  private api = inject(DisbursementService);

  readonly rows = signal<FundDisbursementResponse[]>([]);
  readonly loading = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly awardId = signal<number | null>(null);
  readonly query = signal<PageQuery>({ page: 0, size: 20 });

  readonly columns: ColumnDef[] = [
    { key: 'awardId', header: 'Award', type: 'number' },
    { key: 'milestoneDescription', header: 'Milestone' },
    { key: 'amount', header: 'Amount', type: 'money' },
    { key: 'disbursedDate', header: 'Disbursed', type: 'date' },
    { key: 'receivingAccountRef', header: 'Account Ref' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      options: ['PENDING', 'RELEASED', 'FAILED'].map((s) => ({ value: s, label: s })),
    },
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listReleases(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content);
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onAwardId(value: string): void {
    const id = value === '' ? null : Number(value);
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
      const next: PageQuery = { page: 0, size: q.size, ...params };
      const id = this.awardId();
      if (id !== null && !Number.isNaN(id)) next['awardId'] = id;
      return next;
    });
    this.load();
  }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }
}
