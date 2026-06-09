import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AppointmentRequest, AppointmentResponse, NameCandidate, PersonResponse } from './appointment.types';

/**
 * Outbound adapter to the role-appointment BFF endpoints (ADR-0011, ADR-0022).
 * Cookie/session authenticated; the CSRF header is attached by the app's
 * HttpClient XSRF configuration. `appoint` is the single one-transaction call;
 * the search endpoints back the directory-first candidate list.
 */
@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);

  appoint(request: AppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>('/bff/appointments', request);
  }

  searchByMobile(mobile: string): Observable<PersonResponse> {
    return this.http.get<PersonResponse>('/bff/directory/search', { params: { mobile } });
  }

  searchByName(kshetraId: string, name: string): Observable<NameCandidate[]> {
    return this.http.get<NameCandidate[]>('/bff/directory/search', { params: { name, kshetraId } });
  }

  /**
   * Assigner-reissue (ADR-0004, Slice 18B): the appointing Karyakar (or an MK
   * member, for Sants) issues a fresh, force-change password to a User who has
   * lost their mobile entirely. Same authenticated BFF + CSRF path as `appoint`;
   * whether this caller may reissue for the target is the backend's call (403).
   */
  reissuePassword(targetUserId: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/bff/password-reissue', { targetUserId, newPassword });
  }
}
