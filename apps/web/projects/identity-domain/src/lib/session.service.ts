import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { BrowserLocation } from 'shared-kernel';

import { WebSession } from './web-session';

/** Spring's default OIDC login entry point for the `keycloak` registration. */
const LOGIN_URL = '/oauth2/authorization/keycloak';

/**
 * Lifecycle of the shell session. `pending` until `/bff/me` resolves;
 * `authenticated` once it does; `unlinked` when Keycloak knows the user but no
 * local User row is linked to them (BFF 403).
 */
export type SessionStatus = 'pending' | 'authenticated' | 'unlinked';

/**
 * Loads and holds the web shell's session via the Backend-for-Frontend
 * (ADR-0022). The request is authenticated by the server-side OIDC session
 * cookie, not a Bearer token, so this is a plain `GET /bff/me`.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly location = inject(BrowserLocation);

  private readonly _session = signal<WebSession | null>(null);
  private readonly _status = signal<SessionStatus>('pending');

  /** The current session once loaded, else `null`. */
  readonly session = this._session.asReadonly();

  /** The session lifecycle state, for the shell to branch its top-level view. */
  readonly status = this._status.asReadonly();

  /**
   * Resolves the session from the BFF. A 401 means there is no server-side OIDC
   * session, so we send the browser into the login flow; the returned stream
   * completes silently in that case rather than erroring.
   */
  load(): Observable<void> {
    return this.http.get<WebSession>('/bff/me').pipe(
      tap((session) => {
        this._session.set(session);
        this._status.set('authenticated');
      }),
      map(() => undefined),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.location.assign(LOGIN_URL);
          return of(undefined);
        }
        if (error.status === 403) {
          this._status.set('unlinked');
          return of(undefined);
        }
        throw error;
      }),
    );
  }

  /**
   * Ends the server-side session (BFF clears the HttpSession and returns 204)
   * then reloads at the app root, where the missing session sends the user back
   * into the login flow. The CSRF header is attached by the app's interceptor.
   */
  logout(): Observable<void> {
    return this.http.post('/bff/logout', null).pipe(
      tap(() => this.location.assign('/')),
      map(() => undefined),
    );
  }

  /**
   * Sends the user into Keycloak's hosted change-password page via the
   * `kc_action=UPDATE_PASSWORD` parameter on the OIDC login flow, returning to
   * the app afterwards. Used for the voluntary change from the account menu;
   * the first-login forced change is driven by the required-action set at
   * provisioning time.
   */
  changePassword(): void {
    this.location.assign(`${LOGIN_URL}?kc_action=UPDATE_PASSWORD`);
  }
}
