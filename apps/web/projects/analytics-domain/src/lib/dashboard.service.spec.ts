import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the overview from the BFF', () => {
    service.overview().subscribe();

    const req = http.expectOne('/bff/dashboard/overview');
    expect(req.request.method).toBe('GET');
    req.flush({ kpis: { totalCandidates: 0, priorityCandidates: 0, sabhasWithCandidates: 0 }, headlineCandidates: [] });
  });

  it('reads the People candidate rows from the BFF', () => {
    service.people().subscribe();

    const req = http.expectOne('/bff/dashboard/people');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('reads the Sabha tree from the BFF', () => {
    service.sabhaTree().subscribe();

    const req = http.expectOne('/bff/dashboard/sabha-tree');
    expect(req.request.method).toBe('GET');
    req.flush({ zones: [] });
  });

  it('reads the current thresholds from the BFF', () => {
    service.thresholds().subscribe();

    const req = http.expectOne('/bff/dashboard/thresholds');
    expect(req.request.method).toBe('GET');
    req.flush({ candidate: 3, priority: 6 });
  });

  it('updates the thresholds via PUT to the BFF', () => {
    service.updateThresholds({ candidate: 2, priority: 5 }).subscribe();

    const req = http.expectOne('/bff/dashboard/thresholds');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ candidate: 2, priority: 5 });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('reads the City-chip scope from the BFF', () => {
    service.scope().subscribe();

    const req = http.expectOne('/bff/dashboard/scope');
    expect(req.request.method).toBe('GET');
    req.flush({ sant: true, selectedCityId: null, cities: [] });
  });

  it('chooses a City via POST to the BFF', () => {
    service.chooseCity('city-1').subscribe();

    const req = http.expectOne('/bff/dashboard/city');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ cityId: 'city-1' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
