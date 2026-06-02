import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Section, SessionService, WebSession } from 'identity-domain';

import { sectionGuard } from './section.guard';

function routeFor(section: Section): ActivatedRouteSnapshot {
  return { data: { section } } as unknown as ActivatedRouteSnapshot;
}

const STATE = {} as RouterStateSnapshot;

function configure(sections: Section[]): Router {
  const session: WebSession = {
    username: 'u',
    madhyasthaKaryalaya: true,
    sections,
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      {
        provide: SessionService,
        useValue: { session: signal<WebSession | null>(session).asReadonly() },
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
