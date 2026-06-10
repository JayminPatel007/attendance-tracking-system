import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuditEntry, AuditFilter } from './audit-log.types';

/**
 * Outbound adapter to the audit-log viewer BFF (Slice 19, ADR-0023, ADR-0022).
 * One read: `list` fetches the scoped feed, newest first; authority and
 * geographic scoping are entirely the backend's call, so a denial surfaces as
 * a 403 here. Cookie/session authenticated; the CSRF header is attached by the
 * app's HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly http = inject(HttpClient);

  list(filter: AuditFilter): Observable<AuditEntry[]> {
    let params = new HttpParams();
    if (filter.targetType) {
      params = params.set('targetType', filter.targetType);
    }
    if (filter.targetId) {
      params = params.set('targetId', filter.targetId);
    }
    if (filter.actorUserId) {
      params = params.set('actorUserId', filter.actorUserId);
    }
    if (filter.action) {
      params = params.set('action', filter.action);
    }
    if (filter.from) {
      params = params.set('from', filter.from);
    }
    if (filter.to) {
      params = params.set('to', filter.to);
    }
    if (filter.proxyOnly) {
      params = params.set('proxyOnly', 'true');
    }
    return this.http.get<AuditEntry[]>('/bff/audit-log', { params });
  }
}
