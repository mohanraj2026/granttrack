import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    // Order matters: errorInterceptor is outermost so authInterceptor can transparently refresh and
    // replay a 401 before any error toast is shown. Only genuinely unrecoverable errors surface.
    provideHttpClient(withInterceptors([errorInterceptor, authInterceptor])),
    provideAnimations(),
  ],
};
