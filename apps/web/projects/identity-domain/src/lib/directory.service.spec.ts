import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DirectoryService } from './directory.service';

describe('DirectoryService', () => {
  let service: DirectoryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DirectoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches by mobile against /bff/directory/search', () => {
    service.searchByMobile('+919820000001').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/directory/search');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('mobile')).toBe('+919820000001');
    req.flush({ id: 'p1', fullName: 'A', gender: 'MALE', dateOfBirth: null, mobile: null, guardianPersonId: null });
  });

  it('searches by name within a Kshetra', () => {
    service.searchByName('ksh1', 'Pratik').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/directory/search');
    expect(req.request.params.get('name')).toBe('Pratik');
    expect(req.request.params.get('kshetraId')).toBe('ksh1');
    req.flush([]);
  });
});
