import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { StructuralService } from './structural.service';

describe('StructuralService', () => {
  let service: StructuralService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), StructuralService],
    });
    service = TestBed.inject(StructuralService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists cities from GET /bff/structure/cities', () => {
    let result: unknown;
    service.listCities().subscribe((c) => (result = c));

    const req = http.expectOne('/bff/structure/cities');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'c1', name: 'Surat' }]);

    expect(result).toEqual([{ id: 'c1', name: 'Surat' }]);
  });

  it('creates a city via POST /bff/structure/cities', () => {
    service.createCity('Surat').subscribe();

    const req = http.expectOne('/bff/structure/cities');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Surat' });
    req.flush({ id: 'new-id' });
  });

  it('creates a zone with its parent city', () => {
    service.createZone('c1', 'Mumbai South').subscribe();

    const req = http.expectOne('/bff/structure/zones');
    expect(req.request.body).toEqual({ cityId: 'c1', name: 'Mumbai South' });
    req.flush({ id: 'z9' });
  });

  it('registers a sabha kind', () => {
    service.createSabhaKind('YUVAK', 'BSS').subscribe();

    const req = http.expectOne('/bff/structure/sabha-kinds');
    expect(req.request.body).toEqual({ demographic: 'YUVAK', track: 'BSS' });
    req.flush({ id: 'k9' });
  });

  it('creates a kshetra within a zone', () => {
    service.createKshetra('z1', 'Goregaon-2').subscribe();

    const req = http.expectOne('/bff/structure/kshetras');
    expect(req.request.body).toEqual({ zoneId: 'z1', name: 'Goregaon-2' });
    req.flush({ id: 'ksh9' });
  });

  it('reads the zones the caller is a Sanyojak of', () => {
    service.myZones().subscribe();

    const req = http.expectOne('/bff/structure/my-zones');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'z1', name: 'Mumbai West', cityId: 'c1', cityName: 'Mumbai' }]);
  });

  it('lists kshetras within a given zone', () => {
    service.listKshetras('z1').subscribe();

    const req = http.expectOne('/bff/structure/kshetras?zoneId=z1');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
