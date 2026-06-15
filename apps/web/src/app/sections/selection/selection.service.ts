import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  PendingNominationItem,
  SelectedPersonItem,
  SelectionBffControllerService,
} from '../../generated';

/**
 * Outbound adapter to the demographic Nirdeshak's selection BFF endpoints (Slice
 * 16, ADR-0006, ADR-0022). Delegates to the generated typed client
 * ({@link SelectionBffControllerService}, issue #73) — request/response shapes are
 * the generated `PendingNominationItem` / `SelectedPersonItem` models, so this
 * thin facade only renames the operations to the section's vocabulary. All calls
 * are cookie/session authenticated; the CSRF header is attached by the app's
 * HttpClient XSRF configuration. Errors surface as the usual `HttpErrorResponse`,
 * which the section maps to copy via the shared `http-error` seam (#67).
 */
@Injectable({ providedIn: 'root' })
export class SelectionService {
  private readonly api = inject(SelectionBffControllerService);

  queue(): Observable<PendingNominationItem[]> {
    return this.api.queue();
  }

  selected(): Observable<SelectedPersonItem[]> {
    return this.api.selected();
  }

  approve(nominationId: string): Observable<void> {
    return this.api.approve(nominationId) as Observable<void>;
  }

  reject(nominationId: string, reason: string): Observable<void> {
    return this.api.reject(nominationId, { reason }) as Observable<void>;
  }

  deselect(personId: string, selectiveSabhaId: string): Observable<void> {
    return this.api.deselect({ personId, selectiveSabhaId }) as Observable<void>;
  }
}
