import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-forgot-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, IconComponent],
  template: `
    <div class="row g-0 min-vh-100">
      <!-- ============ Brand rail ============ -->
      <aside class="col-lg-5 col-xl-4 auth-rail d-none d-lg-flex flex-column p-5 text-white">
        <a routerLink="/" class="d-inline-flex align-items-center gap-2 text-white text-decoration-none fw-bold fs-5">
          <span class="auth-rail-mark"><gt-icon name="diamond" [size]="22" /></span> GrantTrack
        </a>

        <span class="d-inline-flex align-items-center gap-2 text-uppercase fw-semibold text-mint mt-5 mb-3" style="font-size:.72rem; letter-spacing:.14em;">
          <gt-icon name="lock" [size]="14" /> Account recovery
        </span>
        <h1 class="fw-bold lh-sm mb-3" style="font-size:clamp(1.7rem,2.5vw,2.35rem); max-width:15ch;">Let's get you back into your workspace.</h1>
        <p class="text-white-50 mb-auto lh-base" style="max-width:32rem;">
          Enter the email associated with your account and we'll start the secure password-reset
          process. For your protection we never reveal whether an address is registered.
        </p>

        <div class="d-flex align-items-center gap-2 mt-5 pt-4 border-top border-light border-opacity-10 text-white-50" style="font-size:.82rem;">
          <span class="text-mint"><gt-icon name="shield-check" [size]="16" /></span> Reset links are single-use and time-limited.
        </div>
      </aside>

      <!-- ============ Form ============ -->
      <main class="col d-flex align-items-center justify-content-center p-4 p-lg-5 bg-body">
        <div class="w-100" style="max-width:26rem;">
          <a routerLink="/" class="d-lg-none d-inline-flex align-items-center gap-2 fw-bold text-decoration-none mb-4 text-dark">
            <span class="auth-rail-mark sm"><gt-icon name="diamond" [size]="18" /></span> GrantTrack
          </a>

          @if (sent()) {
            <h1 class="fw-bold mb-1 text-dark">Check your inbox</h1>
            <p class="text-secondary mb-4">If an account exists for that email, reset instructions are on their way.</p>
            <div class="alert alert-success d-flex gap-2 border-0 align-items-start mb-4">
              <gt-icon name="check-circle" [size]="18" class="flex-shrink-0 mt-1" />
              <div class="small">We've initiated a password reset. Follow the link in the email to choose a new password — it may take a few minutes to arrive.</div>
            </div>
            <a routerLink="/login" class="btn btn-primary w-100 py-2 d-flex align-items-center justify-content-center gap-1">
              <gt-icon name="chevron-left" [size]="17" /> Back to sign in
            </a>
          } @else {
            <h1 class="fw-bold mb-1 text-dark">Forgot password</h1>
            <p class="text-secondary mb-4">Enter your email and we'll send reset instructions.</p>

            <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
              <div class="mb-4">
                <label class="form-label" for="emailInput">Email address <span class="text-danger">*</span></label>
                <div class="input-group gt-input" [class.is-invalid]="form.get('email')?.invalid && form.get('email')?.touched">
                  <span class="input-group-text"><gt-icon name="mail" [size]="17" /></span>
                  <input type="email" id="emailInput" class="form-control py-2" formControlName="email"
                         [class.is-invalid]="form.get('email')?.invalid && form.get('email')?.touched" placeholder="you@institution.edu" autocomplete="username" />
                </div>
                @if (form.get('email')?.invalid && form.get('email')?.touched) {
                  <div class="d-flex align-items-center gap-1 text-danger small mt-1"><gt-icon name="alert-triangle" [size]="13" /> A valid email is required.</div>
                }
              </div>

              <button type="submit" class="btn btn-primary w-100 py-2 d-flex align-items-center justify-content-center gap-1" [disabled]="submitting() || form.invalid">
                @if (submitting()) {
                  <span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Submitting…
                } @else {
                  Send reset link <gt-icon name="send" [size]="16" />
                }
              </button>
            </form>

            <p class="text-center mt-4 mb-0 small"><a routerLink="/login" class="text-decoration-none fw-semibold">Back to sign in</a></p>
          }
        </div>
      </main>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly sent = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.auth.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.sent.set(true);
        this.submitting.set(false);
      },
      error: () => this.submitting.set(false),
    });
  }
}
