import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { EMPTY } from 'rxjs';
import { SessionService, WebSession } from 'identity-domain';

import { ShellComponent } from './shell.component';

function sessionStub(session: WebSession): Partial<SessionService> {
  return {
    session: signal<WebSession | null>(session).asReadonly(),
    status: signal('authenticated' as const).asReadonly(),
    logout: jasmine.createSpy('logout').and.returnValue(EMPTY),
    changePassword: jasmine.createSpy('changePassword'),
  };
}

function click(fixture: ComponentFixture<ShellComponent>, selector: string): void {
  fixture.nativeElement.querySelector(selector).click();
  fixture.detectChanges();
}

function navLabels(fixture: ComponentFixture<ShellComponent>): string[] {
  return Array.from(
    fixture.nativeElement.querySelectorAll('.sidenav .nav-item'),
  ).map((el) => (el as HTMLElement).textContent!.trim());
}

describe('ShellComponent sidebar', () => {
  function render(session: WebSession): ComponentFixture<ShellComponent> {
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: sessionStub(session) },
      ],
    });
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders a nav item for each visible section, in order', () => {
    const fixture = render({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD', 'STRUCTURAL_ADMIN', 'SABHA_DEFINITION'],
    });

    expect(navLabels(fixture)).toEqual([
      'Dashboard',
      'Structural Admin',
      'Sabha Definition',
    ]);
  });

  it('omits sections the user may not see', () => {
    const fixture = render({
      username: 'sanchalak',
      madhyasthaKaryalaya: false,
      sections: ['DASHBOARD', 'SANCHALAK_PROXY'],
    });

    const labels = navLabels(fixture);
    expect(labels).toContain('Dashboard');
    expect(labels).toContain('Sanchalak Proxy');
    expect(labels).not.toContain('Structural Admin');
  });

  it('shows the audit-log section to a user the BFF unlocks it for (Slice 19)', () => {
    const fixture = render({
      username: 'nirdeshak',
      madhyasthaKaryalaya: false,
      sections: ['DASHBOARD', 'AUDIT_LOG'],
    });

    expect(navLabels(fixture)).toContain('Audit Log');
  });

  it('shows the signed-in username in the header', () => {
    const fixture = render({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD'],
    });

    const trigger = fixture.nativeElement.querySelector('.account-trigger');
    expect(trigger.textContent.trim()).toBe('mk-admin');
  });

  it('logs out from the account menu', () => {
    const fixture = render({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD'],
    });
    const sessions = TestBed.inject(SessionService);

    click(fixture, '.account-trigger');
    click(fixture, '.account-menu .menu-item:last-child');

    expect(sessions.logout).toHaveBeenCalled();
  });

  it('starts a password change from the account menu', () => {
    const fixture = render({
      username: 'mk-admin',
      madhyasthaKaryalaya: true,
      sections: ['DASHBOARD'],
    });
    const sessions = TestBed.inject(SessionService);

    click(fixture, '.account-trigger');
    click(fixture, '.account-menu .menu-item:first-child');

    expect(sessions.changePassword).toHaveBeenCalled();
  });
});
