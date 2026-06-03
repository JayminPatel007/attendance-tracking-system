import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AppointmentService } from './appointment.service';
import { AppointmentRequest } from './appointment.types';

describe('AppointmentService', () => {
  let service: AppointmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AppointmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts an appointment to the BFF', () => {
    const request: AppointmentRequest = {
      role: 'SANCHALAK', sabhaId: 's1', existingPersonId: 'p1',
      username: 'u', rawPassword: 'pw',
    };
    service.appoint(request).subscribe();

    const req = http.expectOne('/bff/appointments');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ personId: 'p1', userId: 'u1', assignmentId: 'a1', candidates: [], requiresOverride: false });
  });

  it('searches the Directory by mobile', () => {
    service.searchByMobile('+919820000001').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/directory/search');
    expect(req.request.params.get('mobile')).toBe('+919820000001');
    req.flush({ id: 'p1', fullName: 'X', gender: 'MALE', dateOfBirth: null, mobile: '+919820000001', guardianPersonId: null });
  });

  it('searches the Directory by name within a Kshetra', () => {
    service.searchByName('ksh1', 'Ramesh').subscribe();

    const req = http.expectOne((r) => r.url === '/bff/directory/search');
    expect(req.request.params.get('name')).toBe('Ramesh');
    expect(req.request.params.get('kshetraId')).toBe('ksh1');
    req.flush([]);
  });
});
