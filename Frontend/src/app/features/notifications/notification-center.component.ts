import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NotificationService } from './notifications.service';
import { ToastService } from '../../core/services/toast.service';
import { PageQuery } from '../../core/models/api-response.model';
import { NotificationResponse } from '../../core/models/notification.model';
import { NotificationCategory, NotificationStatus, Role } from '../../core/models/enums';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PaginatorComponent } from '../../shared/components/paginator/paginator.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';
import { HasRoleDirective } from '../../shared/directives/has-role.directive';
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-notification-center',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    PaginatorComponent,
    ModalComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    SpinnerComponent,
    HasRoleDirective,
    IconComponent,
  ],
  template: `
    <gt-page-header title="Notification Center" subtitle="Stay on top of grant activity.">
      <button class="btn btn-primary d-inline-flex align-items-center gap-2" *gtHasRole="[Role.GRANT_ADMIN, Role.ADMIN]" (click)="openCreate()">
        <gt-icon name="plus" [size]="16" /> Send notification
      </button>
    </gt-page-header>

    <div class="card border-0 shadow-sm rounded-4 p-3 mb-4 d-flex flex-row align-items-center justify-content-between flex-wrap gap-3">
      <div class="d-flex align-items-center gap-2">
        <span class="d-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-3" style="width:40px;height:40px;"><gt-icon name="bell" [size]="18" /></span>
        <div>
          <div class="fw-bold text-dark lh-1">{{ unreadCount() }}</div>
          <div class="small text-secondary">unread notification{{ unreadCount() === 1 ? '' : 's' }}</div>
        </div>
      </div>
      <div class="d-flex align-items-center gap-2">
        <label class="form-label mb-0 fw-semibold text-secondary small">Status</label>
        <select class="form-select" style="max-width: 180px; border-radius:10px;" [value]="statusFilter()" (change)="onStatus($event)">
          <option value="UNREAD">Unread</option>
          <option value="READ">Read</option>
          <option value="DISMISSED">Dismissed</option>
        </select>
      </div>
    </div>

    <div class="d-flex flex-wrap gap-2 mb-4">
      @for (c of categoryChips; track c.value) {
        <button
          type="button"
          class="btn btn-sm rounded-pill px-3 fw-semibold"
          [class.btn-primary]="categoryFilter() === c.value"
          [class.btn-outline-secondary]="categoryFilter() !== c.value"
          (click)="onCategory(c.value)">
          {{ c.label }}
        </button>
      }
    </div>

    <div class="card border-0 shadow-sm rounded-4">
      @if (loading()) {
        <div class="p-5"><gt-spinner label="Loading…" /></div>
      } @else if (!filteredRows().length) {
        <gt-empty-state
          icon="bell"
          title="No notifications"
          subtitle="You're all caught up." />
      } @else {
        <ul class="list-group list-group-flush">
          @for (n of filteredRows(); track n.id) {
            <li class="list-group-item py-3 px-4 border-bottom border-start border-4"
                [class.border-primary]="n.status === 'UNREAD'"
                [class.border-transparent]="n.status !== 'UNREAD'">
              <div class="d-flex align-items-center gap-2 mb-2">
                <gt-status-badge [status]="n.category" />
                @if (n.status === 'UNREAD') { <span class="badge rounded-pill bg-primary">New</span> }
                <span class="small text-secondary ms-auto">{{ n.createdAt ? (n.createdAt | date: 'medium') : '—' }}</span>
              </div>
              <p class="mb-3 text-dark">{{ n.message }}</p>
              <div class="d-flex gap-2">
                @if (n.status === 'UNREAD') {
                  <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 d-inline-flex align-items-center gap-1" (click)="markRead(n)"><gt-icon name="check" [size]="14" /> Mark read</button>
                }
                <button class="btn btn-sm btn-light rounded-pill px-3 text-secondary d-inline-flex align-items-center gap-1" (click)="dismiss(n)"><gt-icon name="x" [size]="14" /> Dismiss</button>
              </div>
            </li>
          }
        </ul>
      }

      <div class="card-footer bg-white border-top px-4 py-3">
        <gt-paginator
          [page]="query().page || 0"
          [size]="query().size || 20"
          [totalElements]="total()"
          [totalPages]="totalPages()"
          (pageChange)="onPage($event)"
          (sizeChange)="onSize($event)" />
      </div>
    </div>

    <gt-modal [open]="modalOpen()" title="Send Notification" [width]="560" (closed)="modalOpen.set(false)">
      <form [formGroup]="form" id="notificationForm" (ngSubmit)="save()">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label" for="userId">User ID <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('userId')">
              <span class="input-group-text"><gt-icon name="user" [size]="16" /></span>
              <input type="number" class="form-control py-2" formControlName="userId" id="userId" [class.is-invalid]="invalid('userId')" placeholder="e.g. 42" />
            </div>
            @if (invalid('userId')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> User ID is required.</div> }
          </div>
          <div class="col-md-6">
            <label class="form-label" for="category">Category <span class="text-danger">*</span></label>
            <div class="input-group gt-input" [class.is-invalid]="invalid('category')">
              <span class="input-group-text"><gt-icon name="layers" [size]="16" /></span>
              <select class="form-select" formControlName="category" id="category" [class.is-invalid]="invalid('category')">
                <option value="" disabled>Select category…</option>
                @for (c of categories; track c) { <option [value]="c">{{ c }}</option> }
              </select>
            </div>
            @if (invalid('category')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Category is required.</div> }
          </div>
          <div class="col-12">
            <label class="form-label" for="message">Message <span class="text-danger">*</span></label>
            <textarea class="form-control" rows="3" formControlName="message" id="message" [class.is-invalid]="invalid('message')" placeholder="Notification message…" style="border-radius:10px;"></textarea>
            @if (invalid('message')) { <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Message is required.</div> }
          </div>
        </div>
      </form>

      <div footer class="d-flex justify-content-end gap-2">
        <button class="btn btn-light" (click)="modalOpen.set(false)">Cancel</button>
        <button class="btn btn-primary px-4 d-inline-flex align-items-center gap-2" form="notificationForm" type="submit" [disabled]="saving()">
          @if (saving()) { <span class="spinner-border spinner-border-sm"></span> Sending… } @else { <gt-icon name="send" [size]="16" /> Send notification }
        </button>
      </div>
    </gt-modal>
  `,
  styles: [
    `
      /* Using Bootstrap 5 utility classes for layout styling */
    `,
  ],
})
export class NotificationCenterComponent implements OnInit {
  private api = inject(NotificationService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  protected readonly Role = Role;

  readonly rows = signal<NotificationResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly unreadCount = signal(0);

  readonly query = signal<PageQuery>({ page: 0, size: 20, status: 'UNREAD' });
  readonly statusFilter = computed<NotificationStatus>(
    () => (this.query()['status'] as NotificationStatus) ?? 'UNREAD',
  );
  readonly categoryFilter = signal<NotificationCategory | 'ALL'>('ALL');
  readonly modalOpen = signal(false);

  readonly filteredRows = computed(() => {
    const cat = this.categoryFilter();
    const all = this.rows();
    return cat === 'ALL' ? all : all.filter((n) => n.category === cat);
  });

  readonly categories: NotificationCategory[] = [
    'APPLICATION',
    'REVIEW',
    'AWARD',
    'DISBURSEMENT',
    'PROGRESS',
    'OUTPUT',
  ];

  readonly categoryChips: { value: NotificationCategory | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All' },
    { value: 'APPLICATION', label: 'Application' },
    { value: 'REVIEW', label: 'Review' },
    { value: 'AWARD', label: 'Award' },
    { value: 'DISBURSEMENT', label: 'Disbursement' },
    { value: 'PROGRESS', label: 'Progress' },
    { value: 'OUTPUT', label: 'Output' },
  ];

  readonly form = this.fb.nonNullable.group({
    userId: [null as number | null, [Validators.required]],
    message: ['', [Validators.required]],
    category: ['' as NotificationCategory | '', [Validators.required]],
  });

  ngOnInit(): void {
    this.load();
    this.refreshUnread();
  }

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

  refreshUnread(): void {
    this.api.unreadCount().subscribe((r) => {
      this.unreadCount.set(r.data);
      this.api.unread.set(r.data ?? 0); // keep the shell bell badge in sync
    });
  }

  onStatus(e: Event): void {
    const status = (e.target as HTMLSelectElement).value;
    this.query.update((q) => ({ ...q, page: 0, status }));
    this.load();
  }

  onCategory(value: NotificationCategory | 'ALL'): void {
    this.categoryFilter.set(value);
  }

  onPage(page: number): void {
    this.query.update((q) => ({ ...q, page }));
    this.load();
  }
  onSize(size: number): void {
    this.query.update((q) => ({ ...q, size, page: 0 }));
    this.load();
  }

  markRead(n: NotificationResponse): void {
    this.api.markRead(n.id).subscribe(() => {
      this.toast.success('Notification marked as read.');
      this.refreshUnread();
      this.load();
    });
  }

  dismiss(n: NotificationResponse): void {
    this.api.dismiss(n.id).subscribe(() => {
      this.toast.success('Notification dismissed.');
      this.refreshUnread();
      this.load();
    });
  }

  openCreate(): void {
    this.form.reset({ userId: null, message: '', category: '' });
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = {
      userId: v.userId!,
      message: v.message,
      category: v.category as NotificationCategory,
    };
    this.saving.set(true);
    this.api.create(body).subscribe({
      next: () => {
        this.toast.success('Notification sent.');
        this.modalOpen.set(false);
        this.saving.set(false);
        this.refreshUnread();
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
