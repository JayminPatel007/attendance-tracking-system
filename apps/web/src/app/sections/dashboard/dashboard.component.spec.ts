import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DashboardService } from 'analytics-domain';
import { SessionService, WebSession } from 'identity-domain';

import { DashboardComponent } from './dashboard.component';

function apiSpy(): jasmine.SpyObj<DashboardService> {
  const spy = jasmine.createSpyObj<DashboardService>('DashboardService', [
    'overview',
    'people',
    'sabhaTree',
    'thresholds',
    'updateThresholds',
  ]);
  spy.overview.and.returnValue(of({ kpis: { totalCandidates: 0, priorityCandidates: 0, sabhasWithCandidates: 0 }, headlineCandidates: [] }));
  spy.people.and.returnValue(of([]));
  spy.sabhaTree.and.returnValue(of({ zones: [] }));
  spy.thresholds.and.returnValue(of({ candidate: 3, priority: 6 }));
  spy.updateThresholds.and.returnValue(of(undefined));
  return spy;
}

function sessionStub(madhyasthaKaryalaya: boolean): Partial<SessionService> {
  return {
    session: signal<WebSession | null>({ username: 'u', madhyasthaKaryalaya, sections: ['DASHBOARD'] }),
  } as Partial<SessionService>;
}

function mount(madhyasthaKaryalaya = false): ComponentFixture<DashboardComponent> {
  TestBed.configureTestingModule({
    imports: [DashboardComponent],
    providers: [
      { provide: DashboardService, useValue: apiSpy() },
      { provide: SessionService, useValue: sessionStub(madhyasthaKaryalaya) },
    ],
  });
  const fixture = TestBed.createComponent(DashboardComponent);
  fixture.detectChanges();
  return fixture;
}

describe('DashboardComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('defaults to the overview tab', () => {
    const fixture = mount();

    expect(fixture.componentInstance.tab()).toBe('overview');
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).not.toBeNull();
  });

  it('switches to the people tab', () => {
    const fixture = mount();
    fixture.componentInstance.select('people');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-dashboard-people')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-dashboard-overview')).toBeNull();
  });

  it('switches to the sabha tree tab', () => {
    const fixture = mount();
    fixture.componentInstance.select('tree');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-dashboard-sabha-tree')).not.toBeNull();
  });

  it('shows the MK threshold editor on the overview tab only for an MK caller', () => {
    const fixture = mount(true);

    expect(fixture.nativeElement.querySelector('app-threshold-editor input')).not.toBeNull();
  });

  it('hides the threshold editor controls for a non-MK caller', () => {
    const fixture = mount(false);

    expect(fixture.nativeElement.querySelector('app-threshold-editor input')).toBeNull();
  });
});
