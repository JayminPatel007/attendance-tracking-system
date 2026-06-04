import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OccurrenceReopenService } from './occurrence-reopen.service';

describe('OccurrenceReopenService', () => {
  let service: OccurrenceReopenService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OccurrenceReopenService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the Occurrences the caller may reopen from the BFF', () => {
    service.list().subscribe();

    const req = http.expectOne('/bff/occurrences');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('posts a reopen with the required reason to the BFF', () => {
    service.reopen('occ-1', 'Forgot to mark Ravi').subscribe();

    const req = http.expectOne('/bff/occurrences/occ-1/reopen');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Forgot to mark Ravi' });
    req.flush(null);
  });
});
