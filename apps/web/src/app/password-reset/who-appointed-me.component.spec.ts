import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { WhoAppointedMeComponent } from './who-appointed-me.component';

describe('WhoAppointedMeComponent', () => {
  let fixture: ComponentFixture<WhoAppointedMeComponent>;
  let component: WhoAppointedMeComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WhoAppointedMeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(WhoAppointedMeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows the contacts who can reissue a password for the username', () => {
    component.username = 'ramesh.bhai';
    component.lookup();

    const req = http.expectOne((r) => r.url === '/api/who-appointed-me');
    expect(req.request.params.get('username')).toBe('ramesh.bhai');
    req.flush({ contacts: [{ name: 'Suresh', mobile: '+919820000001' }] });

    expect(component.contacts()).toEqual([{ name: 'Suresh', mobile: '+919820000001' }]);
    expect(component.error()).toBeNull();
  });

  it('shows a not-found message when the username is unknown (404)', () => {
    component.username = 'ghost';
    component.lookup();
    http.expectOne((r) => r.url === '/api/who-appointed-me').flush(null, { status: 404, statusText: '' });

    expect(component.contacts()).toBeNull();
    expect(component.error()).toContain("couldn't find");
  });

  it('does not call the backend for a blank username', () => {
    component.username = '   ';
    component.lookup();
    http.expectNone((r) => r.url === '/api/who-appointed-me');
  });
});
