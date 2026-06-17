import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { NameCandidate, PersonResponse } from './directory.types';

/**
 * Outbound adapter to the Directory search endpoint (ADR-0011, ADR-0013,
 * ADR-0022). The single `/bff/directory/search` call backs the directory-first
 * person picker every appointing flow composes — a mobile lookup returns one
 * Person, a name-within-Kshetra lookup returns the candidate list. Cookie/session
 * authenticated; the CSRF header is attached by the app's HttpClient XSRF config.
 */
@Injectable({ providedIn: 'root' })
export class DirectoryService {
  private readonly http = inject(HttpClient);

  searchByMobile(mobile: string): Observable<PersonResponse> {
    return this.http.get<PersonResponse>('/bff/directory/search', { params: { mobile } });
  }

  searchByName(kshetraId: string, name: string): Observable<NameCandidate[]> {
    return this.http.get<NameCandidate[]>('/bff/directory/search', { params: { name, kshetraId } });
  }
}
