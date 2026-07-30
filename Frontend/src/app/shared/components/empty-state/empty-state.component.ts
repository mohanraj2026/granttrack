import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'gt-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="empty">
      <div class="icon"><gt-icon [name]="icon" [size]="30" /></div>
      <p class="title">{{ title }}</p>
      @if (subtitle) {<p class="sub">{{ subtitle }}</p>}
    </div>
  `,
  styles: [
    `
      .empty { text-align: center; padding: 3rem 1rem; color: var(--gt-text-muted); }
      .icon {
        width: 60px; height: 60px; margin: 0 auto 1rem; border-radius: 16px;
        display: flex; align-items: center; justify-content: center;
        background: var(--gt-blue-50); color: var(--gt-blue);
      }
      .title { font-weight: 600; color: var(--gt-text); margin: 0 0 0.25rem; font-size: 0.95rem; }
      .sub { font-size: 0.85rem; margin: 0; max-width: 36ch; margin-inline: auto; }
    `,
  ],
})
export class EmptyStateComponent {
  @Input() icon: IconName = 'inbox';
  @Input() title = 'Nothing here yet';
  @Input() subtitle = '';
}
