import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CityChip, DashboardBffControllerService, WebSessionResponse } from 'shared-data-access';
import { SessionService } from 'identity-domain';

import { DashboardComponent } from './dashboard.component';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';

const ROLE_SCOPED: CityChip = { sant: false, selectedCityId: null, cities: [] };

function apiSpy(scope: CityChip): ApiStub<DashboardBffControllerService> {
  const spy = apiStub<DashboardBffControllerService>('DashboardBffControllerService', [
    'overview',
    'people',
    'sabhaTree',
    'thresholds',
    'updateThresholds',
    'scope',
    'chooseCity',
  ]);
  spy.overview.and.returnValue(of({ kpis: { totalCandidates: 0, priorityCandidates: 0, sabhasWithCandidates: 0 }, headlineCandidates: [] }));
  spy.people.and.returnValue(of([]));
  spy.sabhaTree.and.returnValue(of({ zones: [] }));
  spy.thresholds.and.returnValue(of({ candidate: 3, priority: 6 }));
  spy.updateThresholds.and.returnValue(of(undefined));
  spy.scope.and.returnValue(of(scope));
  spy.chooseCity.and.returnValue(of(undefined));
  return spy;
}

function sessionStub(madhyasthaKaryalaya: boolean): Partial<SessionService> {
  return {
    session: signal<WebSessionResponse | null>({ username: 'u', madhyasthaKaryalaya, regionalTeam: false, sections: ['DASHBOARD'] }),
  } as Partial<SessionService>;
}

function mount(
  madhyasthaKaryalaya = false,
  scope: CityChip = ROLE_SCOPED,
): { fixture: ComponentFixture<DashboardComponent>; api: ApiStub<DashboardBffControllerService> } {
  const api = apiSpy(scope);
  TestBed.configureTestingModule({
    imports: [DashboardComponent],
    providers: [
      { provide: DashboardBffControllerService, useValue: api },
      { provide: SessionService, useValue: sessionStub(madhyasthaKaryalaya) },
    ],
  });
  const fixture = TestBed.createComponent(DashboardComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('DashboardComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('defaults to the overview tab', () => {
    const { fixture } = mount();

    expect(fixture.componentInstance.tab()).toBe('overview');
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).not.toBeNull();
  });

  it('switches to the people tab', () => {
    const { fixture } = mount();
    fixture.componentInstance.select('people');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-dashboard-people')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).toBeNull();
  });

  it('switches to the sabha tree tab', () => {
    const { fixture } = mount();
    fixture.componentInstance.select('tree');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-dashboard-sabha-tree')).not.toBeNull();
  });

  it('shows the MK threshold editor on the overview tab only for an MK caller', () => {
    const { fixture } = mount(true);

    expect(fixture.nativeElement.querySelector('app-threshold-editor input')).not.toBeNull();
  });

  it('hides the threshold editor controls for a non-MK caller', () => {
    const { fixture } = mount(false);

    expect(fixture.nativeElement.querySelector('app-threshold-editor input')).toBeNull();
  });

  it('renders the scope chip in the header', () => {
    const { fixture } = mount();

    expect(fixture.nativeElement.querySelector('app-city-chip')).not.toBeNull();
  });

  it('prompts a Sant who has not picked a City and hides the sections', () => {
    const { fixture } = mount(false, { sant: true, selectedCityId: null, cities: [{ id: 'c1', name: 'Ahmedabad' }] });

    expect(fixture.componentInstance.showPrompt()).toBeTrue();
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).toBeNull();
  });

  it('reveals and reloads the sections once the Sant picks a City', () => {
    const { fixture } = mount(false, { sant: true, selectedCityId: null, cities: [{ id: 'c1', name: 'Ahmedabad' }] });
    const before = fixture.componentInstance.reloadToken();

    fixture.componentInstance.onCityPicked('c1');
    fixture.detectChanges();

    expect(fixture.componentInstance.showPrompt()).toBeFalse();
    expect(fixture.componentInstance.reloadToken()).toBeGreaterThan(before);
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).not.toBeNull();
  });
});
