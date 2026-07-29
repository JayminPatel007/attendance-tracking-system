import { signal } from '@angular/core';
import { WebSessionResponse } from 'shared-data-access';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SessionService } from 'identity-domain';

import {
  StructuralCreationControllerService,
  StructuralDeletionControllerService,
} from 'shared-data-access';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';
import { StructuralAdminComponent } from './structural-admin.component';

/**
 * The screen reads and writes the structure through one generated module and
 * deletes through another; the section is one surface to the reader, so one stub
 * stands in for both tokens.
 */
type StructuralApi = StructuralCreationControllerService & StructuralDeletionControllerService;

type Authority = 'mk' | 'regional-team' | 'sanyojak';

function sessionStub(authority: Authority): Partial<SessionService> {
  const s: WebSessionResponse = {
    username: 'u',
    madhyasthaKaryalaya: authority === 'mk',
    regionalTeam: authority === 'regional-team',
    sections: ['DASHBOARD', 'STRUCTURAL_ADMIN'],
  };
  return { session: signal<WebSessionResponse | null>(s).asReadonly() };
}

function apiSpy(): ApiStub<StructuralApi> {
  const spy = apiStub<StructuralApi>('StructuralApi', [
    'listCities', 'createCity', 'deleteCity', 'listZones', 'createZone', 'deleteZone',
    'listSabhaKinds', 'createSabhaKind', 'retireSabhaKind', 'reactivateSabhaKind',
    'myCities', 'myZones', 'listKshetras', 'createKshetra', 'deleteKshetra',
  ]);
  spy.listCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai', zoneCount: 0 }]));
  spy.listZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai', kshetraCount: 0 }]));
  spy.listSabhaKinds.and.returnValue(of([{ id: 'k1', demographic: 'YUVAK', track: 'REGULAR', retiredAt: null }]));
  spy.myCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai', zoneCount: 0 }]));
  spy.myZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai', kshetraCount: 0 }]));
  spy.listKshetras.and.returnValue(of([{ id: 'ksh1', name: 'Andheri-7', zoneId: 'z1', sabhaCount: 0 }]));
  spy.createCity.and.returnValue(of({ id: 'new' }));
  spy.deleteCity.and.returnValue(of(void 0));
  spy.createZone.and.returnValue(of({ id: 'new' }));
  spy.deleteZone.and.returnValue(of(void 0));
  spy.createSabhaKind.and.returnValue(of({ id: 'new' }));
  spy.retireSabhaKind.and.returnValue(of(void 0));
  spy.reactivateSabhaKind.and.returnValue(of(void 0));
  spy.createKshetra.and.returnValue(of({ id: 'new' }));
  spy.deleteKshetra.and.returnValue(of(void 0));
  return spy;
}

