import { Component, Input, Output, EventEmitter, forwardRef, signal, computed, ViewChild, ElementRef, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { IconComponent } from '../icon/icon.component';
import { ClickOutsideDirective } from '../../directives/click-outside.directive';

export interface SelectOption {
  value: any;
  label: string;
}

@Component({
  selector: 'gt-searchable-select',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, ClickOutsideDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchableSelectComponent),
      multi: true
    }
  ],
  template: `
    <div class="searchable-select" (gtClickOutside)="close()" [class.open]="isOpen()">
      <div class="select-trigger form-control" (click)="toggleOpen()" [class.is-invalid]="invalid">
        <span class="trigger-label" [class.is-placeholder]="!value()">{{ selectedLabel() || placeholder }}</span>
        <gt-icon name="chevron-down" [size]="16" class="trigger-icon" />
      </div>
      
      @if (isOpen()) {
        <div class="select-dropdown">
          <div class="select-search">
            <gt-icon name="search" [size]="16" class="search-icon" />
            <input 
              type="text" 
              [placeholder]="searchPlaceholder" 
              [ngModel]="searchTerm()"
              (ngModelChange)="onSearchChange($event)"
              (click)="$event.stopPropagation()"
              #searchInput 
            />
          </div>
          <ul class="select-options">
            @for (opt of filteredOptions(); track opt.value) {
              <li class="select-option" [class.selected]="opt.value === value()" (click)="selectOption(opt)">
                {{ opt.label }}
              </li>
            }
            @if (filteredOptions().length === 0) {
              <li class="select-no-results">No results found</li>
            }
          </ul>
        </div>
      }
    </div>
  `,
  styles: [`
    .searchable-select { position: relative; width: 100%; font-family: 'Inter', sans-serif; }
    .select-trigger { 
      display: flex; align-items: center; justify-content: space-between; 
      cursor: pointer; user-select: none; background: var(--gt-surface);
    }
    .trigger-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .trigger-label.is-placeholder { color: var(--gt-text-muted); }
    .trigger-icon { color: var(--gt-text-muted); flex-shrink: 0; transition: transform 0.2s; }
    .open .trigger-icon { transform: rotate(180deg); }
    
    .select-dropdown { 
      position: absolute; top: 100%; left: 0; right: 0; margin-top: 4px; 
      background: var(--gt-surface); border: 1px solid var(--gt-border); 
      border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); 
      z-index: 1000; max-height: 320px; display: flex; flex-direction: column; overflow: hidden; 
    }
    .select-search { 
      padding: 10px 12px; border-bottom: 1px solid var(--gt-border); 
      display: flex; align-items: center; gap: 8px; background: var(--gt-surface-alt); 
    }
    .search-icon { color: var(--gt-text-muted); }
    .select-search input { 
      flex: 1; border: none; background: transparent; outline: none; 
      font-size: 14px; color: var(--gt-text); font-family: inherit;
    }
    .select-options { 
      list-style: none; padding: 4px 0; margin: 0; overflow-y: auto; 
    }
    .select-option { 
      padding: 10px 16px; cursor: pointer; transition: background 0.15s, color 0.15s;
      font-size: 14px;
    }
    .select-option:hover { background: var(--gt-surface-alt); }
    .select-option.selected { 
      background: rgba(43, 90, 219, 0.1); 
      color: var(--gt-primary); font-weight: 500; 
    }
    .select-no-results { 
      padding: 16px; text-align: center; color: var(--gt-text-muted); 
      font-style: italic; font-size: 14px;
    }
    .is-invalid { border-color: var(--gt-error); }
  `]
})
export class SearchableSelectComponent implements ControlValueAccessor {
  options = input<SelectOption[]>([]);
  @Input() placeholder = 'Select an option...';
  @Input() searchPlaceholder = 'Search...';
  @Input() invalid = false;

  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  readonly value = signal<any>(null);
  readonly isOpen = signal(false);
  readonly searchTerm = signal('');

  readonly selectedLabel = computed(() => {
    const val = this.value();
    if (val === null || val === undefined) return '';
    const opt = this.options().find(o => o.value === val);
    return opt ? opt.label : '';
  });

  readonly filteredOptions = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    if (!term) return this.options();
    return this.options().filter(opt => 
      opt.label.toLowerCase().includes(term) || 
      String(opt.value).toLowerCase().includes(term)
    );
  });

  onChange: any = () => {};
  onTouched: any = () => {};

  writeValue(val: any): void {
    this.value.set(val);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  
  setDisabledState?(isDisabled: boolean): void {
    // Basic disable implementation if needed
  }

  toggleOpen(): void {
    const nextState = !this.isOpen();
    this.isOpen.set(nextState);
    if (nextState) {
      this.searchTerm.set('');
      setTimeout(() => {
        if (this.searchInput) {
          this.searchInput.nativeElement.focus();
        }
      });
    } else {
      this.onTouched();
    }
  }

  close(): void {
    if (this.isOpen()) {
      this.isOpen.set(false);
      this.onTouched();
    }
  }

  selectOption(opt: SelectOption): void {
    this.value.set(opt.value);
    this.onChange(opt.value);
    this.close();
  }
  
  onSearchChange(term: string): void {
    this.searchTerm.set(term);
  }
}
