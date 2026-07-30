import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'gt-outputs-tabs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent],
  template: `
    <ul class="nav nav-underline gap-1 mb-4 border-bottom flex-nowrap overflow-auto"
        style="--bs-nav-link-color:#64748b; --bs-nav-link-hover-color:var(--gt-navy); --bs-nav-underline-link-active-color:var(--gt-primary); --bs-nav-underline-border-width:2px;">
      <li class="nav-item">
        <a class="nav-link d-inline-flex align-items-center gap-2 px-3 fw-semibold" routerLink="/outputs/publications" routerLinkActive="active">
          <gt-icon name="book" [size]="16" /> Research Outputs
        </a>
      </li>
      <li class="nav-item">
        <a class="nav-link d-inline-flex align-items-center gap-2 px-3 fw-semibold" routerLink="/outputs/ip" routerLinkActive="active">
          <gt-icon name="award" [size]="16" /> IP Records
        </a>
      </li>
    </ul>
  `,
})
export class OutputsTabsComponent {}
