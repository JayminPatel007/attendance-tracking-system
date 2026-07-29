import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideApi } from 'shared-data-access';
import { provideRouter } from '@angular/router';

import { ForgotPasswordComponent } from './forgot-password.component';

describe('ForgotPasswordComponent', () => {
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let component: ForgotPasswordComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        // Pin the generated client's base path to "" so it issues the same
        // relative URLs this spec asserts (it defaults to the spec server).
        provideApi({ basePath: '' }),
      ],
    });
    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushRequest(status = 200, body: unknown = { resetId: 'r1' }) {
    component.username = 'ramesh.bhai';
    component.sendOtp();
    const req = http.expectOne('/api/password-reset/request');
    req.flush(body as object, status === 200 ? undefined : { status, statusText: '' });
  }

  it('walks username → OTP → new password → done on the happy path', () => {
    expect(component.stage()).toBe('username');

    flushRequest();
    expect(component.stage()).toBe('otp');

    component.otpCode = '123456';
    component.verify();
    http.expectOne('/api/password-reset/verify').flush({ resetToken: 'tok-1' });
    expect(component.stage()).toBe('password');

    component.newPassword = 'NewPass123';
    component.confirmPassword = 'NewPass123';
    component.complete();
    http.expectOne('/api/password-reset/complete').flush(null);
    expect(component.stage()).toBe('done');
  });

  it('shows a not-found message when the username is unknown (404)', () => {
    flushRequest(404, { detail: 'no such user' });
    expect(component.stage()).toBe('username');
    expect(component.error()).toContain("couldn't find");
  });

  it('points a no-mobile user at the who-appointed-me lookup (422)', () => {
    flushRequest(422, { detail: 'no mobile' });
    expect(component.stage()).toBe('username');
    expect(component.error()?.toLowerCase()).toContain('who appointed me');
  });

  it('surfaces the rate-limit / cooldown message on 429', () => {
    flushRequest(429, { detail: 'Too many OTP requests — wait a moment.' });
    expect(component.stage()).toBe('username');
    expect(component.error()).toContain('Too many OTP requests');
  });

  it('keeps the user on the OTP step with the backend message on a rejected OTP (422)', () => {
    flushRequest();
    component.otpCode = '000000';
    component.verify();
    http
      .expectOne('/api/password-reset/verify')
      .flush({ detail: 'Wrong OTP — 2 attempts left.' }, { status: 422, statusText: '' });

    expect(component.stage()).toBe('otp');
    expect(component.error()).toContain('Wrong OTP');
  });

  it('refuses to complete when the two passwords differ', () => {
    flushRequest();
    component.otpCode = '123456';
    component.verify();
    http.expectOne('/api/password-reset/verify').flush({ resetToken: 'tok-1' });

    component.newPassword = 'NewPass123';
    component.confirmPassword = 'Mismatch999';
    component.complete();

    http.expectNone('/api/password-reset/complete');
    expect(component.stage()).toBe('password');
    expect(component.error()).toContain('match');
  });
});
