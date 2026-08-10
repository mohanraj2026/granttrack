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
  templateUrl: './notification-center.component.html',
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
