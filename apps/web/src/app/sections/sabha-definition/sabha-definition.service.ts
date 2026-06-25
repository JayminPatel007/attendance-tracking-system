import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { KshetraView, SabhaKindView, ZoneView } from 'sabha-domain';

import { DefineSabhaRequest, DefineSabhaResponse, SabhaSummary } from './sabha-definition.types';

/**
 * Outbound adapter to the Sabha-definition BFF endpoint (ADR-0012, ADR-0022):
 * `define` is the single one-transaction call that creates the Sabha and appoints
 * its Sanchalak. The lookups back the form's kind/Kshetra dropdowns; the
 * directory-first Sanchalak picker uses the shared `DirectoryService`. All calls
 * are cookie/session authenticated; the CSRF header is attached by the app's
 * HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class SabhaDefinitionService {
  private readonly http = inject(HttpClient);

  define(request: DefineSabhaRequest): Observable<DefineSabhaResponse> {
    return this.http.post<DefineSabhaResponse>('/bff/sabhas', request);
  }

  /** The Sabhas the caller is Nirdeshak of, each with its Occurrence count (ADR-0026). */
  listMySabhas(): Observable<SabhaSummary[]> {
    return this.http.get<SabhaSummary[]>('/bff/sabhas/mine');
  }

  /** Deletes an empty Sabha (ADR-0026); a non-empty Sabha yields a 409 the caller surfaces. */
  deleteSabha(id: string): Observable<void> {
    return this.http.delete<void>(`/bff/sabhas/${id}`);
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
}
