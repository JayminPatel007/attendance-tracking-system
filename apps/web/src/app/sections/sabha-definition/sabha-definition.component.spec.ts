import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { SabhaDefinitionComponent } from './sabha-definition.component';
import { SabhaDefinitionService } from './sabha-definition.service';
import { DefineSabhaResponse } from './sabha-definition.types';

function created(): DefineSabhaResponse {
  return {
    sabhaId: 's1', sanchalakAssignmentId: 'a1', sahSanchalakAssignmentId: null,
    candidates: [], requiresOverride: false,
  };
}

function apiSpy(): jasmine.SpyObj<SabhaDefinitionService> {
  const spy = jasmine.createSpyObj<SabhaDefinitionService>('SabhaDefinitionService', [
    'define', 'listSabhaKinds', 'listZones', 'listKshetras', 'searchByName', 'searchByMobile',
  ]);
  spy.define.and.returnValue(of(created()));
  spy.listSabhaKinds.and.returnValue(of([{ id: 'kind1', demographic: 'YUVAK', track: 'REGULAR' }]));
  spy.listZones.and.returnValue(of([{ id: 'zone1', name: 'Andheri', cityId: 'c1', cityName: 'Mumbai' }]));
  spy.listKshetras.and.returnValue(of([{ id: 'ksh1', name: 'Andheri-7', zoneId: 'zone1' }]));
  spy.searchByName.and.returnValue(of([{ personId: 'cand-1', fullName: 'Pratik Patel', homeSabhas: [] }]));
  spy.searchByMobile.and.returnValue(of({
    id: 'm-1', fullName: 'Mobile Match', gender: 'MALE', dateOfBirth: null,
    mobile: '+919820000001', guardianPersonId: null,
  }));
  return spy;
}

function mount(): { fixture: ComponentFixture<SabhaDefinitionComponent>; api: jasmine.SpyObj<SabhaDefinitionService> } {
  const api = apiSpy();
  TestBed.configureTestingModule({
    imports: [SabhaDefinitionComponent],
    providers: [{ provide: SabhaDefinitionService, useValue: api }],
  });
  const fixture = TestBed.createComponent(SabhaDefinitionComponent);
  fixture.detectChanges();
  return { fixture, api };
}

/** Fills the minimum required fields and picks a Sanchalak, leaving the schedule shape untouched. */
function fillRequired(c: SabhaDefinitionComponent): void {
  c.sabhaKindId = 'kind1';
  c.onZoneChange('zone1');
  c.kshetraId = 'ksh1';
  c.standingVenue = 'Sansthan Hall';
  c.sanchalak.nameQuery = 'Pratik';
  c.searchSanchalakByName();
  c.pickSanchalak('cand-1', 'Pratik Patel');
}

describe('SabhaDefinitionComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads kinds and zones on init and Kshetras when a Zone is chosen', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;

    expect(c.kinds().length).toBe(1);
    expect(c.zones().length).toBe(1);

    c.onZoneChange('zone1');
    expect(api.listKshetras).toHaveBeenCalledWith('zone1');
    expect(c.kshetras().length).toBe(1);
  });

  it('defaults to the weekly shape and toggles to monthly ad-hoc', () => {
    const { fixture } = mount();
    const c = fixture.componentInstance;

    expect(c.weekly()).toBeTrue();
    c.setWeekly(false);
    expect(c.weekly()).toBeFalse();
  });

  it('picks a Sanchalak from the directory and auto-suggests credentials', () => {
    const { fixture } = mount();
    const c = fixture.componentInstance;
    c.kshetraId = 'ksh1';
    c.sanchalak.nameQuery = 'Pratik';

    c.searchSanchalakByName();
    expect(c.sanchalak.candidates().length).toBe(1);

    c.pickSanchalak('cand-1', 'Pratik Patel');
    expect(c.sanchalak.selectedId()).toBe('cand-1');
    expect(c.sanchalak.username).toBe('pratik.patel');
    expect(c.sanchalak.rawPassword.length).toBe(12);
  });

  it('submits a weekly definition carrying the schedule slot', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    fillRequired(c);
    c.dayOfWeek = 'SUNDAY';
    c.startTime = '09:00';
    c.endTime = '10:30';

    expect(c.canSubmit()).toBeTrue();
    c.submit();

    const request = api.define.calls.mostRecent().args[0];
    expect(request.weekly).toBeTrue();
    expect(request.dayOfWeek).toBe('SUNDAY');
    expect(request.startTime).toBe('09:00');
    expect(request.endTime).toBe('10:30');
    expect(request.kshetraId).toBe('ksh1');
    expect(request.sabhaKindId).toBe('kind1');
    expect(request.sanchalak.existingPersonId).toBe('cand-1');
    expect(request.sahSanchalak).toBeNull();
    expect(c.stage()).toBe('done');
  });

  it('submits a monthly ad-hoc definition without schedule fields', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    fillRequired(c);
    c.setWeekly(false);

    c.submit();

    const request = api.define.calls.mostRecent().args[0];
    expect(request.weekly).toBeFalse();
    expect(request.dayOfWeek).toBeNull();
    expect(request.startTime).toBeNull();
    expect(request.endTime).toBeNull();
  });

  it('includes an optional Sah-Sanchalak when one is picked', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    fillRequired(c);
    c.pickSah('cand-2', 'Sah Helper');

    c.submit();

    const request = api.define.calls.mostRecent().args[0];
    expect(request.sahSanchalak?.existingPersonId).toBe('cand-2');
  });

  it('surfaces a 403 raised when defining outside the Nirdeshak scope', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    api.define.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    fillRequired(c);

    c.submit();

    expect(c.error()).toContain('not authorized');
    expect(c.stage()).toBe('editing');
  });
});
