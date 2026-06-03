import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SessionService } from 'identity-domain';

import { StructuralAdminComponent } from './structural-admin.component';
import { StructuralService } from './structural.service';
import { WebSession } from 'identity-domain';

function sessionStub(madhyasthaKaryalaya: boolean): Partial<SessionService> {
  const s: WebSession = {
    username: 'u',
    madhyasthaKaryalaya,
    sections: ['DASHBOARD', 'STRUCTURAL_ADMIN'],
  };
  return { session: signal<WebSession | null>(s).asReadonly() };
}

function apiSpy(): jasmine.SpyObj<StructuralService> {
  const spy = jasmine.createSpyObj<StructuralService>('StructuralService', [
    'listCities', 'createCity', 'listZones', 'createZone',
    'listSabhaKinds', 'createSabhaKind', 'myZones', 'listKshetras', 'createKshetra',
  ]);
  spy.listCities.and.returnValue(of([{ id: 'c1', name: 'Mumbai' }]));
  spy.listZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai' }]));
  spy.listSabhaKinds.and.returnValue(of([{ id: 'k1', demographic: 'YUVAK', track: 'REGULAR' }]));
  spy.myZones.and.returnValue(of([{ id: 'z1', name: 'West', cityId: 'c1', cityName: 'Mumbai' }]));
  spy.listKshetras.and.returnValue(of([{ id: 'ksh1', name: 'Andheri-7', zoneId: 'z1' }]));
  spy.createCity.and.returnValue(of({ id: 'new' }));
  spy.createZone.and.returnValue(of({ id: 'new' }));
  spy.createSabhaKind.and.returnValue(of({ id: 'new' }));
  spy.createKshetra.and.returnValue(of({ id: 'new' }));
  return spy;
}

function mount(mk: boolean): { fixture: ComponentFixture<StructuralAdminComponent>; api: jasmine.SpyObj<StructuralService> } {
  const api = apiSpy();
  TestBed.configureTestingModule({
    imports: [StructuralAdminComponent],
    providers: [
      { provide: SessionService, useValue: sessionStub(mk) },
      { provide: StructuralService, useValue: api },
    ],
  });
  const fixture = TestBed.createComponent(StructuralAdminComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('StructuralAdminComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('shows the MK tabs and loads their lists for a Madhyastha Karyalaya member', () => {
    const { fixture, api } = mount(true);

    expect(fixture.componentInstance.tabs()).toEqual(['cities', 'zones', 'sabha-kinds']);
    expect(api.listCities).toHaveBeenCalled();
    expect(api.listZones).toHaveBeenCalled();
    expect(api.listSabhaKinds).toHaveBeenCalled();
    expect(api.myZones).not.toHaveBeenCalled();
  });

  it('shows only the Kshetras tab and loads the Sanyojak own zones for a non-MK user', () => {
    const { fixture, api } = mount(false);

    expect(fixture.componentInstance.tabs()).toEqual(['kshetras']);
    expect(api.myZones).toHaveBeenCalled();
    expect(api.listCities).not.toHaveBeenCalled();
  });

  it('creates a city then refreshes the city list', () => {
    const { fixture, api } = mount(true);
    api.listCities.calls.reset();

    fixture.componentInstance.newCityName = 'Surat';
    fixture.componentInstance.createCity();

    expect(api.createCity).toHaveBeenCalledWith('Surat');
    expect(api.listCities).toHaveBeenCalled();
  });

  it('disallows a Sanyukta selective kind in the builder but allows the regular one', () => {
    const { fixture } = mount(true);
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
