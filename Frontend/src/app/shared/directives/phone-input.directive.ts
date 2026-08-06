import { Directive, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';
import { PHONE_LENGTH } from '../../core/validators/phone.validators';

/**
 * Keeps a phone/mobile input numeric: anything that is not a digit (typed, pasted, dropped or
 * autofilled) is stripped and the value is capped at PHONE_LENGTH digits. This is a typing aid —
 * `phoneValidators` on the control is what actually blocks submission.
 */
@Directive({
  selector: 'input[gtPhoneInput]',
  standalone: true,
  host: {
    inputmode: 'numeric',
    autocomplete: 'tel',
    '[attr.maxlength]': 'maxLength',
  },
})
export class PhoneInputDirective {
  private ngControl = inject(NgControl, { optional: true, self: true });

  readonly maxLength = PHONE_LENGTH;

  @HostListener('input', ['$event.target'])
  @HostListener('blur', ['$event.target'])
  sanitize(input: HTMLInputElement): void {
    const digits = input.value.replace(/\D/g, '').slice(0, PHONE_LENGTH);
    if (digits === input.value) return;
    input.value = digits;
    this.ngControl?.control?.setValue(digits);
  }
}
