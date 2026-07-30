import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { IconComponent } from '../icon/icon.component';

export interface FilterSelect {
  key: string;
  label: string;
  options: { value: string; label: string }[];
}

/**
 * Search + dropdown filter toolbar. Emits a flat params object on any change.
 *   <gt-search-filter-bar [filters]="filters" (change)="onFilter($event)" />
 */
@Component({
  selector: 'gt-search-filter-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="filter-bar">
      @if (showSearch) {
        <div class="search">
          <span class="ic"><gt-icon name="search" [size]="16" /></span>
          <input
            type="text"
            class="form-control"
            [placeholder]="searchPlaceholder"
            [(ngModel)]="query"
            (ngModelChange)="search$.next($event)" />
        </div>
      }
      @for (f of filters; track f.key) {
        <select class="form-control select" [(ngModel)]="values[f.key]" (ngModelChange)="emit()">
          <option value="">{{ f.label }}: All</option>
          @for (o of f.options; track o.value) {<option [value]="o.value">{{ o.label }}</option>}
        </select>
      }
      <ng-content />
    </div>
  `,
  styles: [
    `
      .filter-bar { display: flex; gap: 0.6rem; align-items: center; flex-wrap: wrap; margin-bottom: 1.1rem; }
      .search { position: relative; flex: 1 1 240px; min-width: 200px; }
      .search .ic { position: absolute; left: 0.65rem; top: 50%; transform: translateY(-50%); color: var(--gt-text-faint); display: inline-flex; pointer-events: none; }
      .search input { padding-left: 2.1rem; }
      .select { max-width: 220px; }
    `,
  ],
})
export class SearchFilterBarComponent implements OnInit {
  @Input() showSearch = true;
  @Input() searchPlaceholder = 'Search…';
  @Input() filters: FilterSelect[] = [];
  @Output() change = new EventEmitter<Record<string, string>>();

  query = '';
  values: Record<string, string> = {};
  readonly search$ = new Subject<string>();

  ngOnInit(): void {
    this.search$.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => this.emit());
  }

  emit(): void {
    const params: Record<string, string> = {};
    if (this.query.trim()) params['q'] = this.query.trim();
    for (const [k, v] of Object.entries(this.values)) {
      if (v) params[k] = v;
    }
    this.change.emit(params);
  }
}
