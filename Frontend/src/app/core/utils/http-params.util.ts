import { HttpParams } from '@angular/common/http';

/** Builds HttpParams from a flat object, skipping null/undefined/empty values. */
export function toHttpParams(query: Record<string, string | number | boolean | undefined | null> = {}): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}
