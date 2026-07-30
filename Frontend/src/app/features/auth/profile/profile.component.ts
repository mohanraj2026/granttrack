import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ROLE_LABELS } from '../../../core/models/enums';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, IconComponent],
  template: `
    <gt-page-header title="My Profile" subtitle="Account details and security." />

    <div class="row g-4">
      <!-- Account -->
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4">
            <div class="d-flex align-items-center gap-3 mb-4">
              <div class="d-flex align-items-center justify-content-center rounded-circle text-white fw-bold flex-shrink-0" style="width:60px;height:60px;background:var(--gt-gradient-primary);font-size:1.4rem;">
                {{ (user()?.name || '?').charAt(0) }}
              </div>
              <div class="min-w-0">
                <div class="h5 fw-bold text-dark mb-0 text-truncate">{{ user()?.name }}</div>
                <div class="text-secondary small text-truncate">{{ user()?.email }}</div>
              </div>
            </div>
            <dl class="kv mb-0">
              <dt>Phone</dt><dd>{{ user()?.phone || '—' }}</dd>
              <dt>Department</dt><dd>{{ user()?.department || '—' }}</dd>
              <dt>Status</dt><dd><span class="badge bg-success-subtle text-success rounded-pill">{{ user()?.status }}</span></dd>
              <dt>Roles</dt>
              <dd class="d-flex gap-1 flex-wrap">
                @for (r of user()?.roles ?? []; track r) { <span class="badge bg-primary rounded-pill">{{ roleLabel(r) }}</span> }
              </dd>
            </dl>
          </div>
        </div>
      </div>

      <!-- Change password -->
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4">
            <h2 class="h5 fw-bold text-dark mb-1">Change password</h2>
            <p class="text-secondary small mb-4">Use a strong password of at least 8 characters.</p>
            <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
              <div class="mb-3">
                <label class="form-label" for="curPw">Current password <span class="text-danger">*</span></label>
                <div class="input-group gt-input">
                  <span class="input-group-text"><gt-icon name="lock" [size]="16" /></span>
                  <input #cpw type="password" id="curPw" class="form-control py-2" formControlName="currentPassword" autocomplete="current-password" placeholder="Enter current password" />
                  <button type="button" class="gt-eye" tabindex="-1" (click)="cpw.type = cpw.type === 'password' ? 'text' : 'password'" aria-label="Toggle password visibility"><gt-icon name="eye" [size]="16" /></button>
                </div>
              </div>
              <div class="mb-4">
                <label class="form-label" for="newPw">New password <span class="text-danger">*</span></label>
                <div class="input-group gt-input" [class.is-invalid]="form.get('newPassword')?.touched && form.get('newPassword')?.invalid">
                  <span class="input-group-text"><gt-icon name="lock" [size]="16" /></span>
                  <input #npw type="password" id="newPw" class="form-control py-2" formControlName="newPassword" autocomplete="new-password" placeholder="At least 8 characters"
                         [class.is-invalid]="form.get('newPassword')?.touched && form.get('newPassword')?.invalid" />
                  <button type="button" class="gt-eye" tabindex="-1" (click)="npw.type = npw.type === 'password' ? 'text' : 'password'" aria-label="Toggle password visibility"><gt-icon name="eye" [size]="16" /></button>
                </div>
                @if (form.get('newPassword')?.touched && form.get('newPassword')?.invalid) {
                  <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> Minimum 8 characters.</div>
                }
              </div>
              <button type="submit" class="btn btn-primary d-inline-flex align-items-center gap-2" [disabled]="submitting() || form.invalid">
                @if (submitting()) { <span class="spinner-border spinner-border-sm"></span> Updating… } @else { <gt-icon name="check" [size]="16" /> Update password }
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ProfileComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  readonly user = this.auth.currentUser;
  readonly submitting = signal(false);

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.changePassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.toast.success('Password updated successfully.');
        this.form.reset();
        this.submitting.set(false);
      },
      error: () => this.submitting.set(false),
    });
  }

  roleLabel(r: string): string {
    return ROLE_LABELS[r] ?? r;
  }
}
