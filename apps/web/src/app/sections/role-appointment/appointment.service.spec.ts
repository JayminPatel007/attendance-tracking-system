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

  it('reissues a password through the authenticated BFF endpoint', () => {
    let done = false;
    service.reissuePassword('u1', 'Forced123').subscribe(() => (done = true));

    const req = http.expectOne('/bff/password-reissue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetUserId: 'u1', newPassword: 'Forced123' });
    req.flush(null);

    expect(done).toBeTrue();
  });
});
