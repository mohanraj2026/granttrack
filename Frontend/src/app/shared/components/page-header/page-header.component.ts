import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'gt-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="ph">
      <div>
        <h1>{{ title }}</h1>
        @if (subtitle) {<p class="text-muted">{{ subtitle }}</p>}
      </div>
      <div class="actions"><ng-content /></div>
    </header>
  `,
  styles: [
    `
      .ph { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; }
      .ph h1 { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.02em; margin: 0; color: var(--gt-navy); }
      .ph p { margin: 0.2rem 0 0; font-size: 0.9rem; }
      .actions { display: flex; gap: 0.6rem; flex-shrink: 0; align-items: center; }
    `,
  ],
})
export class PageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
}
