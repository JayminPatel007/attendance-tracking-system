import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideApi } from 'shared-data-access';

import { PasswordReissueComponent } from './password-reissue.component';

describe('PasswordReissueComponent', () => {
  let fixture: ComponentFixture<PasswordReissueComponent>;
  let component: PasswordReissueComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PasswordReissueComponent],
      // Pin the generated client's base path to "" so it issues the same
      // relative URL this spec asserts (it defaults to the spec server).
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApi({ basePath: '' })],
    });
    fixture = TestBed.createComponent(PasswordReissueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pre-fills a throwaway password to read out', () => {
    expect(component.newPassword.length).toBeGreaterThan(0);
  });

  it('reissues a fresh password to the target user and reveals it on success', () => {
    component.targetUserId = 'u1';
    component.newPassword = 'Temp1234abcd';
    component.submit();

    const req = http.expectOne('/bff/password-reissue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetUserId: 'u1', newPassword: 'Temp1234abcd' });
    req.flush(null);

    expect(component.stage()).toBe('done');
    expect(component.issuedPassword()).toBe('Temp1234abcd');
  });

  it('shows a not-authorized message on 403 and stays editing', () => {
    component.targetUserId = 'u1';
    component.newPassword = 'Temp1234abcd';
    component.submit();
    http.expectOne('/bff/password-reissue').flush(null, { status: 403, statusText: '' });

    expect(component.stage()).toBe('editing');
    expect(component.error()).toContain('not the appointer');
  });

  it('does not call the backend without a target user and password', () => {
    component.targetUserId = '   ';
    component.newPassword = '';
    component.submit();
    http.expectNone('/bff/password-reissue');
    expect(component.stage()).toBe('editing');
  });
});
