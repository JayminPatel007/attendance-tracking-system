import { TestBed } from '@angular/core/testing';
import { WebSessionResponse } from 'shared-data-access';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { SessionService } from 'identity-domain';

import { sectionGuard } from './section.guard';

function routeFor(section: WebSessionResponse.SectionsEnum): ActivatedRouteSnapshot {
  return { data: { section } } as unknown as ActivatedRouteSnapshot;
}

const STATE = {} as RouterStateSnapshot;

function configure(sections: WebSessionResponse.SectionsEnum[]): Router {
  const session: WebSessionResponse = {
    username: 'u',
    madhyasthaKaryalaya: true,
    regionalTeam: false,
    sections,
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      {
        provide: SessionService,
        useValue: { session: signal<WebSessionResponse | null>(session).asReadonly() },
      },
    ],
  });
  return TestBed.inject(Router);
}

describe('sectionGuard', () => {
  it('allows activation when the section is visible', () => {
    configure(['DASHBOARD', 'STRUCTURAL_ADMIN']);

    const result = TestBed.runInInjectionContext(() =>
      sectionGuard(routeFor('STRUCTURAL_ADMIN'), STATE),
    );

    expect(result).toBeTrue();
  });

  it('redirects to the dashboard when the section is not visible', () => {
    const router = configure(['DASHBOARD']);

    const result = TestBed.runInInjectionContext(() =>
      sectionGuard(routeFor('STRUCTURAL_ADMIN'), STATE),
    );

    expect(result).toEqual(router.parseUrl('/dashboard'));
  });
});
