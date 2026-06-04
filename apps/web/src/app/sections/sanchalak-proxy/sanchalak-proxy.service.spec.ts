import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SanchalakProxyService } from './sanchalak-proxy.service';

describe('SanchalakProxyService', () => {
  let service: SanchalakProxyService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SanchalakProxyService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the Sabhas the Nirikshak may proxy from the BFF', () => {
    service.listSabhas().subscribe();

    const req = http.expectOne('/bff/proxy/sabhas');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('lists a Sabha\'s Occurrences from the BFF', () => {
    service.listOccurrences('sabha-1').subscribe();

    const req = http.expectOne('/bff/proxy/sabhas/sabha-1/occurrences');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('posts a proxy cancel with the reason to the BFF', () => {
    service.cancel('occ-1', 'Sanchalak away').subscribe();

    const req = http.expectOne('/bff/proxy/occurrences/occ-1/cancel');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Sanchalak away' });
    req.flush(null);
  });

  it('posts a proxy venue override to the BFF', () => {
    service.overrideVenue('occ-1', 'Community Hall').subscribe();

    const req = http.expectOne('/bff/proxy/occurrences/occ-1/venue-override');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ venue: 'Community Hall' });
    req.flush(null);
  });

  it('posts a proxy reschedule to the BFF', () => {
    service.reschedule('occ-1', { date: '2026-08-09', startTime: '18:00', endTime: '19:30' }).subscribe();

    const req = http.expectOne('/bff/proxy/occurrences/occ-1/reschedule');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ date: '2026-08-09', startTime: '18:00', endTime: '19:30' });
    req.flush(null);
  });
});
