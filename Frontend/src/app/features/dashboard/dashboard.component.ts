import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { ROLE_LABELS, Role } from '../../core/models/enums';
import { ApiResponse, PageResponse } from '../../core/models/api-response.model';
import { NotificationResponse } from '../../core/models/notification.model';
import { IconComponent, IconName } from '../../shared/components/icon/icon.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ApplicationsService } from '../applications/applications.service';
import { AwardService } from '../awards/awards.service';
import { ReviewService } from '../reviews/reviews.service';
import { ProgressService } from '../progress/progress.service';
import { OutputService } from '../outputs/outputs.service';
import { FundingService } from '../funding/funding.service';
import { NotificationService } from '../notifications/notifications.service';

type Tone = 'primary' | 'success' | 'warning' | 'info' | 'danger';
interface Kpi { icon: IconName; tone: Tone; value: number; label: string; }
interface RecentItem { primary: string; secondary?: string; status?: string; amount?: number | null; link: unknown[]; }
interface DashView { focus: string; cta: { label: string; path: string }; panelTitle: string; panelLink: string; }

interface QuickLink { title: string; desc: string; icon: IconName; path: string; roles?: Role[]; }

@Component({
  selector: 'gt-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, IconComponent, StatusBadgeComponent, SpinnerComponent, EmptyStateComponent],
  template: `
    <div class="container-fluid py-4">
      <!-- ============ Header ============ -->
      <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold text-dark mb-1">{{ greeting() }}</h1>
          <p class="text-secondary mb-2">{{ view().focus }}</p>
          <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="text-secondary small fw-semibold text-uppercase">Signed in as</span>
            @for (r of user()?.roles ?? []; track r) {
              <span class="badge bg-primary rounded-pill px-3 py-2">{{ roleLabel(r) }}</span>
            }
          </div>
        </div>
        <a [routerLink]="view().cta.path" class="btn btn-primary d-inline-flex align-items-center gap-2">
          {{ view().cta.label }} <gt-icon name="chevron-right" [size]="16" />
        </a>
      </div>

      <!-- ============ KPI summary (live counts) ============ -->
      <div class="row g-3 g-xl-4 mb-4">
        @if (loading()) {
          @for (s of [1,2,3,4]; track s) {
            <div class="col-sm-6 col-xl-3">
              <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body p-4">
                <div class="placeholder-glow"><span class="placeholder col-4 rounded-3" style="height:48px;"></span></div>
                <div class="placeholder-glow mt-3"><span class="placeholder col-3"></span></div>
                <div class="placeholder-glow"><span class="placeholder col-7"></span></div>
              </div></div>
            </div>
          }
        } @else {
          @for (k of kpis(); track k.label) {
            <div class="col-sm-6 col-xl-3">
              <div class="card border-0 shadow-sm rounded-4 h-100 gt-hover-lift">
                <div class="card-body p-4">
                  <div class="d-flex justify-content-between align-items-start mb-3">
                    <span class="d-flex align-items-center justify-content-center bg-{{ k.tone }} bg-opacity-10 text-{{ k.tone }} rounded-3" style="width:48px;height:48px;"><gt-icon [name]="k.icon" [size]="22" /></span>
                  </div>
                  <div class="h3 fw-bold text-dark mb-0">{{ k.value }}</div>
                  <div class="text-secondary small fw-semibold">{{ k.label }}</div>
                </div>
              </div>
            </div>
          }
        }
      </div>

      <div class="row g-3 g-xl-4">
        <!-- ============ Left: role-relevant recent records ============ -->
        <div class="col-xl-8">
          <div class="card border-0 shadow-sm rounded-4">
            <div class="card-header bg-white border-0 pt-4 px-4 pb-2 d-flex justify-content-between align-items-center">
              <h2 class="h6 fw-bold text-dark mb-0">{{ view().panelTitle }}</h2>
              <a [routerLink]="view().panelLink" class="small fw-semibold text-decoration-none">View all</a>
            </div>
            @if (loading()) {
              <div class="p-5"><gt-spinner label="Loading…" /></div>
            } @else {
              <div class="list-group list-group-flush">
                @for (it of recent(); track $index) {
                  <a class="list-group-item list-group-item-action border-0 border-bottom px-4 py-3 d-flex align-items-center gap-3 text-decoration-none" [routerLink]="it.link">
                    <div class="flex-grow-1" style="min-width:0;">
                      <div class="fw-semibold text-dark text-truncate">{{ it.primary }}</div>
                      @if (it.secondary) { <div class="text-secondary small text-truncate">{{ it.secondary }}</div> }
                    </div>
                    @if (it.amount != null) { <span class="fw-semibold text-dark small text-nowrap">{{ it.amount | currency: 'INR' : 'symbol' : '1.0-0' }}</span> }
                    @if (it.status) { <gt-status-badge [status]="it.status" /> }
                  </a>
                } @empty {
                  <div class="p-4"><gt-empty-state icon="inbox" [title]="emptyTitle()" subtitle="Items will appear here as they're created." /></div>
                }
              </div>
            }
          </div>
        </div>

        <!-- ============ Right: real notification activity ============ -->
        <div class="col-xl-4">
          <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-header bg-white border-0 pt-4 px-4 pb-2 d-flex justify-content-between align-items-center">
              <h2 class="h6 fw-bold text-dark mb-0">Recent activity</h2>
              <a routerLink="/notifications" class="small fw-semibold text-decoration-none">View all</a>
            </div>
            @if (loading()) {
              <div class="p-5"><gt-spinner label="Loading…" /></div>
            } @else {
              <div class="list-group list-group-flush">
                @for (n of activity(); track n.id) {
                  <div class="list-group-item border-0 border-bottom px-4 py-3 d-flex gap-3">
                    <span class="d-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle flex-shrink-0" style="width:34px;height:34px;"><gt-icon name="bell" [size]="15" /></span>
                    <div style="min-width:0;">
                      <div class="small text-dark">{{ n.message }}</div>
                      <div class="text-secondary" style="font-size:.75rem;">{{ n.createdAt ? (n.createdAt | date: 'medium') : '' }}</div>
                    </div>
                  </div>
                } @empty {
                  <div class="p-4"><gt-empty-state icon="bell" title="No activity yet" subtitle="Your notifications will show here." /></div>
                }
              </div>
            }
          </div>
        </div>
      </div>

      <!-- ============ Quick actions (role-based) ============ -->
      <h2 class="h6 fw-bold text-dark mt-4 mb-3">Quick actions</h2>
      <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-3">
        @for (link of visibleLinks(); track link.path) {
          <div class="col">
            <a class="card border-0 shadow-sm rounded-4 h-100 text-decoration-none gt-hover-lift" [routerLink]="link.path">
              <div class="card-body d-flex align-items-center gap-3 p-3">
                <span class="d-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-3 flex-shrink-0" style="width:46px;height:46px;"><gt-icon [name]="link.icon" [size]="20" /></span>
                <div class="flex-grow-1" style="min-width:0;">
                  <div class="fw-bold text-dark text-truncate">{{ link.title }}</div>
                  <div class="text-secondary small text-truncate">{{ link.desc }}</div>
                </div>
                <gt-icon name="chevron-right" [size]="18" class="text-secondary opacity-50 flex-shrink-0" />
              </div>
            </a>
          </div>
        }
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private auth = inject(AuthService);
  private apps = inject(ApplicationsService);
  private awards = inject(AwardService);
  private reviews = inject(ReviewService);
  private progress = inject(ProgressService);
  private outputs = inject(OutputService);
  private funding = inject(FundingService);
  private notif = inject(NotificationService);

  readonly user = this.auth.currentUser;
  readonly loading = signal(true);
  readonly kpis = signal<Kpi[]>([]);
  readonly recent = signal<RecentItem[]>([]);
  readonly activity = signal<NotificationResponse[]>([]);

  private role(): 'admin' | 'reviewer' | 'finance' | 'compliance' | 'researcher' {
    if (this.auth.hasAnyRole([Role.ADMIN, Role.GRANT_ADMIN])) return 'admin';
    if (this.auth.hasAnyRole([Role.REVIEWER])) return 'reviewer';
    if (this.auth.hasAnyRole([Role.FINANCE_OFFICER])) return 'finance';
    if (this.auth.hasAnyRole([Role.COMPLIANCE_OFFICER])) return 'compliance';
    return 'researcher';
  }

  readonly view = computed<DashView>(() => {
    this.user();
    switch (this.role()) {
      case 'admin':
        return { focus: 'Programme-wide activity across the grant lifecycle.', cta: { label: 'Manage funding', path: '/funding' }, panelTitle: 'Recent applications', panelLink: '/applications' };
      case 'reviewer':
        return { focus: 'Your assigned proposals and review progress.', cta: { label: 'Open review queue', path: '/reviews' }, panelTitle: 'Your review queue', panelLink: '/reviews' };
      case 'finance':
        return { focus: 'Awards and milestones awaiting your action.', cta: { label: 'Go to disbursements', path: '/disbursements' }, panelTitle: 'Awards awaiting finance review', panelLink: '/awards' };
      case 'compliance':
        return { focus: 'Progress reports and deliverables to review.', cta: { label: 'Compliance desk', path: '/progress' }, panelTitle: 'Reports awaiting review', panelLink: '/progress/reports' };
      default:
        return { focus: 'The status of your grant applications and awards.', cta: { label: 'New application', path: '/applications' }, panelTitle: 'My applications', panelLink: '/applications' };
    }
  });

  readonly emptyTitle = computed(() => {
    switch (this.role()) {
      case 'reviewer': return 'No assignments yet';
      case 'finance': return 'Nothing awaiting review';
      case 'compliance': return 'Nothing to review';
      case 'admin': return 'No applications yet';
      default: return 'No applications yet';
    }
  });

  private readonly links: QuickLink[] = [
    { title: 'My Applications', desc: 'Create and track grant applications', icon: 'file-text', path: '/applications', roles: [Role.RESEARCHER] },
    { title: 'Review Queue', desc: 'Score applications assigned to you', icon: 'scale', path: '/reviews', roles: [Role.REVIEWER] },
    { title: 'Assignment Panel', desc: 'Assign reviewers & record decisions', icon: 'layers', path: '/reviews/assignments', roles: [Role.GRANT_ADMIN, Role.ADMIN] },
    { title: 'Funding Schemes', desc: 'Configure schemes and grant calls', icon: 'wallet', path: '/funding/schemes', roles: [Role.ADMIN, Role.GRANT_ADMIN] },
    { title: 'Awards', desc: 'Manage grant awards', icon: 'award', path: '/awards', roles: [Role.GRANT_ADMIN, Role.ADMIN, Role.FINANCE_OFFICER] },
    { title: 'Finance Queue', desc: 'Approve milestones & release funds', icon: 'landmark', path: '/disbursements', roles: [Role.FINANCE_OFFICER, Role.GRANT_ADMIN] },
    { title: 'Compliance Desk', desc: 'Review progress & deliverables', icon: 'shield-check', path: '/progress', roles: [Role.COMPLIANCE_OFFICER] },
    { title: 'Research Outputs', desc: 'Record publications, patents & datasets', icon: 'book', path: '/outputs', roles: [Role.RESEARCHER] },
    { title: 'User Administration', desc: 'Manage user accounts', icon: 'users', path: '/users', roles: [Role.ADMIN, Role.GRANT_ADMIN] },
    { title: 'Notifications', desc: 'Your in-app messages', icon: 'bell', path: '/notifications' },
  ];

  readonly visibleLinks = computed(() => {
    this.user();
    const isAdmin = this.auth.hasAnyRole([Role.ADMIN]);
    return this.links.filter((l) => isAdmin || this.auth.hasAnyRole(l.roles ?? []));
  });

  ngOnInit(): void {
    this.load();
  }

  private count(obs: Observable<ApiResponse<PageResponse<unknown>>>): Observable<number> {
    return obs.pipe(map((r) => r.data?.totalElements ?? 0), catchError(() => of(0)));
  }
  private items<T>(obs: Observable<ApiResponse<PageResponse<T>>>): Observable<T[]> {
    return obs.pipe(map((r) => r.data?.content ?? []), catchError(() => of([] as T[])));
  }
  private unread(): Observable<number> {
    return this.notif.unreadCount().pipe(map((r) => r.data ?? 0), catchError(() => of(0)));
  }

  private load(): void {
    this.loading.set(true);
    const activity$ = this.items(this.notif.list({ page: 0, size: 6, sort: 'createdAt,desc' }));

    switch (this.role()) {
      case 'admin':
        forkJoin({
          apps: this.count(this.apps.list({ page: 0, size: 1 })),
          review: this.count(this.apps.list({ page: 0, size: 1, status: 'UNDER_REVIEW' })),
          awards: this.count(this.awards.list({ page: 0, size: 1 })),
          calls: this.count(this.funding.listCalls({ page: 0, size: 1, status: 'OPEN' })),
          recent: this.items(this.apps.list({ page: 0, size: 5, sort: 'createdAt,desc' })),
          activity: activity$,
        }).subscribe((r) => {
          this.kpis.set([
            { icon: 'file-text', tone: 'primary', value: r.apps, label: 'Applications' },
            { icon: 'scale', tone: 'warning', value: r.review, label: 'Under review' },
            { icon: 'award', tone: 'success', value: r.awards, label: 'Awards' },
            { icon: 'sparkles', tone: 'info', value: r.calls, label: 'Open calls' },
          ]);
          this.recent.set(r.recent.map((a: any) => this.appItem(a)));
          this.finish(r.activity);
        });
        break;

      case 'reviewer':
        forkJoin({
          assigned: this.count(this.reviews.listAssignments({ page: 0, size: 1 })),
          awaiting: this.count(this.reviews.listAssignments({ page: 0, size: 1, status: 'ASSIGNED' })),
          submitted: this.count(this.reviews.listAssignments({ page: 0, size: 1, status: 'SUBMITTED' })),
          unread: this.unread(),
          recent: this.items(this.reviews.listAssignments({ page: 0, size: 5, sort: 'assignedDate,desc' })),
          activity: activity$,
        }).subscribe((r) => {
          this.kpis.set([
            { icon: 'clipboard', tone: 'primary', value: r.assigned, label: 'Assigned to me' },
            { icon: 'scale', tone: 'warning', value: r.awaiting, label: 'Awaiting response' },
            { icon: 'check-square', tone: 'success', value: r.submitted, label: 'Submitted' },
            { icon: 'bell', tone: 'info', value: r.unread, label: 'Unread notices' },
          ]);
          this.recent.set(r.recent.map((a: any) => ({
            primary: 'Application #' + String(a.applicationId).padStart(4, '0'),
            secondary: a.reviewDeadline ? 'Deadline ' + a.reviewDeadline : 'No deadline set',
            status: a.status,
            link: ['/reviews/assignments', a.id],
          })));
          this.finish(r.activity);
        });
        break;

      case 'finance':
        forkJoin({
          pending: this.count(this.awards.list({ page: 0, size: 1, financeReviewStatus: 'PENDING' })),
          accepted: this.count(this.awards.list({ page: 0, size: 1, financeReviewStatus: 'ACCEPTED' })),
          active: this.count(this.awards.list({ page: 0, size: 1, status: 'ACTIVE' })),
          unread: this.unread(),
          recent: this.items(this.awards.list({ page: 0, size: 5, financeReviewStatus: 'PENDING', sort: 'createdAt,desc' })),
          activity: activity$,
        }).subscribe((r) => {
          this.kpis.set([
            { icon: 'landmark', tone: 'warning', value: r.pending, label: 'Awaiting review' },
            { icon: 'check-square', tone: 'success', value: r.accepted, label: 'Accepted' },
            { icon: 'award', tone: 'primary', value: r.active, label: 'Active awards' },
            { icon: 'bell', tone: 'info', value: r.unread, label: 'Unread notices' },
          ]);
          this.recent.set(r.recent.map((a: any) => this.awardItem(a)));
          this.finish(r.activity);
        });
        break;

      case 'compliance':
        forkJoin({
          reports: this.count(this.progress.listReports({ page: 0, size: 1, status: 'SUBMITTED' })),
          deliverables: this.count(this.progress.listDeliverables({ page: 0, size: 1, status: 'SUBMITTED' })),
          approved: this.count(this.progress.listReports({ page: 0, size: 1, status: 'APPROVED' })),
          unread: this.unread(),
          recent: this.items(this.progress.listReports({ page: 0, size: 5, status: 'SUBMITTED', sort: 'createdAt,desc' })),
          activity: activity$,
        }).subscribe((r) => {
          this.kpis.set([
            { icon: 'file-text', tone: 'warning', value: r.reports, label: 'Reports to review' },
            { icon: 'clipboard', tone: 'primary', value: r.deliverables, label: 'Deliverables to review' },
            { icon: 'check-square', tone: 'success', value: r.approved, label: 'Approved reports' },
            { icon: 'bell', tone: 'info', value: r.unread, label: 'Unread notices' },
          ]);
          this.recent.set(r.recent.map((a: any) => ({
            primary: 'Progress report · Award #' + String(a.awardId).padStart(4, '0'),
            secondary: a.period ? 'Period ' + a.period : 'Submitted for review',
            status: a.status,
            link: ['/progress/reports'],
          })));
          this.finish(r.activity);
        });
        break;

      default: // researcher
        forkJoin({
          apps: this.count(this.apps.list({ page: 0, size: 1 })),
          awarded: this.count(this.apps.list({ page: 0, size: 1, status: 'AWARDED' })),
          outputs: this.count(this.outputs.listOutputs({ page: 0, size: 1 })),
          unread: this.unread(),
          recent: this.items(this.apps.list({ page: 0, size: 5, sort: 'createdAt,desc' })),
          activity: activity$,
        }).subscribe((r) => {
          this.kpis.set([
            { icon: 'file-text', tone: 'primary', value: r.apps, label: 'My applications' },
            { icon: 'award', tone: 'success', value: r.awarded, label: 'Awarded' },
            { icon: 'book', tone: 'info', value: r.outputs, label: 'Research outputs' },
            { icon: 'bell', tone: 'warning', value: r.unread, label: 'Unread notices' },
          ]);
          this.recent.set(r.recent.map((a: any) => this.appItem(a)));
          this.finish(r.activity);
        });
        break;
    }
  }

  private appItem(a: any): RecentItem {
    return {
      primary: a.projectTitle,
      secondary: 'APP' + String(a.id).padStart(4, '0') + (a.discipline ? ' · ' + a.discipline : ''),
      status: a.status,
      amount: a.requestedAmount ?? null,
      link: ['/applications', a.id],
    };
  }

  private awardItem(a: any): RecentItem {
    return {
      primary: 'Award · APP' + String(a.applicationId).padStart(4, '0'),
      secondary: a.startDate ? 'Starts ' + a.startDate : 'Awaiting finance review',
      status: a.financeReviewStatus,
      amount: a.awardedAmount ?? null,
      link: ['/awards', a.id],
    };
  }

  private finish(activity: NotificationResponse[]): void {
    this.activity.set(activity);
    this.loading.set(false);
  }

  greeting(): string {
    const name = this.user()?.name?.split(' ')[0] ?? 'there';
    const hr = new Date().getHours();
    const part = hr < 12 ? 'Good morning' : hr < 18 ? 'Good afternoon' : 'Good evening';
    return `${part}, ${name}`;
  }

  roleLabel(r: string): string {
    return ROLE_LABELS[r] ?? r;
  }
}
