import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ProxyOccurrence, ProxySabha, RescheduleRequest } from './sanchalak-proxy.types';

/**
 * Outbound adapter to the Nirikshak Sanchalak-proxy BFF endpoints (Slice 14,
 * ADR-0001, ADR-0022). `listSabhas` backs the picker (already scoped to the
 * caller's assignment server-side); `listOccurrences` backs the toolkit; the
 * shaping posts delegate to the same backend operations the Sanchalak uses, where
 * authority and the on-behalf-of audit attribution are enforced. All calls are
 * cookie/session authenticated; the CSRF header is attached by the app's
 * HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class SanchalakProxyService {
  private readonly http = inject(HttpClient);

  listSabhas(): Observable<ProxySabha[]> {
    return this.http.get<ProxySabha[]>('/bff/proxy/sabhas');
  }

  listOccurrences(sabhaId: string): Observable<ProxyOccurrence[]> {
    return this.http.get<ProxyOccurrence[]>(`/bff/proxy/sabhas/${sabhaId}/occurrences`);
  }

  cancel(occurrenceId: string, reason: string): Observable<void> {
    return this.http.post<void>(`/bff/proxy/occurrences/${occurrenceId}/cancel`, { reason });
  }

  overrideVenue(occurrenceId: string, venue: string): Observable<void> {
    return this.http.post<void>(`/bff/proxy/occurrences/${occurrenceId}/venue-override`, { venue });
  }

  reschedule(occurrenceId: string, request: RescheduleRequest): Observable<void> {
    return this.http.post<void>(`/bff/proxy/occurrences/${occurrenceId}/reschedule`, request);
  }
}
