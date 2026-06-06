import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PendingNomination, SelectedPerson } from './selection.types';

/**
 * Outbound adapter to the demographic Nirdeshak's selection BFF endpoints (Slice
 * 16, ADR-0006, ADR-0022). `queue` and `selected` back the two lists; `approve` /
 * `reject` decide a PENDING nomination; `deselect` removes a previously approved
 * Person's selective Home Sabha. All calls are cookie/session authenticated; the
 * CSRF header is attached by the app's HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class SelectionService {
  private readonly http = inject(HttpClient);

  queue(): Observable<PendingNomination[]> {
    return this.http.get<PendingNomination[]>('/bff/selection/nominations');
  }

  selected(): Observable<SelectedPerson[]> {
    return this.http.get<SelectedPerson[]>('/bff/selection/selected');
  }

  approve(nominationId: string): Observable<void> {
    return this.http.post<void>(`/bff/selection/nominations/${nominationId}/approve`, {});
  }

  reject(nominationId: string, reason: string): Observable<void> {
    return this.http.post<void>(`/bff/selection/nominations/${nominationId}/reject`, { reason });
  }

  deselect(personId: string, selectiveSabhaId: string): Observable<void> {
    return this.http.post<void>('/bff/selection/deselect', { personId, selectiveSabhaId });
  }
}
