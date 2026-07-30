import { AfterViewInit, ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

interface SegView {
  color: string;
  dash: string;
  rotation: number;
}

/**
 * Animated SVG donut with legend.
 * <gt-donut [segments]="[{label:'Accepted',value:3,color:'#15803d'}]" centerLabel="Deliverables" />
 */
@Component({
  selector: 'gt-donut',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="donut-wrap">
      <div class="donut" [style.width.px]="size" [style.height.px]="size">
        <svg [attr.width]="size" [attr.height]="size" [attr.viewBox]="'0 0 ' + size + ' ' + size">
          <circle [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="r" fill="none"
                  stroke="var(--gt-surface-3)" [attr.stroke-width]="stroke" />
          @for (s of segs(); track $index) {
            <circle class="seg" [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="r" fill="none"
                    [attr.stroke]="s.color" [attr.stroke-width]="stroke"
                    [attr.stroke-dasharray]="s.dash"
                    [attr.transform]="'rotate(' + s.rotation + ' ' + size / 2 + ' ' + size / 2 + ')'" />
          }
        </svg>
        <div class="donut-center">
          <div class="total">{{ total() }}</div>
          @if (centerLabel) {<div class="cl">{{ centerLabel }}</div>}
        </div>
      </div>
      <ul class="legend">
        @for (s of segments; track s.label) {
          <li><span class="sw" [style.background]="s.color"></span>{{ s.label }} <b>{{ s.value }}</b></li>
        }
      </ul>
    </div>
  `,
  styles: [
    `
      .donut-wrap { display: flex; align-items: center; gap: 1.4rem; flex-wrap: wrap; }
      .donut { position: relative; flex-shrink: 0; }
      .seg { transition: stroke-dasharray 1s cubic-bezier(.22,1,.36,1); }
      .donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
      .donut-center .total { font-size: 1.7rem; font-weight: 800; color: var(--gt-navy); line-height: 1; }
      .donut-center .cl { font-size: 0.72rem; color: var(--gt-text-muted); }
      .legend { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.5rem; }
      .legend li { display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; color: var(--gt-text); }
      .legend b { margin-left: auto; color: var(--gt-navy); }
      .sw { width: 11px; height: 11px; border-radius: 3px; flex-shrink: 0; }
    `,
  ],
})
export class DonutChartComponent implements AfterViewInit {
  @Input() size = 150;
  @Input() stroke = 20;
  @Input() centerLabel = '';

  private _segments = signal<DonutSegment[]>([]);
  private animated = signal(false);
  @Input() set segments(value: DonutSegment[]) {
    this._segments.set(value ?? []);
  }
  get segments(): DonutSegment[] { return this._segments(); }

  get r(): number { return (this.size - this.stroke) / 2; }
  get circumference(): number { return 2 * Math.PI * this.r; }

  readonly total = computed(() => this._segments().reduce((sum, s) => sum + s.value, 0));

  readonly segs = computed<SegView[]>(() => {
    const C = this.circumference;
    const total = this.total();
    if (total <= 0) return [];
    let cumulative = 0;
    return this._segments()
      .filter((s) => s.value > 0)
      .map((s) => {
        const frac = s.value / total;
        const len = this.animated() ? frac * C : 0;
        const rotation = -90 + (cumulative / total) * 360;
        cumulative += s.value;
        return { color: s.color, dash: `${len} ${C}`, rotation };
      });
  });

  ngAfterViewInit(): void {
    setTimeout(() => this.animated.set(true), 60);
  }
}
