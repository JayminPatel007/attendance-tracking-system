import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SabhaDefinitionService } from './sabha-definition.service';
import { DefineSabhaRequest } from './sabha-definition.types';

describe('SabhaDefinitionService', () => {
  let service: SabhaDefinitionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SabhaDefinitionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts a weekly Sabha definition to the BFF', () => {
    const request: DefineSabhaRequest = {
      kshetraId: 'ksh1',
      sabhaKindId: 'kind1',
      weekly: true,
      dayOfWeek: 'SUNDAY',
      startTime: '09:00',
      endTime: '10:30',
      standingVenue: 'Sansthan Hall',
      sanchalak: { existingPersonId: 'p1', username: 'u', rawPassword: 'pw' },
      sahSanchalak: null,
    };
    service.define(request).subscribe();

    const req = http.expectOne('/bff/sabhas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({
      sabhaId: 's1', sanchalakAssignmentId: 'a1', sahSanchalakAssignmentId: null,
      candidates: [], requiresOverride: false,
    });
  });

  it('lists the Kshetras within a Zone for the form dropdown', () => {
    service.listKshetras('zone1').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/structure/kshetras');
    expect(req.request.params.get('zoneId')).toBe('zone1');
    req.flush([]);
  });

  it('searches the Directory by name within a Kshetra for the Sanchalak picker', () => {
    service.searchByName('ksh1', 'Pratik').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/directory/search');
    expect(req.request.params.get('name')).toBe('Pratik');
    expect(req.request.params.get('kshetraId')).toBe('ksh1');
    req.flush([]);
  });
});
