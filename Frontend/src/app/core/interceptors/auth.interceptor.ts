import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';

/** Endpoints that must NOT receive a bearer token / trigger refresh. */
const AUTH_BYPASS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password'];

// Shared state so concurrent 401s trigger a single refresh and then replay.
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(TokenStorageService);
  const auth = inject(AuthService);
  const router = inject(Router);

  const isBypass = AUTH_BYPASS.some((u) => req.url.includes(u));
  const token = storage.accessToken;

  const authReq = token && !isBypass ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isBypass) {
        return handle401(authReq, next, auth, storage, router);
      }
      return throwError(() => error);
    }),
  );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
  storage: TokenStorageService,
  router: Router,
): Observable<HttpEvent<unknown>> {
  if (!storage.refreshToken) {
    auth.clearSession();
    router.navigate(['/login']);
    return throwError(() => new Error('Session expired'));
  }

  if (isRefreshing) {
    // Wait for the in-flight refresh, then replay with the new token.
    return refreshedToken$.pipe(
      filter((t): t is string => t !== null),
      take(1),
      switchMap((t) => next(addToken(req, t))),
    );
  }

  isRefreshing = true;
  refreshedToken$.next(null);

  return auth.refresh().pipe(
    switchMap((res) => {
      isRefreshing = false;
      refreshedToken$.next(res.data.accessToken);
      return next(addToken(req, res.data.accessToken));
    }),
    catchError((err) => {
      isRefreshing = false;
      auth.clearSession();
      router.navigate(['/login']);
      return throwError(() => err);
    }),
  );
}
