import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService, ToastType } from '../../../core/services/toast.service';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'gt-toast-container',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="toast-stack" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast toast-{{ t.type }}" (click)="toast.dismiss(t.id)">
          <span class="ic"><gt-icon [name]="icon(t.type)" [size]="18" /></span>
          <span class="msg">{{ t.message }}</span>
          <button class="x" (click)="toast.dismiss(t.id); $event.stopPropagation()" aria-label="Dismiss">
            <gt-icon name="x" [size]="15" />
          </button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .toast-stack { position: fixed; top: 1rem; right: 1rem; z-index: 9999; display: flex; flex-direction: column; gap: 0.6rem; max-width: 400px; }
      .toast {
        display: flex; align-items: center; gap: 0.65rem;
        background: var(--gt-surface); border: 1px solid var(--gt-border);
        border-left-width: 4px; border-radius: var(--gt-radius-sm);
        box-shadow: var(--gt-shadow); padding: 0.8rem 0.9rem; cursor: pointer;
        animation: slide-in .2s ease-out;
      }
      .toast .ic { display: inline-flex; }
      .toast .msg { flex: 1; font-size: 0.86rem; color: var(--gt-text); }
      .toast .x { background: none; border: none; cursor: pointer; color: var(--gt-text-faint); display: inline-flex; padding: 2px; }
      .toast .x:hover { color: var(--gt-text); }
      .toast-success { border-left-color: var(--gt-success); } .toast-success .ic { color: var(--gt-success); }
      .toast-error { border-left-color: var(--gt-danger); } .toast-error .ic { color: var(--gt-danger); }
      .toast-info { border-left-color: var(--gt-info); } .toast-info .ic { color: var(--gt-info); }
      .toast-warning { border-left-color: var(--gt-warning); } .toast-warning .ic { color: var(--gt-warning); }
      @keyframes slide-in { from { opacity: 0; transform: translateX(20px); } to { opacity: 1; transform: none; } }
    `,
  ],
})
export class ToastContainerComponent {
  readonly toast = inject(ToastService);

  icon(type: ToastType): IconName {
    const map: Record<ToastType, IconName> = {
      success: 'check-circle',
      error: 'x-circle',
      info: 'info',
      warning: 'alert-triangle',
    };
    return map[type];
  }
}
