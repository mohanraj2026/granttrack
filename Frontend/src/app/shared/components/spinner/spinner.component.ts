import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'gt-spinner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="wrap" [style.padding.px]="pad">
      <div class="ring"></div>
      @if (label) {<span class="lbl">{{ label }}</span>}
    </div>
  `,
  styles: [
    `
      .wrap { display: flex; align-items: center; justify-content: center; gap: 0.6rem; color: var(--gt-text-muted); }
      .ring {
        width: 22px; height: 22px; border-radius: 50%;
        border: 3px solid var(--gt-border); border-top-color: var(--gt-blue);
        animation: spin 0.7s linear infinite;
      }
      .lbl { font-size: 0.85rem; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class SpinnerComponent {
  @Input() label = '';
  @Input() pad = 24;
}
