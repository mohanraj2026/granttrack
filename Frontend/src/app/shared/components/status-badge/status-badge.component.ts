import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { BadgeVariant, STATUS_VARIANT, humanize } from '../../../core/models/enums';

@Component({
  selector: 'gt-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge badge-{{ variant() }}">{{ label() }}</span>`,
})
export class StatusBadgeComponent {
  private _status = signal<string>('');
  @Input({ required: true })
  set status(value: string) {
    this._status.set(value ?? '');
  }

  readonly variant = computed<BadgeVariant>(() => STATUS_VARIANT[this._status()] ?? 'neutral');
  readonly label = computed(() => humanize(this._status()));
}
