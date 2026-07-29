import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { SessionService } from 'identity-domain';
import { of } from 'rxjs';

import { routes } from './app.routes';
import { isPublicPath } from './password-reset/public-paths';
import { provideApi } from 'shared-data-access';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // XSRF defaults (cookie XSRF-TOKEN -> header X-XSRF-TOKEN) match Spring's
    // CookieCsrfTokenRepository, so the BFF logout POST is CSRF-protected.
    provideHttpClient(
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    // The generated API client (issue #73) defaults its base path to the spec's
    // server URL ("http://localhost"); pin it to "" so every request stays a
    // same-origin relative path — the proxy in dev, the same host in prod — and
    // keeps riding the session cookie + XSRF interceptor above.
    provideApi({ basePath: '' }),
    // Resolve the BFF session before the app renders: 200 sets the session,
    // 401 redirects into the OIDC login, 403 surfaces the not-linked state. The
    // public reset routes are exempt — a session-less visitor must reach them
    // without being bounced into Keycloak (ADR-0004, Slice 18B).
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [SessionService],
      useFactory: (sessions: SessionService) => () =>
        isPublicPath(window.location.pathname) ? of(undefined) : sessions.load(),
    },
  ],
};
