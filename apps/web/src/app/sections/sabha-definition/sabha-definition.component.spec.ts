import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DirectoryService, PersonPickerComponent } from 'identity-domain';
import { of, throwError } from 'rxjs';

import { SabhaDefinitionComponent } from './sabha-definition.component';
import { SabhaDefinitionService } from './sabha-definition.service';
import { DefineSabhaResponse, SabhaSummary } from './sabha-definition.types';

function created(): DefineSabhaResponse {
  return {
    sabhaId: 's1', sanchalakAssignmentId: 'a1', sahSanchalakAssignmentId: null,
    candidates: [], requiresOverride: false,
  };
}

function apiSpy(): jasmine.SpyObj<SabhaDefinitionService> {
  const spy = jasmine.createSpyObj<SabhaDefinitionService>('SabhaDefinitionService', [
    'define', 'listSabhaKinds', 'listZones', 'listKshetras', 'listMySabhas', 'deleteSabha',
  ]);
  spy.define.and.returnValue(of(created()));
  spy.listSabhaKinds.and.returnValue(of([{ id: 'kind1', demographic: 'YUVAK', track: 'REGULAR' }]));
  spy.listZones.and.returnValue(of([{ id: 'zone1', name: 'Andheri', cityId: 'c1', cityName: 'Mumbai', kshetraCount: 1 }]));
  spy.listKshetras.and.returnValue(of([{ id: 'ksh1', name: 'Andheri-7', zoneId: 'zone1', sabhaCount: 1 }]));
  spy.listMySabhas.and.returnValue(of([]));
  spy.deleteSabha.and.returnValue(of(void 0));
  return spy;
}

function directorySpy(): jasmine.SpyObj<DirectoryService> {
  const spy = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['searchByName', 'searchByMobile']);
  spy.searchByName.and.returnValue(of([{ personId: 'cand-1', fullName: 'Pratik Patel', homeSabhas: [] }]));
  spy.searchByMobile.and.returnValue(of({
    id: 'm-1', fullName: 'Mobile Match', gender: 'MALE', dateOfBirth: null,
    mobile: '+919820000001', guardianPersonId: null,
  }));
  return spy;
}

interface Mounted {
  fixture: ComponentFixture<SabhaDefinitionComponent>;
  api: jasmine.SpyObj<SabhaDefinitionService>;
  directory: jasmine.SpyObj<DirectoryService>;
}

function sabha(overrides: Partial<SabhaSummary> & { id: string }): SabhaSummary {
  return {
    kshetraId: 'ksh1', kshetraName: 'Andheri-7', demographic: 'YUVAK', track: 'REGULAR',
    standingVenue: 'Sansthan Hall', occurrenceCount: 0, ...overrides,
  };
}

function mount(configure?: (api: jasmine.SpyObj<SabhaDefinitionService>) => void): Mounted {
  const api = apiSpy();
  configure?.(api);
  const directory = directorySpy();
  TestBed.configureTestingModule({
    imports: [SabhaDefinitionComponent],
    providers: [
      { provide: SabhaDefinitionService, useValue: api },
      { provide: DirectoryService, useValue: directory },
    ],
  });
  const fixture = TestBed.createComponent(SabhaDefinitionComponent);
  fixture.detectChanges();
  return { fixture, api, directory };
}

/** The composed pickers, in template order: [Sanchalak, Sah-Sanchalak]. */
function pickers(fixture: ComponentFixture<SabhaDefinitionComponent>): PersonPickerComponent[] {
  fixture.detectChanges();
  return fixture.debugElement
    .queryAll(By.directive(PersonPickerComponent))
    .map((d) => d.componentInstance as PersonPickerComponent);
}

/** Fills the minimum required fields and picks a Sanchalak, leaving the schedule shape untouched. */
function fillRequired(fixture: ComponentFixture<SabhaDefinitionComponent>): { sanchalak: PersonPickerComponent; sah: PersonPickerComponent } {
  const c = fixture.componentInstance;
  c.sabhaKindId = 'kind1';
  c.onZoneChange('zone1');
  c.kshetraId = 'ksh1';
  c.standingVenue = 'Sansthan Hall';
  fixture.detectChanges();
  const [sanchalak, sah] = pickers(fixture);
  sanchalak.nameQuery = 'Pratik';
  sanchalak.searchByName();
  sanchalak.pick('cand-1', 'Pratik Patel');
  fixture.detectChanges();
  return { sanchalak, sah };
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
    fixture.detectChanges();
    const [sanchalak] = pickers(fixture);
    sanchalak.nameQuery = 'Pratik';

    sanchalak.searchByName();
    expect(sanchalak.candidates().length).toBe(1);

    sanchalak.pick('cand-1', 'Pratik Patel');
    expect(sanchalak.selectedId()).toBe('cand-1');
    expect(sanchalak.username).toBe('pratik.patel');
    expect(sanchalak.rawPassword.length).toBe(12);
  });

  it('submits a weekly definition carrying the schedule slot', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    const { sanchalak, sah } = fillRequired(fixture);
    c.dayOfWeek = 'SUNDAY';
    c.startTime = '09:00';
    c.endTime = '10:30';

    expect(c.canSubmit(sanchalak)).toBeTrue();
    c.submit(sanchalak, sah);

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
    const { sanchalak, sah } = fillRequired(fixture);
    c.setWeekly(false);

    c.submit(sanchalak, sah);

    const request = api.define.calls.mostRecent().args[0];
    expect(request.weekly).toBeFalse();
    expect(request.dayOfWeek).toBeNull();
    expect(request.startTime).toBeNull();
    expect(request.endTime).toBeNull();
  });

  it('includes an optional Sah-Sanchalak when one is picked', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    const { sanchalak, sah } = fillRequired(fixture);
    sah.pick('cand-2', 'Sah Helper');

    c.submit(sanchalak, sah);

    const request = api.define.calls.mostRecent().args[0];
    expect(request.sahSanchalak?.existingPersonId).toBe('cand-2');
  });

  it('surfaces a 403 raised when defining outside the Nirdeshak scope', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    api.define.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    const { sanchalak, sah } = fillRequired(fixture);

    c.submit(sanchalak, sah);

    expect(c.error()).toContain('not authorized');
    expect(c.stage()).toBe('editing');
  });

  it('loads the caller’s own Sabhas on init', () => {
    const { fixture, api } = mount();

    expect(api.listMySabhas).toHaveBeenCalled();
    expect(fixture.componentInstance.mySabhas().length).toBe(0);
  });

  it('deletes an empty Sabha then refreshes the list', () => {
    const { fixture, api } = mount();
    api.listMySabhas.calls.reset();

    fixture.componentInstance.deleteSabha(sabha({ id: 's1', occurrenceCount: 0 }));

    expect(api.deleteSabha).toHaveBeenCalledWith('s1');
    expect(api.listMySabhas).toHaveBeenCalled();
  });

  it('disables the Sabha delete with the block reason while it has Occurrences', () => {
    const { fixture } = mount((api) =>
      api.listMySabhas.and.returnValue(of([sabha({ id: 's1', occurrenceCount: 4 })])));
    const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.my-sabhas button.delete');

    expect(btn?.disabled).toBeTrue();
    expect(btn?.textContent).toContain('has 4 Occurrences');
  });

  it('enables the Sabha delete when the Sabha has no Occurrences', () => {
    const { fixture } = mount((api) =>
      api.listMySabhas.and.returnValue(of([sabha({ id: 's1', occurrenceCount: 0 })])));
    const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.my-sabhas button.delete');

    expect(btn?.disabled).toBeFalse();
    expect(btn?.textContent).toContain('Delete');
  });
});