function mount(
  authority: Authority,
  configure?: (api: ApiStub<StructuralApi>) => void,
): { fixture: ComponentFixture<StructuralAdminComponent>; api: ApiStub<StructuralApi> } {
  const api = apiSpy();
  configure?.(api);
  TestBed.configureTestingModule({
    imports: [StructuralAdminComponent],
    providers: [
      { provide: SessionService, useValue: sessionStub(authority) },
      { provide: StructuralCreationControllerService, useValue: api },
      { provide: StructuralDeletionControllerService, useValue: api },
    ],
  });
  const fixture = TestBed.createComponent(StructuralAdminComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('StructuralAdminComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('shows the MK tabs without Zones and loads their lists for a Madhyastha Karyalaya member', () => {
    const { fixture, api } = mount('mk');

    expect(fixture.componentInstance.tabs()).toEqual(['cities', 'sabha-kinds']);
    expect(api.listCities).toHaveBeenCalled();
    expect(api.listSabhaKinds).toHaveBeenCalled();
    expect(api.listZones).not.toHaveBeenCalled();
    expect(api.myCities).not.toHaveBeenCalled();
    expect(api.myZones).not.toHaveBeenCalled();
  });

  it('shows only the Zones tab and loads the RT cities for the picker for a Regional Team member', () => {
    const { fixture, api } = mount('regional-team');

    expect(fixture.componentInstance.tabs()).toEqual(['zones']);
    expect(api.listZones).toHaveBeenCalled();
    expect(api.myCities).toHaveBeenCalled();
    expect(api.listCities).not.toHaveBeenCalled();
    expect(api.myZones).not.toHaveBeenCalled();
  });

  it('shows only the Kshetras tab and loads the Sanyojak own zones for a Sanyojak', () => {
    const { fixture, api } = mount('sanyojak');

    expect(fixture.componentInstance.tabs()).toEqual(['kshetras']);
    expect(api.myZones).toHaveBeenCalled();
    expect(api.listCities).not.toHaveBeenCalled();
    expect(api.myCities).not.toHaveBeenCalled();
  });

  it('creates a city then refreshes the city list', () => {
    const { fixture, api } = mount('mk');
    api.listCities.calls.reset();

    fixture.componentInstance.newCityName = 'Surat';
    fixture.componentInstance.createCity();

    expect(api.createCity).toHaveBeenCalledWith({ name: 'Surat' });
    expect(api.listCities).toHaveBeenCalled();
  });

  it('lets a Regional Team member create a Zone in one of their cities then refreshes the zone list', () => {
    const { fixture, api } = mount('regional-team');
    expect(fixture.componentInstance.myCities()).toEqual([{ id: 'c1', name: 'Mumbai', zoneCount: 0 }]);
    api.listZones.calls.reset();

    fixture.componentInstance.newZoneName = 'Mumbai South';
    fixture.componentInstance.newZoneCityId = 'c1';
    fixture.componentInstance.createZone();

    expect(api.createZone).toHaveBeenCalledWith({ cityId: 'c1', name: 'Mumbai South' });
    expect(api.listZones).toHaveBeenCalled();
  });

  it('tells a Regional Team member of no City that they cannot create Zones', () => {
    const { fixture } = mount('regional-team', (api) => api.myCities.and.returnValue(of([])));
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('.create-card')).toBeNull();
    expect(el.querySelector('p.empty')?.textContent).toContain('not on the Regional Team of any City');
  });

  it('soft-retires an active kind then refreshes the kind list', () => {
    const { fixture, api } = mount('mk');
    api.listSabhaKinds.calls.reset();

    fixture.componentInstance.retireSabhaKind({ id: 'k1', demographic: 'YUVAK', track: 'REGULAR', retiredAt: null });

    expect(api.retireSabhaKind).toHaveBeenCalledWith('k1');
    expect(api.listSabhaKinds).toHaveBeenCalled();
  });

  it('reactivates a retired kind then refreshes the kind list', () => {
    const { fixture, api } = mount('mk');
    api.listSabhaKinds.calls.reset();

    fixture.componentInstance.reactivateSabhaKind(
      { id: 'k1', demographic: 'YUVAK', track: 'REGULAR', retiredAt: '2026-06-19T10:00:00Z' });

    expect(api.reactivateSabhaKind).toHaveBeenCalledWith('k1');
    expect(api.listSabhaKinds).toHaveBeenCalled();
  });

  it('reflects the retired marker through isRetired', () => {
    const { fixture } = mount('mk');
    const c = fixture.componentInstance;

    expect(c.isRetired({ id: 'k1', demographic: 'YUVAK', track: 'REGULAR', retiredAt: null })).toBeFalse();
    expect(c.isRetired(
      { id: 'k2', demographic: 'YUVAK', track: 'REGULAR', retiredAt: '2026-06-19T10:00:00Z' })).toBeTrue();
  });

  it('deletes an empty city then refreshes the city list', () => {
    const { fixture, api } = mount('mk');
    api.listCities.calls.reset();

    fixture.componentInstance.deleteCity({ id: 'c1', name: 'Mumbai', zoneCount: 0 });

    expect(api.deleteCity).toHaveBeenCalledWith('c1');
    expect(api.listCities).toHaveBeenCalled();
  });

  it('disables the city delete with the block reason while it still has Zones', () => {
    const { fixture } = mount('mk', (api) =>
      api.listCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai', zoneCount: 3 }])));
    const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.entity-list button.delete');

    expect(btn?.disabled).toBeTrue();
    expect(btn?.textContent).toContain('has 3 Zones');
  });

  it('enables the city delete when the city is empty', () => {
    const { fixture } = mount('mk', (api) =>
      api.listCities.and.returnValue(of([{ id: 'c1', name: 'Surat', zoneCount: 0 }])));
    const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.entity-list button.delete');

    expect(btn?.disabled).toBeFalse();
    expect(btn?.textContent).toContain('Delete');
  });

  it('a Sanyojak deletes an empty Kshetra then refreshes its zone list', () => {
    const { fixture, api } = mount('sanyojak');
    api.listKshetras.calls.reset();

    fixture.componentInstance.deleteKshetra({ id: 'ksh1', name: 'Andheri-7', zoneId: 'z1', sabhaCount: 0 });

    expect(api.deleteKshetra).toHaveBeenCalledWith('ksh1');
    expect(api.listKshetras).toHaveBeenCalled();
  });

  it('disallows a Sanyukta selective kind in the builder but allows the regular one', () => {
    const { fixture } = mount('mk');
    const c = fixture.componentInstance;

    c.newKindDemographic = 'SANYUKTA';
    c.newKindTrack = 'BSS';
    expect(c.canRegisterKind()).toBeFalse();

    c.newKindTrack = 'REGULAR';
    expect(c.canRegisterKind()).toBeTrue();

    c.newKindDemographic = 'YUVAK';
    c.newKindTrack = 'YSS';
    expect(c.canRegisterKind()).toBeTrue();
  });
});
