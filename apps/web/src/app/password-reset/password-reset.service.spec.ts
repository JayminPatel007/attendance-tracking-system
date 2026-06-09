import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PasswordResetService } from './password-reset.service';

describe('PasswordResetService', () => {
  let service: PasswordResetService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PasswordResetService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a reset on the public API and returns the reset id', () => {
    let resetId: string | undefined;
    service.requestReset('ramesh.bhai').subscribe((id) => (resetId = id));

    const req = http.expectOne('/api/password-reset/request');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'ramesh.bhai' });
    req.flush({ resetId: 'r1' });

    expect(resetId).toBe('r1');
  });

  it('verifies an OTP and returns the short-lived reset token', () => {
    let token: string | undefined;
    service.verifyOtp('r1', '123456').subscribe((t) => (token = t));

    const req = http.expectOne('/api/password-reset/verify');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resetId: 'r1', otpCode: '123456' });
    req.flush({ resetToken: 'tok-1' });

    expect(token).toBe('tok-1');
  });

  it('completes the reset by setting a new password against the token', () => {
    let done = false;
    service.completeReset('tok-1', 'NewPass123').subscribe(() => (done = true));

    const req = http.expectOne('/api/password-reset/complete');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resetToken: 'tok-1', newPassword: 'NewPass123' });
    req.flush(null);

    expect(done).toBeTrue();
  });

  it('looks up who can reissue a locked-out user, keyed on username', () => {
    let contacts: { name: string; mobile: string }[] | undefined;
    service.whoAppointedMe('ramesh.bhai').subscribe((c) => (contacts = c));

    const req = http.expectOne((r) => r.url === '/api/who-appointed-me');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('username')).toBe('ramesh.bhai');
    req.flush({ contacts: [{ name: 'Suresh', mobile: '+919820000001' }] });

    expect(contacts).toEqual([{ name: 'Suresh', mobile: '+919820000001' }]);
  });
});
