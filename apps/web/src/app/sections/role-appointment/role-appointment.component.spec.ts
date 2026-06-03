import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AppointmentService } from './appointment.service';
import { RoleAppointmentComponent } from './role-appointment.component';
import { AppointmentResponse } from './appointment.types';

function appointed(): AppointmentResponse {
  return { personId: 'p1', userId: 'u1', assignmentId: 'a1', candidates: [], requiresOverride: false };
}

function apiSpy(): jasmine.SpyObj<AppointmentService> {
  const spy = jasmine.createSpyObj<AppointmentService>('AppointmentService', [
    'appoint', 'searchByMobile', 'searchByName',
  ]);
  spy.appoint.and.returnValue(of(appointed()));
  spy.searchByMobile.and.returnValue(of({
    id: 'existing-1', fullName: 'Existing One', gender: 'MALE', dateOfBirth: null,
    mobile: '+919820000001', guardianPersonId: null,
  }));
  spy.searchByName.and.returnValue(of([{ personId: 'cand-1', fullName: 'Close Match', homeSabhas: [] }]));
  return spy;
}

function mount(): { fixture: ComponentFixture<RoleAppointmentComponent>; api: jasmine.SpyObj<AppointmentService> } {
  const api = apiSpy();
  TestBed.configureTestingModule({
    imports: [RoleAppointmentComponent],
    providers: [{ provide: AppointmentService, useValue: api }],
  });
  const fixture = TestBed.createComponent(RoleAppointmentComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('RoleAppointmentComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('switches the scope shape when the role changes', () => {
    const { fixture } = mount();
    const c = fixture.componentInstance;

    expect(c.scopeKind()).toBe('SABHA'); // default SANCHALAK
    c.onRoleChange('SANYOJAK');
    expect(c.scopeKind()).toBe('ZONE');
    c.onRoleChange('REGIONAL_TEAM');
    expect(c.scopeKind()).toBe('CITY');
  });

  it('reuses the existing Person when one is picked from the search results', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    c.sabhaId = 'sabha-1';
    c.username = 'someone';
    c.rawPassword = 'pw';

    c.searchByName();
    c.pickExisting('cand-1');
    c.submit();

    const request = api.appoint.calls.mostRecent().args[0];
    expect(request.existingPersonId).toBe('cand-1');
    expect(request.newPerson).toBeFalsy();
    expect(request.sabhaId).toBe('sabha-1');
    expect(fixture.componentInstance.stage()).toBe('done');
  });

  it('auto-suggests a username when starting an inline create and sends the new Person', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    c.sabhaId = 'sabha-1';

    c.startCreateNew();
    c.newPerson.fullName = 'Fresh Sanchalak';
    c.onNewPersonNameChange();
    expect(c.username).toBe('fresh.sanchalak');
    expect(c.rawPassword.length).toBe(12);

    c.newPerson.mobile = '+919820000123';
    c.newPerson.homeSabhaId = 'sabha-1';
    c.submit();

    const request = api.appoint.calls.mostRecent().args[0];
    expect(request.existingPersonId).toBeFalsy();
    expect(request.newPerson?.fullName).toBe('Fresh Sanchalak');
  });

  it('surfaces a username collision (409) before commit', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    api.appoint.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'USERNAME_TAKEN' },
    })));
    c.sabhaId = 'sabha-1';
    c.startCreateNew();
    c.newPerson.fullName = 'Taken Name';
    c.onNewPersonNameChange();

    c.submit();

    expect(c.error()).toContain('username is already taken');
    expect(c.stage()).toBe('editing');
  });

  it('shows the name soft-warn candidates and resubmits with an override', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    api.appoint.and.returnValue(of({
      personId: null, userId: null, assignmentId: null,
      candidates: [{ personId: 'dup-1', fullName: 'Look Alike', homeSabhas: [] }],
      requiresOverride: true,
    }));
    c.sabhaId = 'sabha-1';
    c.startCreateNew();
    c.newPerson.fullName = 'Look Alike';
    c.onNewPersonNameChange();

    c.submit();
    expect(c.softWarn().length).toBe(1);
    expect(c.stage()).toBe('editing');

    // Override clears the warning and resubmits the new Person with the override flag set.
    api.appoint.and.returnValue(of(appointed()));
    c.overrideSoftWarn();

    const request = api.appoint.calls.mostRecent().args[0];
    expect(request.newPerson?.overrideDuplicateWarning).toBeTrue();
    expect(c.stage()).toBe('done');
  });
});
