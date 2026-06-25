import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CityView,
  Demographic,
  KshetraView,
  SabhaKindView,
  Track,
  ZoneView,
} from 'sabha-domain';

interface CreatedResponse {
  id: string;
}

/**
 * Outbound adapter to the structural-admin BFF endpoints (ADR-0009, ADR-0022).
 * All calls are cookie/session authenticated; the CSRF header is attached by the
 * app's HttpClient XSRF configuration.
 */
@Injectable({ providedIn: 'root' })
export class StructuralService {
  private readonly http = inject(HttpClient);

  listCities(): Observable<CityView[]> {
    return this.http.get<CityView[]>('/bff/structure/cities');
  }

  createCity(name: string): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/cities', { name });
  }

  /** Deletes an empty City (ADR-0026); a non-empty City yields a 409 the caller surfaces. */
  deleteCity(id: string): Observable<void> {
    return this.http.delete<void>(`/bff/structure/cities/${id}`);
  }

  listZones(): Observable<ZoneView[]> {
    return this.http.get<ZoneView[]>('/bff/structure/zones');
  }

  createZone(cityId: string, name: string): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/zones', { cityId, name });
  }

  /** Deletes an empty Zone (ADR-0026); a non-empty Zone yields a 409 the caller surfaces. */
  deleteZone(id: string): Observable<void> {
    return this.http.delete<void>(`/bff/structure/zones/${id}`);
  }

  listSabhaKinds(): Observable<SabhaKindView[]> {
    return this.http.get<SabhaKindView[]>('/bff/structure/sabha-kinds');
  }

  createSabhaKind(demographic: Demographic, track: Track): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/sabha-kinds', { demographic, track });
  }

  retireSabhaKind(id: string): Observable<void> {
    return this.http.post<void>(`/bff/structure/sabha-kinds/${id}/retire`, {});
  }

  reactivateSabhaKind(id: string): Observable<void> {
    return this.http.post<void>(`/bff/structure/sabha-kinds/${id}/reactivate`, {});
  }

  myCities(): Observable<CityView[]> {
    return this.http.get<CityView[]>('/bff/structure/my-cities');
  }

  myZones(): Observable<ZoneView[]> {
    return this.http.get<ZoneView[]>('/bff/structure/my-zones');
  }

  listKshetras(zoneId: string): Observable<KshetraView[]> {
    return this.http.get<KshetraView[]>('/bff/structure/kshetras', { params: { zoneId } });
  }

  createKshetra(zoneId: string, name: string): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/kshetras', { zoneId, name });
  }

  /** Deletes an empty Kshetra (ADR-0026); a non-empty Kshetra yields a 409 the caller surfaces. */
  deleteKshetra(id: string): Observable<void> {
    return this.http.delete<void>(`/bff/structure/kshetras/${id}`);
  }
}
