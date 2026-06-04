import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { OccurrenceListItem, ReopenRequest } from './occurrence-reopen.types';

/**
 * Outbound adapter to the Occurrence-reopen BFF endpoints (Slice 13, ADR-0001,
 * ADR-0022). `list` backs the left-pane Occurrence list (already scoped to the
 * caller's reopen authority server-side); `reopen` posts the reason-required
 * transition. All calls are cookie/session authenticated; the CSRF header is
 * attached by the app's HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class OccurrenceReopenService {
  private readonly http = inject(HttpClient);

  list(): Observable<OccurrenceListItem[]> {
    return this.http.get<OccurrenceListItem[]>('/bff/occurrences');
  }

  reopen(occurrenceId: string, reason: string): Observable<void> {
    const body: ReopenRequest = { reason };
    return this.http.post<void>(`/bff/occurrences/${occurrenceId}/reopen`, body);
  }
}
