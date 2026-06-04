import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  KshetraView,
  SabhaKindView,
  ZoneView,
} from '../structural-admin/structural.types';
import { NameCandidate, PersonResponse } from '../role-appointment/appointment.types';
import { DefineSabhaRequest, DefineSabhaResponse } from './sabha-definition.types';

/**
 * Outbound adapter to the Sabha-definition BFF endpoint (ADR-0012, ADR-0022):
 * `define` is the single one-transaction call that creates the Sabha and appoints
 * its Sanchalak. The lookups back the form's kind/Kshetra dropdowns and the
 * directory-first Sanchalak picker (reused from Slice 11). All calls are
 * cookie/session authenticated; the CSRF header is attached by the app's
 * HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class SabhaDefinitionService {
  private readonly http = inject(HttpClient);

  define(request: DefineSabhaRequest): Observable<DefineSabhaResponse> {
    return this.http.post<DefineSabhaResponse>('/bff/sabhas', request);
  }

  listSabhaKinds(): Observable<SabhaKindView[]> {
    return this.http.get<SabhaKindView[]>('/bff/structure/sabha-kinds');
  }

  listZones(): Observable<ZoneView[]> {
    return this.http.get<ZoneView[]>('/bff/structure/zones');
  }

  listKshetras(zoneId: string): Observable<KshetraView[]> {
    return this.http.get<KshetraView[]>('/bff/structure/kshetras', { params: { zoneId } });
  }

  searchByName(kshetraId: string, name: string): Observable<NameCandidate[]> {
    return this.http.get<NameCandidate[]>('/bff/directory/search', { params: { name, kshetraId } });
  }

  searchByMobile(mobile: string): Observable<PersonResponse> {
    return this.http.get<PersonResponse>('/bff/directory/search', { params: { mobile } });
  }
}
