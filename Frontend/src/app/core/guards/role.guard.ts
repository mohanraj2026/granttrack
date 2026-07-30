import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/** Allows the route only if the user holds one of route.data.roles. */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const required = (route.data?.['roles'] as string[]) ?? [];
  if (auth.hasAnyRole(required)) {
    return true;
  }
  toast.error('You do not have permission to access that area.');
  return router.createUrlTree(['/dashboard']);
};
