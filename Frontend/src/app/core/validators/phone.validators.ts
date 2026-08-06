import { AbstractControl, ValidatorFn, Validators } from '@angular/forms';

/** Every phone / mobile number in GrantTrack is exactly this many digits. */
export const PHONE_LENGTH = 10;

/** Digits only — no country code, spaces, dashes or brackets. */
export const PHONE_PATTERN = /^[0-9]{10}$/;

/** Validator set for every phone/mobile control: mandatory, numeric, exactly 10 digits. */
export const phoneValidators: ValidatorFn[] = [Validators.required, Validators.pattern(PHONE_PATTERN)];

/** Validation message for a touched phone control, or null when there is nothing to show. */
export function phoneErrorMessage(control: AbstractControl | null | undefined): string | null {
  if (!control || control.valid || !control.touched) return null;
  if (control.hasError('required')) return 'Mobile number is required.';
  return `Enter a valid ${PHONE_LENGTH}-digit mobile number — digits only, no spaces or symbols.`;
}
