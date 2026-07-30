import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { FundingService } from '../../funding/funding.service';
import { ToastService } from '../../../core/services/toast.service';
import { InstitutionResponse } from '../../../core/models/funding.model';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-register',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, IconComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private funding = inject(FundingService);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly institutions = signal<InstitutionResponse[]>([]);
  collegeIdFile: File | null = null;
  profilePhotoFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
    countryCode: ['+91'],
    phone: [''],
    institutionSearch: [''],
    institutionId: [null as number | null, [Validators.required]],
    department: [''],
    education: [''],
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(g: any) {
    return g.get('password').value === g.get('confirmPassword').value ? null : { mismatch: true };
  }

  ngOnInit() {
    this.funding.listInstitutions({ size: 200, sort: 'name,asc' }).subscribe(r => this.institutions.set(r.data.content));
  }

  onCollegeIdSelected(event: any) {
    this.collegeIdFile = event.target.files[0] || null;
  }
  onProfilePhotoSelected(event: any) {
    this.profilePhotoFile = event.target.files[0] || null;
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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const v = this.form.getRawValue();
    this.auth
      .register({
        name: v.name,
        email: v.email,
        password: v.password,
        phone: v.phone || undefined,
        countryCode: v.countryCode || undefined,
        institutionId: v.institutionId ?? undefined,
        department: v.department || undefined,
        education: v.education || undefined,
      }, this.collegeIdFile, this.profilePhotoFile)
      .subscribe({
        next: () => {
          this.toast.success('Registration successful. Please sign in.');
          this.router.navigate(['/login']);
        },
        error: () => this.submitting.set(false),
      });
  }

  invalid(ctrl: string): boolean {
    const c = this.form.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
