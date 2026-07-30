import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, TemplateRef } from '@angular/core';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { SpinnerComponent } from '../spinner/spinner.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { IconComponent, IconName } from '../icon/icon.component';

export interface ColumnDef {
  key: string;
  header: string;
  sortable?: boolean;
  type?: 'text' | 'money' | 'date' | 'datetime' | 'number' | 'badge' | 'bool' | 'progress' | 'appId' | 'userId';
  width?: string;
  align?: 'left' | 'right' | 'center';
}

export type SortDir = 'asc' | 'desc';

/**
 * Reusable presentational table. Data columns render by type; a row-action column is
 * supplied via [rowActions] (a TemplateRef given the row as implicit context).
 *
 *   <gt-data-table [columns]="cols" [rows]="rows" [loading]="loading"
 *      [sortField]="sortField" [sortDir]="sortDir" (sortChange)="onSort($event)"
 *      [rowActions]="actions">
 *     <ng-template #actions let-row>
 *       <button class="btn btn-sm btn-secondary" (click)="edit(row)">Edit</button>
 *     </ng-template>
 *   </gt-data-table>
 */
@Component({
  selector: 'gt-data-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, StatusBadgeComponent, SpinnerComponent, EmptyStateComponent, IconComponent],
  template: `
    <div class="table-wrap">
      <table class="gt-table">
        <thead>
          <tr>
            @for (col of columns; track col.key) {
              <th
                [class.sortable]="col.sortable"
                [style.width]="col.width"
                [style.text-align]="col.align || 'left'"
                (click)="col.sortable && toggleSort(col.key)">
                {{ col.header }}
                @if (col.sortable && sortField === col.key) {
                  <span class="arrow" [class.asc]="sortDir === 'asc'"><gt-icon name="chevron-down" [size]="13" /></span>
                }
              </th>
            }
            @if (rowActions) {<th style="text-align:right">Actions</th>}
          </tr>
        </thead>
        <tbody>
          @if (!loading) {
            @for (row of rows; track trackKey(row)) {
              <tr>
                @for (col of columns; track col.key) {
                  <td [style.text-align]="col.align || (col.type === 'money' || col.type === 'number' ? 'right' : 'left')">
                    @switch (col.type) {
                      @case ('badge') { <gt-status-badge [status]="value(row, col.key)" /> }
                      @case ('money') { <span class="money">{{ value(row, col.key) | currency: 'INR' : 'symbol' : '1.0-0' }}</span> }
                      @case ('date') { {{ value(row, col.key) ? (value(row, col.key) | date: 'mediumDate') : '—' }} }
                      @case ('datetime') { {{ value(row, col.key) ? (value(row, col.key) | date: 'medium') : '—' }} }
                      @case ('bool') { {{ value(row, col.key) ? 'Yes' : 'No' }} }
                      @case ('appId') { <span class="font-monospace text-muted small fw-semibold">{{ value(row, col.key) ? 'APP' + value(row, col.key).toString().padStart(4, '0') : '—' }}</span> }
                      @case ('userId') { <span class="font-monospace text-muted small fw-semibold">{{ value(row, col.key) ? 'GTU' + value(row, col.key).toString().padStart(4, '0') : '—' }}</span> }
                      @case ('progress') { 
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <div style="flex: 1; height: 6px; background: var(--gt-border); border-radius: 3px; overflow: hidden;">
                            <div [style.width.%]="value(row, col.key)" [style.background]="value(row, col.key) >= 100 ? 'var(--gt-danger)' : (value(row, col.key) > 75 ? 'var(--gt-warning)' : 'var(--gt-blue)')" style="height: 100%; transition: width 0.3s ease;"></div>
                          </div>
                          <span style="font-size: 0.75rem; color: var(--gt-text-light); min-width: 2.5rem; text-align: right;">{{ value(row, col.key) || 0 }}%</span>
                        </div>
                      }
                      @default { {{ display(row, col.key) }} }
                    }
                  </td>
                }
                @if (rowActions) {
                  <td style="text-align:right">
                    <ng-container *ngTemplateOutlet="rowActions; context: { $implicit: row }" />
                  </td>
                }
              </tr>
            }
          }
        </tbody>
      </table>

      @if (loading) { <gt-spinner label="Loading…" /> }
      @else if (!rows.length) {
        <gt-empty-state [title]="emptyTitle" [subtitle]="emptySubtitle" [icon]="emptyIcon" />
      }
    </div>
  `,
  styles: [
    `
      .table-wrap { overflow: visible; }
      .arrow { display: inline-flex; vertical-align: middle; margin-left: 0.25rem; color: var(--gt-blue); transition: transform .15s; }
      .arrow.asc { transform: rotate(180deg); }
    `,
  ],
})
export class DataTableComponent {
  @Input({ required: true }) columns: ColumnDef[] = [];
  // Accept any row shape so typed DTO arrays bind under strictTemplates.
  @Input({ required: true }) rows: any[] = [];
  @Input() loading = false;
  @Input() idKey = 'id';
  @Input() rowActions?: TemplateRef<unknown>;
  @Input() sortField = '';
  @Input() sortDir: SortDir = 'asc';
  @Input() emptyTitle = 'No records found';
  @Input() emptySubtitle = 'Try adjusting your filters.';
  @Input() emptyIcon: IconName = 'inbox';

  @Output() sortChange = new EventEmitter<string>();

  value(row: any, key: string): any {
    return row[key];
  }
  display(row: any, key: string): string {
    const v = row[key];
    return v === null || v === undefined || v === '' ? '—' : String(v);
  }
  trackKey(row: any): unknown {
    return row[this.idKey] ?? row;
  }

  toggleSort(key: string): void {
    const dir: SortDir = this.sortField === key && this.sortDir === 'asc' ? 'desc' : 'asc';
    this.sortChange.emit(`${key},${dir}`);
  }
}
