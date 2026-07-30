import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

export const matchPasswordValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword ? { passwordMismatch: true } : null;
};
import { UserAdminService } from './users.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { PageQuery } from '../../core/models/api-response.model';
import { CreatedUserResponse, UserResponse } from '../../core/models/user.model';
import { ROLE_LABELS, Role, UserStatus } from '../../core/models/enums';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { DataTableComponent, ColumnDef } from '../../shared/components/data-table/data-table.component';
import { PaginatorComponent } from '../../shared/components/paginator/paginator.component';
import { SearchFilterBarComponent } from '../../shared/components/search-filter-bar/search-filter-bar.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DropdownMenuComponent } from '../../shared/components/dropdown-menu/dropdown-menu.component';
import { FundingService } from '../funding/funding.service';
import { InstitutionResponse } from '../../core/models/funding.model';

type UserRow = UserResponse & { rolesText: string };

@Component({
  selector: 'gt-user-admin',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, PageHeaderComponent, DataTableComponent,
    PaginatorComponent, SearchFilterBarComponent, ModalComponent, IconComponent,
    DropdownMenuComponent
  ],
  templateUrl: './user-admin.component.html',
  styleUrl: './user-admin.component.scss',
})
export class UserAdminComponent implements OnInit {
  private api = inject(UserAdminService);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private fundingApi = inject(FundingService);

  readonly rows = signal<UserRow[]>([]);
  readonly institutions = signal<InstitutionResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly query = signal<PageQuery>({ page: 0, size: 20, sort: 'name,asc' });

  readonly createOpen = signal(false);
  readonly created = signal<CreatedUserResponse | null>(null);

  readonly editOpen = signal(false);
  readonly editingUser = signal<UserRow | null>(null);

  readonly deleteOpen = signal(false);
  readonly userToDelete = signal<UserRow | null>(null);
  readonly deleting = signal(false);

  readonly isAdmin = computed(() => this.auth.roles().includes(Role.ADMIN));

  /** Role options the current user is permitted to provision. */
  readonly roleOptions = computed(() => {
    const base = [
      { value: Role.RESEARCHER, label: 'Researcher (PI)' },
      { value: Role.REVIEWER, label: 'Peer Reviewer' },
      { value: Role.COMPLIANCE_OFFICER, label: 'Compliance Officer' },
    ];
    if (this.isAdmin()) {
      base.push(
        { value: Role.FINANCE_OFFICER, label: 'Finance Officer' },
        { value: Role.GRANT_ADMIN, label: 'Grant Administrator' },
      );
    }
    return base;
  });

  readonly columns: ColumnDef[] = [
    { key: 'formattedId', header: 'User ID' },
    { key: 'name', header: 'Name', sortable: true },
    { key: 'email', header: 'Email', sortable: true },
    { key: 'department', header: 'Department' },
    { key: 'rolesText', header: 'Roles' },
    { key: 'status', header: 'Status', type: 'badge' },
  ];

