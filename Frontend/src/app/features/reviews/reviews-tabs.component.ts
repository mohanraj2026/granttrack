import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { HasRoleDirective } from '../../shared/directives/has-role.directive';
import { Role } from '../../core/models/enums';
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-reviews-tabs',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, HasRoleDirective, IconComponent],
  template: `
    <ul class="nav nav-underline gap-1 mb-4 border-bottom flex-nowrap overflow-auto"
        style="--bs-nav-link-color:#64748b; --bs-nav-link-hover-color:var(--gt-navy); --bs-nav-underline-link-active-color:var(--gt-primary); --bs-nav-underline-border-width:2px;">
      <li class="nav-item">
        <a class="nav-link d-inline-flex align-items-center gap-2 px-3 fw-semibold" routerLink="/reviews/queue" routerLinkActive="active" *gtHasRole="[Role.REVIEWER, Role.GRANT_ADMIN, Role.ADMIN]">
          <gt-icon name="clipboard" [size]="16" /> My Queue
        </a>
      </li>
      <li class="nav-item">
        <a class="nav-link d-inline-flex align-items-center gap-2 px-3 fw-semibold" routerLink="/reviews/assignments" routerLinkActive="active" *gtHasRole="[Role.GRANT_ADMIN, Role.ADMIN]">
          <gt-icon name="users" [size]="16" /> Assignment Panel
        </a>
      </li>
    </ul>
  `,
})
export class ReviewsTabsComponent {
  protected readonly Role = Role;
}
