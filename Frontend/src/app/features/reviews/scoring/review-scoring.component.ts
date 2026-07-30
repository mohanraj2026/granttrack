import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ReviewService } from '../reviews.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  ReviewScoreRequest,
  ReviewScoreResponse,
  ReviewerAssignmentResponse,
} from '../../../core/models/review.model';
import { BlindApplicationResponse } from '../../../core/models/application.model';
import { ReviewCriterion, Role } from '../../../core/models/enums';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

interface CriterionDef {
  key: ReviewCriterion;
  label: string;
}

@Component({
  selector: 'gt-review-scoring',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    StatusBadgeComponent,
    SpinnerComponent,
    IconComponent,
    ModalComponent,
  ],
  templateUrl: './review-scoring.component.html',
})
export class ReviewScoringComponent implements OnInit {
  private api = inject(ReviewService);
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly declineOpen = signal(false);
  readonly declineReason = signal('');

  protected readonly Role = Role;

  readonly id = signal<number>(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly downloading = signal(false);
  readonly assignment = signal<ReviewerAssignmentResponse | null>(null);
  readonly blindApp = signal<BlindApplicationResponse | null>(null);
  readonly scores = signal<ReviewScoreResponse[]>([]);

  readonly criteria: CriterionDef[] = [
    { key: 'SCIENTIFIC_MERIT', label: 'Scientific Merit' },
    { key: 'FEASIBILITY', label: 'Feasibility' },
    { key: 'TEAM_EXPERTISE', label: 'Team Expertise' },
    { key: 'IMPACT', label: 'Impact' },
    { key: 'INNOVATION', label: 'Innovation' },
    { key: 'BUDGET_JUSTIFICATION', label: 'Budget Justification' },
  ];

  readonly conflictCleared = computed(
    () => this.assignment()?.conflictScreeningStatus === 'CLEAR',
  );
  readonly accepted = computed(() => this.assignment()?.status === 'ACCEPTED');
  readonly submitted = computed(() => this.assignment()?.status === 'SUBMITTED');
  readonly scoredKeys = computed(() => new Set(this.scores().map((s) => s.criterion)));
  readonly hasScores = computed(() => this.scores().length > 0);

  // One number control + comments per criterion, plus a single overall recommendation.
  readonly form = this.fb.nonNullable.group({
    SCIENTIFIC_MERIT: [null as number | null, [Validators.min(1), Validators.max(10)]],
    SCIENTIFIC_MERIT_comments: [''],
    FEASIBILITY: [null as number | null, [Validators.min(1), Validators.max(10)]],
    FEASIBILITY_comments: [''],
    TEAM_EXPERTISE: [null as number | null, [Validators.min(1), Validators.max(10)]],
    TEAM_EXPERTISE_comments: [''],
    IMPACT: [null as number | null, [Validators.min(1), Validators.max(10)]],
    IMPACT_comments: [''],
    INNOVATION: [null as number | null, [Validators.min(1), Validators.max(10)]],
    INNOVATION_comments: [''],
    BUDGET_JUSTIFICATION: [null as number | null, [Validators.min(1), Validators.max(10)]],
    BUDGET_JUSTIFICATION_comments: [''],
    overallRecommendation: ['FUND_AT_FULL_AMOUNT', [Validators.required]],
  });

  // Real-time average of any entered scores.
  private readonly formValue = signal(this.form.getRawValue());
  readonly average = computed(() => {
    const v = this.formValue();
    const vals = this.criteria
      .map((c) => v[c.key])
      .filter((n): n is number => n !== null && n !== undefined && !Number.isNaN(n));
    if (!vals.length) return 0;
    return vals.reduce((a, b) => a + b, 0) / vals.length;
  });

  ngOnInit(): void {
    this.id.set(Number(this.route.snapshot.paramMap.get('id')));
    this.form.valueChanges.subscribe(() => this.formValue.set(this.form.getRawValue()));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({
      list: this.api.listAssignments({ size: 200 }),
      scores: this.api.listScores(this.id()),
    }).subscribe({
      next: ({ list, scores }) => {
        const found = list.data.content.find((a) => a.id === this.id()) ?? null;
        this.assignment.set(found);
        this.scores.set(scores.data);
        this.patchFromScores(scores.data);
        
        if (found) {
          this.api.getBlindApplication(found.applicationId).subscribe({
            next: (res) => {
              this.blindApp.set(res.data);
              this.loading.set(false);
            },
            error: () => this.loading.set(false),
          });
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  private patchFromScores(scores: ReviewScoreResponse[]): void {
    for (const s of scores) {
      this.form.patchValue({ [s.criterion]: s.score, [`${s.criterion}_comments`]: s.comments ?? '' });
      if (s.overallRecommendation) {
        this.form.patchValue({ overallRecommendation: s.overallRecommendation });
      }
    }
    this.formValue.set(this.form.getRawValue());
  }

  private http = inject(HttpClient);
  
  downloadAbstract(): void {
    const app = this.blindApp();
    if (!app || !app.abstractDocName) return;
    this.downloading.set(true);
    
    this.http.get(`${environment.apiUrl}/applications/${app.id}/abstract-document`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = app.abstractDocName ?? 'abstract';
          a.click();
          URL.revokeObjectURL(url);
          this.downloading.set(false);
        },
        error: () => {
          this.toast.error('Failed to download abstract.');
          this.downloading.set(false);
        }
      });
  }

  isScored(key: ReviewCriterion): boolean {
    return this.scoredKeys().has(key);
  }

  declareNoConflict(): void {
    this.api.conflictCheck(this.id(), 'CLEAR').subscribe((r) => {
      this.assignment.set(r.data);
      this.toast.success('No conflict declared.');
    });
  }

  acceptAssignment(): void {
    this.api.respond(this.id(), 'ACCEPT').subscribe((r) => {
      this.assignment.set(r.data);
      this.toast.success('Assignment accepted.');
    });
  }

  openDecline(): void {
    this.declineReason.set('');
    this.declineOpen.set(true);
  }

  confirmDecline(): void {
    const reason = this.declineReason().trim();
    if (!reason) {
      this.toast.warning('Please provide a reason for declining.');
      return;
    }
    this.saving.set(true);
    this.api.respond(this.id(), 'DECLINE', reason).subscribe({
      next: () => {
        this.saving.set(false);
        this.declineOpen.set(false);
        this.toast.success('Assignment declined. The grant administrator has been notified.');
        this.router.navigate(['/reviews']);
      },
      error: () => this.saving.set(false),
    });
  }

  submitAllScores(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const done = this.scoredKeys();
    const requests = this.criteria
      .filter((c) => !done.has(c.key) && v[c.key] !== null && v[c.key] !== undefined)
      .map((c) => {
        const body: ReviewScoreRequest = {
          criterion: c.key,
          score: v[c.key] as number,
          comments: (v[`${c.key}_comments`] as string) || undefined,
          overallRecommendation: v.overallRecommendation as ReviewScoreRequest['overallRecommendation'],
        };
        return this.api.submitScore(this.id(), body);
      });

    if (!requests.length) {
      this.toast.warning('No new scores to submit.');
      return;
    }
    this.saving.set(true);
    forkJoin(requests).subscribe({
      next: () => {
        this.toast.success('Scores submitted.');
        this.saving.set(false);
        this.reloadScores();
      },
      error: () => this.saving.set(false),
    });
  }

  submitReview(): void {
    if (!confirm('Submit your review? Scores cannot be changed afterwards.')) return;
    this.saving.set(true);
    this.api.submitReview(this.id()).subscribe({
      next: (r) => {
        this.assignment.set(r.data);
        this.toast.success('Review submitted.');
        this.saving.set(false);
        this.reloadScores();
      },
      error: () => this.saving.set(false),
    });
  }

  private reloadScores(): void {
    this.api.listScores(this.id()).subscribe((r) => {
      this.scores.set(r.data);
      this.patchFromScores(r.data);
    });
  }
}
