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
} from './structural.types';

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

  listZones(): Observable<ZoneView[]> {
    return this.http.get<ZoneView[]>('/bff/structure/zones');
  }

  createZone(cityId: string, name: string): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/zones', { cityId, name });
  }

  listSabhaKinds(): Observable<SabhaKindView[]> {
    return this.http.get<SabhaKindView[]>('/bff/structure/sabha-kinds');
  }

  createSabhaKind(demographic: Demographic, track: Track): Observable<CreatedResponse> {
    return this.http.post<CreatedResponse>('/bff/structure/sabha-kinds', { demographic, track });
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
}
