import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BrowserLocation } from 'shared-kernel';

import { SessionService } from './session.service';

class FakeBrowserLocation {
  assigned: string | null = null;
  assign(url: string): void {
    this.assigned = url;
  }
}

describe('SessionService', () => {
  let service: SessionService;
  let http: HttpTestingController;
  let location: FakeBrowserLocation;

  beforeEach(() => {
    location = new FakeBrowserLocation();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BrowserLocation, useValue: location },
      ],
    });
    service = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('exposes the web session returned by GET /bff/me', () => {
    service.load().subscribe();

    const req = http.expectOne('/bff/me');
    expect(req.request.method).toBe('GET');
    req.flush({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD', 'STRUCTURAL_ADMIN'],
    });

    expect(service.session()).toEqual({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD', 'STRUCTURAL_ADMIN'],
    });
  });

  it('redirects to the OIDC login endpoint when the BFF reports no session (401)', () => {
    service.load().subscribe();

    http.expectOne('/bff/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(location.assigned).toBe('/oauth2/authorization/keycloak');
    expect(service.session()).toBeNull();
  });

  it('surfaces an unlinked state without redirecting when the BFF returns 403', () => {
    service.load().subscribe();

    http.expectOne('/bff/me').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(service.status()).toBe('unlinked');
    expect(location.assigned).toBeNull();
  });

  it('reports an authenticated status once the session loads', () => {
    service.load().subscribe();

    http.expectOne('/bff/me').flush({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD'],
    });

    expect(service.status()).toBe('authenticated');
  });

  it('logs out via POST /bff/logout then reloads at the app root', () => {
    service.logout().subscribe();

    const req = http.expectOne('/bff/logout');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(location.assigned).toBe('/');
  });

  it('starts a forced password change via the OIDC kc_action flow', () => {
    service.changePassword();

    expect(location.assigned).toBe(
      '/oauth2/authorization/keycloak?kc_action=UPDATE_PASSWORD',
    );
  });
});
