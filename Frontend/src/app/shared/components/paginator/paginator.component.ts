import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'gt-paginator',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="pager">
      <div class="info">
        @if (totalElements > 0) {
          {{ from() }}–{{ to() }} of {{ totalElements }}
        } @else {
          0 results
        }
      </div>
      <div class="controls">
        <label class="size">
          Rows:
          <select [value]="size" (change)="onSize($event)">
            @for (s of sizes; track s) {<option [value]="s">{{ s }}</option>}
          </select>
        </label>
        <button class="btn btn-secondary btn-sm" [disabled]="page === 0" (click)="go(page - 1)">
          <gt-icon name="chevron-left" [size]="15" /> Prev
        </button>
        <span class="pg">Page {{ page + 1 }} / {{ totalPages || 1 }}</span>
        <button class="btn btn-secondary btn-sm" [disabled]="page + 1 >= totalPages" (click)="go(page + 1)">
          Next <gt-icon name="chevron-right" [size]="15" />
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .pager { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.85rem 1rem; flex-wrap: wrap; }
      .info { font-size: 0.82rem; color: var(--gt-text-muted); }
      .controls { display: flex; align-items: center; gap: 0.6rem; }
      .size { font-size: 0.8rem; color: var(--gt-text-muted); display: flex; align-items: center; gap: 0.35rem; }
      .size select { padding: 0.25rem 0.4rem; border: 1px solid var(--gt-border-strong); border-radius: var(--gt-radius-sm); }
      .pg { font-size: 0.82rem; color: var(--gt-text-muted); }
    `,
  ],
})
export class PaginatorComponent {
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Input() sizes = [10, 20, 50, 100];

  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  from(): number { return this.page * this.size + 1; }
  to(): number { return Math.min((this.page + 1) * this.size, this.totalElements); }

  go(p: number): void {
    if (p >= 0 && p < this.totalPages) this.pageChange.emit(p);
  }
  onSize(e: Event): void {
    this.sizeChange.emit(Number((e.target as HTMLSelectElement).value));
  }
}
