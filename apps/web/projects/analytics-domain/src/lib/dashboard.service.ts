import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CandidateRow, DashboardOverview, SabhaTree, Thresholds } from './dashboard.types';

/**
 * Outbound adapter to the re-engagement dashboard BFF endpoints (Slice 15,
 * ADR-0010, ADR-0022). Every read is scoped to the caller's roles server-side,
 * so no scope parameters are sent. All calls are cookie/session authenticated;
 * the CSRF header for the threshold PUT is attached by the app's HttpClient XSRF
 * configuration. Threshold reads are open to any resolved caller; the PUT is
 * MK-only server-side (403 otherwise, 422 on an invalid pair).
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  overview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>('/bff/dashboard/overview');
  }

  people(): Observable<CandidateRow[]> {
    return this.http.get<CandidateRow[]>('/bff/dashboard/people');
  }

  sabhaTree(): Observable<SabhaTree> {
    return this.http.get<SabhaTree>('/bff/dashboard/sabha-tree');
  }

  thresholds(): Observable<Thresholds> {
    return this.http.get<Thresholds>('/bff/dashboard/thresholds');
  }

  updateThresholds(thresholds: Thresholds): Observable<void> {
    return this.http.put<void>('/bff/dashboard/thresholds', thresholds);
  }
}
