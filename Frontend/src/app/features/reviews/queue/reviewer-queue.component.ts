import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReviewService } from '../reviews.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageQuery } from '../../../core/models/api-response.model';
import { ReviewerAssignmentResponse } from '../../../core/models/review.model';
import { Role } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { DropdownMenuComponent } from '../../../shared/components/dropdown-menu/dropdown-menu.component';
import { ReviewsTabsComponent } from '../reviews-tabs.component';

@Component({
  selector: 'gt-reviewer-queue',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    PageHeaderComponent,
    DataTableComponent,
    PaginatorComponent,
    SearchFilterBarComponent,
    ModalComponent,
    IconComponent,
    DropdownMenuComponent,
    ReviewsTabsComponent
  ],
  templateUrl: './reviewer-queue.component.html',
})
export class ReviewerQueueComponent implements OnInit {
  private api = inject(ReviewService);
  private auth = inject(AuthService);

  protected readonly Role = Role;

  readonly rows = signal<ReviewerAssignmentResponse[]>([]);
  readonly loading = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  
  readonly viewOpen = signal(false);
  readonly viewApp = signal<any | null>(null);

  private reviewerId: number | null = null;
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'reviewDeadline,asc' });

  // Blind review: only the application id is shown, never PI identity.
  readonly columns: ColumnDef[] = [
    { key: 'applicationId', header: 'Application', type: 'appId', sortable: true },
    { key: 'assignedDate', header: 'Assigned', type: 'date' },
    { key: 'reviewDeadline', header: 'Deadline', type: 'date' },
    { key: 'conflictScreeningStatus', header: 'Conflict', type: 'badge' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    {
      key: 'status',
      label: 'Status',
      // Declined assignments are removed from the reviewer's queue, so they aren't offered as a filter.
      options: ['ASSIGNED', 'ACCEPTED', 'SUBMITTED'].map((s) => ({
        value: s,
        label: s.charAt(0) + s.slice(1).toLowerCase(),
      })),
    },
  ];

  ngOnInit(): void {
    this.reviewerId = this.auth.currentUser()?.id ?? null;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.listAssignments({ reviewerId: this.reviewerId ?? undefined, ...this.query() }).subscribe({
      next: (r) => {
        // A declined assignment disappears from the reviewer's queue.
        this.rows.set(r.data.content.filter((a) => a.status !== 'DECLINED'));
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
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  viewApplication(appId: number): void {
    this.api.getBlindApplication(appId).subscribe({
      next: (res) => {
        this.viewApp.set(res.data);
        this.viewOpen.set(true);
      }
    });
  }
}
