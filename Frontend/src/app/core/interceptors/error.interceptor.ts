import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

/** Surfaces backend errors as toasts using the standard { success, message } envelope. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        // 401s on bypassed/refresh flows are handled by the auth interceptor; stay quiet there.
        const silent = error.status === 401 && req.url.includes('/auth/refresh');
        if (!silent) {
          toast.error(extractMessage(error));
        }
      }
      return throwError(() => error);
    }),
  );
};

function extractMessage(error: HttpErrorResponse): string {
  const body = error.error;
  if (body && typeof body === 'object' && 'message' in body && body.message) {
    const fieldErrors = body.data?.fieldErrors as { field: string; message: string }[] | undefined;
    if (fieldErrors?.length) {
      return `${body.message}: ${fieldErrors.map((f) => `${f.field} ${f.message}`).join('; ')}`;
    }
    return String(body.message);
  }
  if (error.status === 0) return 'Cannot reach the server. Is the backend running?';
  return `Request failed (${error.status})`;
}
