import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // zone.js-based change detection. Angular 21 defaults to zoneless when zone.js is absent,
    // which silently broke every component that updates plain fields from an HTTP subscribe
    // callback (the view never re-rendered - e.g. the Ambassadors list stuck on "Loading...").
    // eventCoalescing batches multiple events in the same task into one CD run.
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};
