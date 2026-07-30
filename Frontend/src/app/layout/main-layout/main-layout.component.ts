import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { animate, style, transition, trigger } from '@angular/animations';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { NotificationService } from '../../features/notifications/notifications.service';
import { ROLE_LABELS, Role } from '../../core/models/enums';
import { IconComponent, IconName } from '../../shared/components/icon/icon.component';

interface NavItem {
  label: string;
  path: string;
  icon: IconName;
  roles?: Role[];
}

@Component({
  selector: 'gt-main-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './main-layout.component.html',
  animations: [
    trigger('routeFade', [
      transition('* => *', [
        style({ opacity: 0, transform: 'translateY(12px)' }),
        animate('320ms cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'none' })),
      ]),
    ]),
  ],
})
export class MainLayoutComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);
  private notifications = inject(NotificationService);

  readonly collapsed = signal(false);
  readonly mobileOpen = signal(false);
  readonly menuOpen = signal(false);
  readonly currentUrl = signal(this.router.url);
  readonly user = this.auth.currentUser;
  /** Live unread-notification count for the bell badge. */
  readonly unread = this.notifications.unread;

  private readonly nav: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'Open Calls', path: '/applications/opportunities', icon: 'sparkles', roles: [Role.RESEARCHER] },
    { label: 'Applications', path: '/applications', icon: 'file-text' },
    { label: 'Reviews', path: '/reviews', icon: 'scale', roles: [Role.REVIEWER, Role.GRANT_ADMIN, Role.ADMIN] },
    { label: 'Funding', path: '/funding', icon: 'wallet', roles: [Role.ADMIN, Role.GRANT_ADMIN] },
    // Post-award lifecycle areas: every role participates except the (blind-review-only) Reviewer.
    { label: 'Awards', path: '/awards', icon: 'award', roles: [Role.ADMIN, Role.GRANT_ADMIN, Role.RESEARCHER, Role.FINANCE_OFFICER, Role.COMPLIANCE_OFFICER] },
    { label: 'Disbursements', path: '/disbursements', icon: 'landmark', roles: [Role.ADMIN, Role.GRANT_ADMIN, Role.RESEARCHER, Role.FINANCE_OFFICER, Role.COMPLIANCE_OFFICER] },
    { label: 'Progress', path: '/progress', icon: 'trending-up', roles: [Role.ADMIN, Role.GRANT_ADMIN, Role.RESEARCHER, Role.FINANCE_OFFICER, Role.COMPLIANCE_OFFICER] },
    { label: 'Outputs', path: '/outputs', icon: 'book', roles: [Role.ADMIN, Role.GRANT_ADMIN, Role.RESEARCHER, Role.FINANCE_OFFICER, Role.COMPLIANCE_OFFICER] },
    { label: 'Notifications', path: '/notifications', icon: 'bell' },
    { label: 'User Admin', path: '/users', icon: 'users', roles: [Role.ADMIN, Role.GRANT_ADMIN] },
  ];

  readonly visibleNav = computed(() => {
    this.user();
    const isAdmin = this.auth.hasAnyRole([Role.ADMIN]);
    return this.nav.filter((i) => isAdmin || this.auth.hasAnyRole(i.roles ?? []));
  });

  readonly initials = computed(() => {
    const name = this.user()?.name ?? '';
    return name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase() || '?';
  });

  readonly primaryRole = computed(() => {
    const roles = this.user()?.roles ?? [];
    return roles.length ? ROLE_LABELS[roles[0]] ?? roles[0] : '';
  });

  // Presentational breadcrumb derived from the current URL (no backend impact).
  readonly breadcrumbs = computed<{ label: string; url: string }[]>(() => {
    const path = this.currentUrl().split('?')[0].split('#')[0];
    const segments = path.split('/').filter(Boolean);
    const crumbs: { label: string; url: string }[] = [];
    let acc = '';
    for (const seg of segments) {
      acc += '/' + seg;
      const label = /^\d+$/.test(seg)
        ? 'Details'
        : seg.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
      crumbs.push({ label, url: acc });
    }
    return crumbs;
  });

  constructor() {
    // Close the mobile drawer / user menu on navigation, and refresh the unread badge.
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe((e) => {
      this.mobileOpen.set(false);
      this.menuOpen.set(false);
      this.currentUrl.set(e.urlAfterRedirects);
      this.notifications.refreshUnread();
    });
    // Initial load + light polling so newly-arrived notifications surface without a manual refresh.
    this.notifications.refreshUnread();
    setInterval(() => this.notifications.refreshUnread(), 60_000);
  }

  logout(): void {
    this.menuOpen.set(false);
    const obs = this.auth.logout();
    const done = () => {
      this.toast.success('Successfully logged out.');
      this.router.navigate(['/login']);
    };
    if (obs) obs.subscribe({ next: done, error: done });
    else done();
  }

}
