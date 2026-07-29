import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CandidateRow, DashboardBffControllerService, DashboardOverview } from 'shared-data-access';

import { DashboardOverviewComponent } from './dashboard-overview.component';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';

function candidate(overrides: Partial<CandidateRow> = {}): CandidateRow {
  return {
    personId: 'p-1',
    personName: 'Ravi Patel',
    homeSabhaId: 's-1',
    sabhaKind: 'REGULAR_YUVAK',
    kshetraName: 'Andheri-7',
    demographic: 'YUVAK',
    missedStreak: 4,
    tier: 'CANDIDATE',
    ...overrides,
  };
}

function overview(overrides: Partial<DashboardOverview> = {}): DashboardOverview {
  return {
    kpis: { totalCandidates: 5, priorityCandidates: 2, sabhasWithCandidates: 3 },
    headlineCandidates: [candidate()],
    ...overrides,
  };
}

function apiSpy(data: DashboardOverview): ApiStub<DashboardBffControllerService> {
  const spy = apiStub<DashboardBffControllerService>('DashboardBffControllerService', ['overview']);
  spy.overview.and.returnValue(of(data));
  return spy;
}

function mount(data: DashboardOverview): {
  fixture: ComponentFixture<DashboardOverviewComponent>;
  api: ApiStub<DashboardBffControllerService>;
} {
  const api = apiSpy(data);
  TestBed.configureTestingModule({
    imports: [DashboardOverviewComponent],
    providers: [{ provide: DashboardBffControllerService, useValue: api }],
  });
  const fixture = TestBed.createComponent(DashboardOverviewComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('DashboardOverviewComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the overview on init', () => {
    const { fixture, api } = mount(overview());

    expect(api.overview).toHaveBeenCalled();
    expect(fixture.componentInstance.kpis()).toEqual({
      totalCandidates: 5,
      priorityCandidates: 2,
      sabhasWithCandidates: 3,
    });
  });

  it('renders the KPI figures in the strip', () => {
    const { fixture } = mount(overview());

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('5');
    expect(text).toContain('2');
    expect(text).toContain('3');
  });

  it('lists the headline candidates with their streak', () => {
    const { fixture } = mount(overview({ headlineCandidates: [candidate({ personName: 'Ravi Patel', missedStreak: 4 })] }));

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ravi Patel');
    expect(text).toContain('4');
  });

  it('shows an empty message when no candidates are in scope', () => {
    const { fixture } = mount(overview({ headlineCandidates: [] }));

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text.toLowerCase()).toContain('no re-engagement candidates');
  });
});