  readonly statusFilter = [
    { key: 'status', label: 'Status', options: [
      { value: 'ACTIVE', label: 'Active' },
      { value: 'INACTIVE', label: 'Inactive' },
    ] },
  ];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    institutionSearch: [''],
    institutionId: [null as number | null],
    department: [''],
    role: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  }, { validators: matchPasswordValidator });

  /** Edit form — details only (no password / role change). */
  readonly editForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    institutionSearch: [''],
    institutionId: [null as number | null],
    department: [''],
  });

  ngOnInit(): void {
    this.load();
    this.fundingApi.listInstitutions({ size: 1000 }).subscribe(res => this.institutions.set(res.data.content));
  }

  load(): void {
    this.loading.set(true);
    this.api.list(this.query()).subscribe({
      next: (r) => {
        this.rows.set(r.data.content.map((u) => ({ ...u, rolesText: this.rolesText(u) })));
        this.total.set(r.data.totalElements);
        this.totalPages.set(r.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private rolesText(u: UserResponse): string {
    return (u.roles ?? []).map((r) => ROLE_LABELS[r] ?? r).join(', ');
  }

  onFilter(params: Record<string, string>): void { this.query.update((q) => ({ page: 0, size: q.size, sort: q.sort, ...params })); this.load(); }
  onSort(sort: string): void { this.query.update((q) => ({ ...q, sort })); this.load(); }
  onPage(page: number): void { this.query.update((q) => ({ ...q, page })); this.load(); }
  onSize(size: number): void { this.query.update((q) => ({ ...q, size, page: 0 })); this.load(); }

  openCreate(): void {
    this.form.reset({ role: '', institutionSearch: '', institutionId: null, password: '', confirmPassword: '' });
    this.createOpen.set(true);
  }

  onInstitutionSelected(event: any): void {
    const val = event.target.value;
    const inst = this.institutions().find(i => i.name === val);
    if (inst) {
      this.form.patchValue({ institutionId: inst.id });
    } else {
      this.form.patchValue({ institutionId: null });
    }
  }

  submitCreate(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.api.createUser({
      name: v.name,
      email: v.email,
      phone: v.phone || undefined,
      institutionId: v.institutionId ?? undefined,
      department: v.department || undefined,
      role: v.role,
      password: v.password,
    }).subscribe({
      next: (r) => {
        this.saving.set(false);
        this.createOpen.set(false);
        this.created.set(r.data);
        this.toast.success('User account provisioned.');
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }


  roleLabel(role: string): string { return ROLE_LABELS[role] ?? role; }

  openEdit(u: UserRow): void {
    this.editingUser.set(u);
    const inst = this.institutions().find((i) => i.id === u.institutionId);
    this.editForm.reset({
      name: u.name,
      email: u.email,
      phone: u.phone ?? '',
      institutionSearch: inst?.name ?? '',
      institutionId: u.institutionId ?? null,
      department: u.department ?? '',
    });
    this.editOpen.set(true);
  }

  onEditInstitutionSelected(event: any): void {
    const val = event.target.value;
    const inst = this.institutions().find((i) => i.name === val);
    this.editForm.patchValue({ institutionId: inst ? inst.id : null });
  }

  submitEdit(): void {
    const u = this.editingUser();
    if (!u || this.editForm.invalid) { this.editForm.markAllAsTouched(); return; }
    const v = this.editForm.getRawValue();
    this.saving.set(true);
    this.api.updateUser(u.id, {
      name: v.name,
      email: v.email,
      phone: v.phone || undefined,
      institutionId: v.institutionId ?? undefined,
      department: v.department || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.editOpen.set(false);
        this.editingUser.set(null);
        this.toast.success('User updated.');
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  editInvalid(ctrl: string): boolean {
    const c = this.editForm.get(ctrl);
    return !!c && c.invalid && c.touched;
  }

  toggleStatus(u: UserRow): void {
    const next: UserStatus = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const verb = next === 'INACTIVE' ? 'Deactivate' : 'Activate';
    if (!confirm(`${verb} user "${u.name}"?`)) return;
    this.api.setStatus(u.id, next).subscribe(() => {
      this.toast.success(next === 'INACTIVE' ? 'User deactivated.' : 'User activated.');
      this.load();
    });
  }

  confirmDelete(u: UserRow): void {
    if (u.roles.includes(Role.ADMIN)) {
      this.toast.error('System administrator cannot be deleted.');
      return;
    }
    this.userToDelete.set(u);
    this.deleteOpen.set(true);
  }

  executeDelete(): void {
    const user = this.userToDelete();
    if (!user) return;
    this.deleting.set(true);
    this.api.deleteUser(user.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteOpen.set(false);
        this.userToDelete.set(null);
        this.toast.success('User deleted successfully.');
        this.load();
      },
      error: () => this.deleting.set(false)
    });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
