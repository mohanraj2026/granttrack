import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApplicationsService } from '../applications.service';
import { FundingService } from '../../funding/funding.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  ApplicationBudgetResponse,
  CoInvestigatorResponse,
  GrantApplicationResponse,
} from '../../../core/models/application.model';
import { UserResponse } from '../../../core/models/user.model';
import { GrantCallResponse, InstitutionResponse } from '../../../core/models/funding.model';
import { UserAdminService } from '../../users/users.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-application-wizard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, IconComponent],
  templateUrl: './application-wizard.component.html',
  animations: [
    trigger('stepSlide', [
      transition(':enter', [
        style({ transform: 'translateX(20px)', opacity: 0 }),
        animate('300ms cubic-bezier(0.25, 0.8, 0.25, 1)', style({ transform: 'none', opacity: 1 }))
      ])
    ])
  ]
})
export class ApplicationWizardComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(ApplicationsService);
  private funding = inject(FundingService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly step = signal(1);
  readonly appId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly loading = signal(false);

  // Abstract document upload state
  readonly uploadedName = signal<string | null>(null);
  readonly uploading = signal(false);
  readonly docError = signal<string | null>(null);

  readonly calls = signal<GrantCallResponse[]>([]);
  readonly institutions = signal<InstitutionResponse[]>([]);
  readonly researchers = signal<UserResponse[]>([]);
  readonly coInvestigators = signal<CoInvestigatorResponse[]>([]);
  readonly budgets = signal<ApplicationBudgetResponse[]>([]);

  readonly budgetHeads = ['PERSONNEL', 'EQUIPMENT', 'TRAVEL', 'CONSUMABLES', 'OVERHEAD', 'SUBCONTRACT'];
  readonly coiRoles = ['CO_INVESTIGATOR', 'RESEARCH_ASSISTANT', 'INDUSTRIAL_PARTNER'];

  readonly budgetTotal = computed(() => this.budgets().reduce((sum, b) => sum + Number(b.amount), 0));

  readonly metaForm = this.fb.nonNullable.group({
    callId: [null as number | null, [Validators.required]],
    projectTitle: ['', [Validators.required, Validators.maxLength(300)]],
    researchAbstract: [''],
    discipline: [''],
    requestedAmount: [0, [Validators.required, Validators.min(1)]],
    projectDurationMonths: [null as number | null],
    institutionId: [null as number | null],
  }, { validators: this.amountValidator.bind(this) });

  amountValidator(g: any) {
    const callId = g.get('callId').value;
    const reqAmount = g.get('requestedAmount').value;
    if (callId && reqAmount) {
      const call = this.calls().find(c => c.id === callId);
      if (call && call.schemeMaxAwardAmount && reqAmount > call.schemeMaxAwardAmount) {
        return { amountExceedsMax: true };
      }
    }
    return null;
  }

  maxAllowed(): number {
    const callId = this.metaForm.value.callId;
    if (!callId) return 0;
    const call = this.calls().find(c => c.id === callId);
    return call?.schemeMaxAwardAmount || 0;
  }

  private userAdminApi = inject(UserAdminService);

  readonly coiForm = this.fb.nonNullable.group({
    userSearch: [''],
    userId: [null as number | null],
    institutionId: [null as number | null],
    role: ['CO_INVESTIGATOR', [Validators.required]],
    contribution: [''],
  });

  readonly budgetForm = this.fb.nonNullable.group({
    budgetHead: ['PERSONNEL', [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    justification: [''],
  });

  ngOnInit(): void {
    // Reference data (open calls + institutions).
    const preselectCallId = this.route.snapshot.queryParamMap.get('callId');
    this.funding.listCalls({ size: 200, status: 'OPEN' }).subscribe((r) => {
      this.calls.set(r.data.content);
      // Preselect the call when arriving from "Apply" on the Opportunities page.
      if (preselectCallId && !this.appId()) {
        const id = Number(preselectCallId);
        if (r.data.content.some((c) => c.id === id)) {
          this.metaForm.patchValue({ callId: id });
        }
      }
    });
    this.funding.listInstitutions({ size: 200 }).subscribe((r) => this.institutions.set(r.data.content));
    this.userAdminApi.lookupResearchers('').subscribe((r) => this.researchers.set(r.data));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.appId.set(Number(idParam));
      this.loadExisting(Number(idParam));
    }
  }

  onDocSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.docError.set(null);

    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!['pdf', 'doc', 'docx'].includes(ext)) {
      this.docError.set('Only PDF, DOC or DOCX files are allowed.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.docError.set('File exceeds the 10 MB limit.');
      return;
    }
    const id = this.appId();
    if (!id) {
      this.docError.set('Save the proposal details first.');
      return;
    }
    this.uploading.set(true);
    this.api.uploadAbstract(id, file).subscribe({
      next: (r) => {
        this.uploadedName.set(r.data.abstractDocName ?? file.name);
        this.uploading.set(false);
        this.toast.success('Abstract document uploaded.');
      },
      error: () => this.uploading.set(false),
    });
  }

  private loadExisting(id: number): void {
    this.loading.set(true);
    forkJoin({
      app: this.api.get(id),
      cois: this.api.listCoInvestigators(id),
      budgets: this.api.listBudgets(id),
    }).subscribe({
      next: ({ app, cois, budgets }) => {
        const a = app.data;
        if (a.status !== 'DRAFT') {
          this.toast.warning('Only draft applications can be edited.');
          this.router.navigate(['/applications', id]);
          return;
        }
        this.metaForm.patchValue({
          callId: a.callId,
          projectTitle: a.projectTitle,
          researchAbstract: a.researchAbstract ?? '',
          discipline: a.discipline ?? '',
          requestedAmount: a.requestedAmount,
          projectDurationMonths: a.projectDurationMonths ?? null,
          institutionId: a.institutionId ?? null,
        });
        this.coInvestigators.set(cois.data);
        this.budgets.set(budgets.data);
        this.uploadedName.set(a.abstractDocName ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** Step 1 → create/update the draft, then advance. */
  saveMetaAndContinue(): void {
    if (this.metaForm.invalid) {
      this.metaForm.markAllAsTouched();
      return;
    }
    const v = this.metaForm.getRawValue();
    const body = {
      callId: v.callId!,
      projectTitle: v.projectTitle,
      researchAbstract: v.researchAbstract || undefined,
      discipline: v.discipline || undefined,
      requestedAmount: v.requestedAmount,
      projectDurationMonths: v.projectDurationMonths ?? undefined,
      institutionId: v.institutionId ?? undefined,
    };
    this.saving.set(true);
    const id = this.appId();
    const req = id ? this.api.update(id, body) : this.api.create(body);
    req.subscribe({
      next: (r) => {
        this.appId.set(r.data.id);
        this.saving.set(false);
        this.toast.success('Draft saved.');
        this.step.set(2);
      },
      error: () => this.saving.set(false),
    });
  }

  onResearcherSelected(event: any): void {
    const val = event.target.value;
    // Extract ID from value (e.g. "Jane Smith (jane@ex.edu) - ID: 123")
    const match = val.match(/ID:\s*(\d+)$/);
    if (match) {
      const id = Number(match[1]);
      const user = this.researchers().find(u => u.id === id);
      if (user) {
        this.coiForm.patchValue({ userId: user.id, institutionId: user.institutionId ?? null });
        return;
      }
    }
    this.coiForm.patchValue({ userId: null });
  }

  addCoInvestigator(): void {
    const id = this.appId();
    if (!id || this.coiForm.invalid) return;
    const v = this.coiForm.getRawValue();
    this.api
      .addCoInvestigator(id, {
        userId: v.userId ?? undefined,
        institutionId: v.institutionId ?? undefined,
        role: v.role as never,
        contribution: v.contribution || undefined,
      })
      .subscribe((r) => {
        this.coInvestigators.update((list) => [...list, r.data]);
        this.coiForm.reset({ role: 'CO_INVESTIGATOR', userSearch: '' });
        this.toast.success('Team member added.');
      });
  }

  removeCoInvestigator(id: number): void {
    if (!confirm('Remove this team member?')) return;
    const appId = this.appId();
    if (!appId) return;
    this.api.removeCoInvestigator(appId, id).subscribe(() => {
      this.coInvestigators.update(list => list.filter(c => c.id !== id));
      this.toast.success('Team member removed.');
    });
  }

  addBudget(): void {
    const id = this.appId();
    if (!id || this.budgetForm.invalid) return;
    const v = this.budgetForm.getRawValue();

    const currentTotal = this.budgetTotal();
    const requested = this.metaForm.value.requestedAmount || 0;
    if (currentTotal + v.amount > requested) {
      this.toast.error(`Budget exceeds the requested amount (${requested}).`);
      return;
    }

    this.api
      .addBudget(id, { budgetHead: v.budgetHead as never, amount: v.amount, justification: v.justification || undefined })
      .subscribe((r) => {
        this.budgets.update((list) => [...list, r.data]);
        this.budgetForm.reset({ budgetHead: 'PERSONNEL', amount: 0 });
        this.toast.success('Budget line added.');
      });
  }

  removeBudget(id: number): void {
    if (!confirm('Remove this budget line?')) return;
    const appId = this.appId();
    if (!appId) return;
    this.api.removeBudget(appId, id).subscribe(() => {
      this.budgets.update(list => list.filter(b => b.id !== id));
      this.toast.success('Budget line removed.');
    });
  }

  submitApplication(): void {
    const id = this.appId();
    if (!id) return;
    if (!confirm('Submit this application? It cannot be edited afterwards.')) return;
    this.saving.set(true);
    this.api.submit(id).subscribe({
      next: () => {
        this.toast.success('Application submitted successfully.');
        this.router.navigate(['/applications', id]);
      },
      error: () => this.saving.set(false),
    });
  }

  goStep(n: number): void {
    // Steps 2-4 require a saved draft.
    if (n > 1 && !this.appId()) {
      this.toast.warning('Save the proposal details first.');
      return;
    }
    this.step.set(n);
  }

  callLabel(id: number | null): string {
    return this.calls().find((c) => c.id === id)?.callTitle ?? '—';
  }
  metaInvalid(ctrl: string): boolean {
    const c = this.metaForm.get(ctrl);
    return !!c && c.invalid && c.touched;
  }
}
