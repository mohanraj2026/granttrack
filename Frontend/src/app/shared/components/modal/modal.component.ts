import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Generic dialog shell. Parent controls visibility via [open] and listens to (closed).
 * Project body content directly and footer buttons with [slot=footer]:
 *   <gt-modal [open]="show" title="Edit" (closed)="show=false">
 *     ...body...
 *     <div footer><button>Save</button></div>
 *   </gt-modal>
 */
@Component({
  selector: 'gt-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open) {
      <div class="backdrop" (click)="onBackdrop()">
        <div class="dialog" [style.max-width.px]="width" (click)="$event.stopPropagation()">
          <header class="dlg-head">
            <h3>{{ title }}</h3>
            <button class="x" (click)="close()" aria-label="Close">×</button>
          </header>
          <div class="dlg-body"><ng-content /></div>
          <footer class="dlg-foot"><ng-content select="[footer]" /></footer>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .backdrop {
        position: fixed; inset: 0; background: rgba(15, 23, 42, 0.5);
        display: flex; align-items: flex-start; justify-content: center;
        padding: 5vh 1rem; z-index: 1000; overflow-y: auto;
        animation: fade 0.15s ease-out;
      }
      .dialog {
        background: #ffffff; border-radius: var(--gt-radius);
        border: 1px solid #e9eef5;
        box-shadow: var(--gt-shadow-lg); width: 100%; max-width: 560px;
        animation: pop 0.18s ease-out;
      }
      .dlg-head {
        display: flex; align-items: center; justify-content: space-between;
        padding: 1rem 1.4rem; border-bottom: 1px solid var(--gt-border);
      }
      .dlg-head h3 { margin: 0; }
      .x { background: none; border: none; font-size: 1.4rem; line-height: 1; cursor: pointer; color: var(--gt-text-faint); }
      .dlg-body { padding: 1.4rem; }
      .dlg-foot { padding: 1rem 1.4rem; border-top: 1px solid var(--gt-border); display: flex; justify-content: flex-end; gap: 0.6rem; }
      .dlg-foot:empty { display: none; }
      @keyframes fade { from { opacity: 0; } to { opacity: 1; } }
      @keyframes pop { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: none; } }
    `,
  ],
})
export class ModalComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() width = 560;
  @Input() closeOnBackdrop = true;
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
  onBackdrop(): void {
    if (this.closeOnBackdrop) this.close();
  }
}
