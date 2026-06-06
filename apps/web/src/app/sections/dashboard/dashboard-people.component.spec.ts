import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CandidateRow, DashboardService } from 'analytics-domain';

import { DashboardPeopleComponent } from './dashboard-people.component';

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

function apiSpy(rows: CandidateRow[]): jasmine.SpyObj<DashboardService> {
  const spy = jasmine.createSpyObj<DashboardService>('DashboardService', ['people']);
  spy.people.and.returnValue(of(rows));
  return spy;
}

function mount(rows: CandidateRow[]): {
  fixture: ComponentFixture<DashboardPeopleComponent>;
  api: jasmine.SpyObj<DashboardService>;
} {
  const api = apiSpy(rows);
  TestBed.configureTestingModule({
    imports: [DashboardPeopleComponent],
    providers: [{ provide: DashboardService, useValue: api }],
  });
  const fixture = TestBed.createComponent(DashboardPeopleComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('DashboardPeopleComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the candidate rows on init', () => {
    const { fixture, api } = mount([candidate()]);

    expect(api.people).toHaveBeenCalled();
    expect(fixture.componentInstance.filtered().length).toBe(1);
  });

  it('filters by a case-insensitive name search', () => {
    const { fixture } = mount([
      candidate({ personId: 'p-1', personName: 'Ravi Patel' }),
      candidate({ personId: 'p-2', personName: 'Amit Shah' }),
    ]);
    const c = fixture.componentInstance;

    c.search = 'amit';

    expect(c.filtered().map((r) => r.personName)).toEqual(['Amit Shah']);
  });

  it('filters to priority candidates only', () => {
    const { fixture } = mount([
      candidate({ personId: 'p-1', tier: 'CANDIDATE' }),
      candidate({ personId: 'p-2', tier: 'PRIORITY' }),
    ]);
    const c = fixture.componentInstance;

    c.priorityOnly.set(true);

    expect(c.filtered().map((r) => r.personId)).toEqual(['p-2']);
  });

  it('sorts the rows by missed streak, longest first', () => {
    const { fixture } = mount([
      candidate({ personId: 'p-1', missedStreak: 3 }),
      candidate({ personId: 'p-2', missedStreak: 7 }),
    ]);

    expect(fixture.componentInstance.filtered().map((r) => r.personId)).toEqual(['p-2', 'p-1']);
  });

  it('shows an empty message when the filters match nothing', () => {
    const { fixture } = mount([candidate({ personName: 'Ravi Patel' })]);
    fixture.componentInstance.search = 'zzz';
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text.toLowerCase()).toContain('no people');
  });
});
