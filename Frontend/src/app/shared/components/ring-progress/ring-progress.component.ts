import { AfterViewInit, ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';

/**
 * Animated circular progress ring / gauge.
 * <gt-ring [value]="62" label="Budget used" color="var(--gt-blue)" />
 */
@Component({
  selector: 'gt-ring',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ring-wrap" [style.width.px]="size" [style.height.px]="size">
      <svg [attr.width]="size" [attr.height]="size" [attr.viewBox]="'0 0 ' + size + ' ' + size">
        <circle [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="r" fill="none"
                [attr.stroke-width]="stroke" stroke="var(--gt-surface-3)" />
        <circle class="bar" [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="r" fill="none"
                [attr.stroke]="color" [attr.stroke-width]="stroke" stroke-linecap="round"
                [attr.stroke-dasharray]="circumference"
                [attr.stroke-dashoffset]="offset()"
                [attr.transform]="'rotate(-90 ' + size / 2 + ' ' + size / 2 + ')'" />
      </svg>
      <div class="ring-center">
        <div class="val">{{ display() }}{{ suffix }}</div>
        @if (label) {<div class="lbl">{{ label }}</div>}
      </div>
    </div>
  `,
  styles: [
    `
      .ring-wrap { position: relative; display: inline-flex; }
      .bar { transition: stroke-dashoffset 1s cubic-bezier(.22,1,.36,1); }
      .ring-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
      .val { font-size: 1.5rem; font-weight: 800; color: var(--gt-navy); line-height: 1; }
      .lbl { font-size: 0.72rem; color: var(--gt-text-muted); margin-top: 0.2rem; }
    `,
  ],
})
export class RingProgressComponent implements AfterViewInit {
  @Input() size = 130;
  @Input() stroke = 12;
  @Input() color = 'var(--gt-blue)';
  @Input() label = '';
  @Input() suffix = '%';

  private _value = signal(0);
  private animated = signal(false);
  @Input() set value(v: number) {
    this._value.set(Math.max(0, Math.min(100, v ?? 0)));
  }

  get r(): number { return (this.size - this.stroke) / 2; }
  get circumference(): number { return 2 * Math.PI * this.r; }

  readonly display = computed(() => Math.round(this._value()));
  readonly offset = computed(() =>
    this.animated() ? this.circumference * (1 - this._value() / 100) : this.circumference,
  );

  ngAfterViewInit(): void {
    setTimeout(() => this.animated.set(true), 60);
  }
}
