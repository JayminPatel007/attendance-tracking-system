import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SessionService } from 'identity-domain';

import { StructuralAdminComponent } from './structural-admin.component';
import { StructuralService } from './structural.service';
import { WebSession } from 'identity-domain';

type Authority = 'mk' | 'regional-team' | 'sanyojak';

function sessionStub(authority: Authority): Partial<SessionService> {
  const s: WebSession = {
    username: 'u',
    madhyasthaKaryalaya: authority === 'mk',
    regionalTeam: authority === 'regional-team',
    sections: ['DASHBOARD', 'STRUCTURAL_ADMIN'],
  };
  return { session: signal<WebSession | null>(s).asReadonly() };
}

function apiSpy(): jasmine.SpyObj<StructuralService> {
  const spy = jasmine.createSpyObj<StructuralService>('StructuralService', [
    'listCities', 'createCity', 'listZones', 'createZone',
    'listSabhaKinds', 'createSabhaKind', 'myCities', 'myZones', 'listKshetras', 'createKshetra',
  ]);
  spy.listCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai' }]));
  spy.listZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai' }]));
  spy.listSabhaKinds.and.returnValue(of([{ id: 'k1', demographic: 'YUVAK', track: 'REGULAR' }]));
  spy.myCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai' }]));
  spy.myZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai' }]));
  spy.listKshetras.and.returnValue(of([{ id: 'ksh1', name: 'Andheri-7', zoneId: 'z1' }]));
  spy.createCity.and.returnValue(of({ id: 'new' }));
  spy.createZone.and.returnValue(of({ id: 'new' }));
  spy.createSabhaKind.and.returnValue(of({ id: 'new' }));
  spy.createKshetra.and.returnValue(of({ id: 'new' }));
  return spy;
}

function mount(authority: Authority): { fixture: ComponentFixture<StructuralAdminComponent>; api: jasmine.SpyObj<StructuralService> } {
  const api = apiSpy();
  TestBed.configureTestingModule({
    imports: [StructuralAdminComponent],
    providers: [
      { provide: SessionService, useValue: sessionStub(authority) },
      { provide: StructuralService, useValue: api },
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

    expect(api.createCity).toHaveBeenCalledWith('Surat');
    expect(api.listCities).toHaveBeenCalled();
  });

  it('lets a Regional Team member create a Zone in one of their cities then refreshes the zone list', () => {
    const { fixture, api } = mount('regional-team');
    expect(fixture.componentInstance.myCities()).toEqual([{ id: 'c1', name: 'Mumbai' }]);
    api.listZones.calls.reset();

    fixture.componentInstance.newZoneName = 'Mumbai South';
    fixture.componentInstance.newZoneCityId = 'c1';
    fixture.componentInstance.createZone();

    expect(api.createZone).toHaveBeenCalledWith('c1', 'Mumbai South');
    expect(api.listZones).toHaveBeenCalled();
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
