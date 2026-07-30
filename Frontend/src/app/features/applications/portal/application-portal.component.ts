import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ApplicationsService } from '../applications.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { GrantApplicationResponse } from '../../../core/models/application.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { Role } from '../../../core/models/enums';

@Component({
  selector: 'gt-application-portal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, RouterLink, PageHeaderComponent, DataTableComponent, PaginatorComponent,
    SearchFilterBarComponent, HasRoleDirective, DropdownMenuComponent, IconComponent
  ],
  template: `
    <gt-page-header title="Grant Applications" subtitle="Your proposals across the pipeline.">
      <a *gtHasRole="researcher" class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" routerLink="/applications/opportunities"><gt-icon name="search" [size]="16" /> Browse open calls</a>
      <button *gtHasRole="researcher" class="btn btn-primary d-inline-flex align-items-center gap-2" (click)="newApp()"><gt-icon name="plus" [size]="16" /> New application</button>
    </gt-page-header>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
        <gt-search-filter-bar searchPlaceholder="Search by project title…" [filters]="filters" (change)="onFilter($event)" />
      </div>
      <div class="card-body p-0">
        <gt-data-table
          [columns]="columns" [rows]="rows()" [loading]="loading()"
          [sortField]="(query().sort || '').split(',')[0]"
          [sortDir]="$any((query().sort || '').split(',')[1] || 'asc')"
          (sortChange)="onSort($event)" [rowActions]="actions"
          emptyTitle="No applications yet"
          emptySubtitle="Start a new application against an open grant call.">
          <ng-template #actions let-row>
            <gt-dropdown-menu>
              <button class="dropdown-item py-2" (click)="view(row)">
                <gt-icon name="eye" [size]="16" class="me-2 text-secondary" /> View
              </button>
              @if (row.status === 'DRAFT') {
                <button class="dropdown-item py-2" (click)="edit(row)">
                  <gt-icon name="edit" [size]="16" class="me-2 text-secondary" /> Edit
                </button>
                <button class="dropdown-item py-2" (click)="submit(row)">
                  <gt-icon name="send" [size]="16" class="me-2 text-success" /> Submit
                </button>
              }
              @if (row.status === 'DRAFT' || row.status === 'SUBMITTED') {
                <button class="dropdown-item py-2 text-danger" (click)="withdraw(row)">
                  <gt-icon name="x-circle" [size]="16" class="me-2 text-danger" /> Withdraw
                </button>
              }
            </gt-dropdown-menu>
          </ng-template>
        </gt-data-table>
      </div>
      <div class="card-footer bg-white border-top px-4 py-3">
        <gt-paginator [page]="query().page || 0" [size]="query().size || 20" [totalElements]="total()"
          [totalPages]="totalPages()" (pageChange)="onPage($event)" (sizeChange)="onSize($event)" />
      </div>
    </div>
  `,
})
export class ApplicationPortalComponent implements OnInit {
  private api = inject(ApplicationsService);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly researcher = [Role.RESEARCHER];
  readonly rows = signal<GrantApplicationResponse[]>([]);
  readonly loading = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'createdAt,desc' });

  readonly columns: ColumnDef[] = [
    { key: 'id', header: 'Application #', type: 'appId', sortable: true, width: '120px' },
    { key: 'projectTitle', header: 'Project', sortable: true },
    { key: 'discipline', header: 'Discipline' },
    { key: 'requestedAmount', header: 'Requested', type: 'money' },
    { key: 'submissionDate', header: 'Submitted', type: 'date' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly filters = [
    {
      key: 'status',
      label: 'Status',
      options: ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'AWARDED', 'DECLINED', 'WITHDRAWN'].map((s) => ({
        value: s,
        label: s.replace('_', ' '),
      })),
    },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.list(this.query()).subscribe({
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
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  newApp(): void { this.router.navigate(['/applications/new']); }
  view(a: GrantApplicationResponse): void { this.router.navigate(['/applications', a.id]); }
  edit(a: GrantApplicationResponse): void { this.router.navigate(['/applications', a.id, 'edit']); }

  submit(a: GrantApplicationResponse): void {
    if (!confirm('Submit this application? You will not be able to edit it afterwards.')) return;
    this.api.submit(a.id).subscribe(() => { this.toast.success('Application submitted.'); this.load(); });
  }
  withdraw(a: GrantApplicationResponse): void {
    if (!confirm('Withdraw this application?')) return;
    this.api.withdraw(a.id).subscribe(() => { this.toast.success('Application withdrawn.'); this.load(); });
  }
}
