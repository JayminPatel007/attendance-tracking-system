import { Injectable } from '@angular/core';

/**
 * Thin injectable wrapper over `window.location`. Full-page navigations — the
 * OIDC login/logout/account redirects of the BFF flow (ADR-0022) — go through
 * here so they can be observed in tests instead of actually navigating the
 * Karma browser away.
 */
@Injectable({ providedIn: 'root' })
export class BrowserLocation {
  /** Navigate the whole document to {@code url}. */
  assign(url: string): void {
    window.location.assign(url);
  }

  /** The current document URL. */
  get href(): string {
    return window.location.href;
  }
}
