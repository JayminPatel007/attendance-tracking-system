import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuditLogComponent } from './audit-log.component';
import { AuditLogService } from './audit-log.service';
import { AuditEntry } from 'shared-data-access';

function entry(overrides: Partial<AuditEntry> = {}): AuditEntry {
  return {
    id: 'entry-1',
    at: '2026-06-08T19:30:00Z',
    actorUserId: 'user-7',
    actorName: 'Nirdeshak One',
    onBehalfOfUserId: null,
    onBehalfName: null,
    targetType: 'OCCURRENCE',
    targetId: 'occ-1',
    action: 'REOPENED',
    detail: 'Forgot to mark Ravi',
    ...overrides,
  };
}

function apiSpy(entries: AuditEntry[]): jasmine.SpyObj<AuditLogService> {
  const spy = jasmine.createSpyObj<AuditLogService>('AuditLogService', ['list']);
  spy.list.and.returnValue(of(entries));
  return spy;
}

function mount(
  entries: AuditEntry[] = [],
  queryParams: Record<string, string> = {},
): {
  fixture: ComponentFixture<AuditLogComponent>;
  api: jasmine.SpyObj<AuditLogService>;
} {
  const api = apiSpy(entries);
  TestBed.configureTestingModule({
    imports: [AuditLogComponent],
    providers: [
      { provide: AuditLogService, useValue: api },
      { provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap(queryParams)) } },
    ],
  });
  const fixture = TestBed.createComponent(AuditLogComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('AuditLogComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the full audit feed on init', () => {
    const { fixture, api } = mount([entry()]);

    expect(api.list).toHaveBeenCalledWith({});
    expect(fixture.componentInstance.entries().length).toBe(1);
  });

  it('renders a row per entry, even when two entries share one source id', () => {
    const { fixture } = mount([
      entry({ id: 'sel-1', action: 'NOMINATED' }),
      entry({ id: 'sel-1', action: 'SELECTED' }),
    ]);

    const rows: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('NOMINATED');
    expect(rows[1].textContent).toContain('SELECTED');
  });

  it('attributes a proxy action to the actor on behalf of the Sanchalak', () => {
    const { fixture } = mount([
      entry({ actorName: 'Nirikshak N', onBehalfOfUserId: 'user-4', onBehalfName: 'Sanchalak S' }),
    ]);

    const row: HTMLElement = fixture.nativeElement.querySelector('tbody tr');
    expect(row.textContent).toContain('Nirikshak N');
    expect(row.textContent).toContain('on behalf of Sanchalak S');
  });

  it('applies the filter form to the feed, sending only the set fields', () => {
    const { fixture, api } = mount();
    const c = fixture.componentInstance;
    c.targetType = 'OCCURRENCE';
    c.action = ' REOPENED ';
    c.actorUserId = 'user-7';
    c.from = '2026-06-01';
    c.to = '2026-06-09';
    c.proxyOnly = true;

    c.apply();

    expect(api.list).toHaveBeenCalledWith({
      targetType: 'OCCURRENCE',
      action: 'REOPENED',
      actorUserId: 'user-7',
      from: '2026-06-01',
      to: '2026-06-09',
      proxyOnly: true,
    });
  });

  it('pre-applies a deep-linked entity drill-down from the query params', () => {
    const { fixture, api } = mount([entry()], { targetType: 'OCCURRENCE', targetId: 'occ-1' });

    expect(api.list).toHaveBeenCalledWith({ targetType: 'OCCURRENCE', targetId: 'occ-1' });
    const drill: HTMLElement = fixture.nativeElement.querySelector('.audit__drill');
    expect(drill.textContent).toContain('OCCURRENCE');
    expect(drill.textContent).toContain('occ-1');
  });

  it('clears the drill-down back to the feed, keeping the target type as a filter', () => {
    const { fixture, api } = mount([entry()], { targetType: 'OCCURRENCE', targetId: 'occ-1' });

    fixture.componentInstance.clearDrillDown();
    fixture.detectChanges();

    expect(api.list).toHaveBeenCalledWith({ targetType: 'OCCURRENCE' });
    expect(fixture.nativeElement.querySelector('.audit__drill')).toBeNull();
  });

  it('shows an empty state when no entries match the filters', () => {
    const { fixture } = mount([]);

    const empty: HTMLElement = fixture.nativeElement.querySelector('.audit__empty');
    expect(empty.textContent).toContain('No audit entries');
  });

  it('surfaces a 403 as an authorization message', () => {
    const { fixture, api } = mount();
    api.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    fixture.componentInstance.apply();
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('not authorized');
    expect(fixture.nativeElement.querySelector('.audit__error')).not.toBeNull();
  });
});
