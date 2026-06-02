import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { SessionService, SessionStatus } from 'identity-domain';

import { AppComponent } from './app.component';

function configure(status: SessionStatus) {
  TestBed.configureTestingModule({
    imports: [AppComponent],
    providers: [
      provideRouter([]),
      { provide: SessionService, useValue: { status: signal(status).asReadonly() } },
    ],
  });
}

describe('AppComponent', () => {
  it('routes into the shell when the session is authenticated', () => {
    configure('authenticated');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('router-outlet')).not.toBeNull();
    expect(compiled.querySelector('.not-linked')).toBeNull();
  });

  it('shows the not-linked notice when Keycloak login has no local account', () => {
    configure('unlinked');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.not-linked')).not.toBeNull();
    expect(compiled.querySelector('router-outlet')).toBeNull();
  });
});
