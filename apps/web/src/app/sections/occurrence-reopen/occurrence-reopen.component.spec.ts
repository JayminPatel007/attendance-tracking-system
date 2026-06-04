import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { OccurrenceReopenComponent, kindLabel } from './occurrence-reopen.component';
import { OccurrenceReopenService } from './occurrence-reopen.service';
import { OccurrenceListItem } from './occurrence-reopen.types';

function finalized(overrides: Partial<OccurrenceListItem> = {}): OccurrenceListItem {
  return {
    occurrenceId: 'occ-1',
    date: '2026-05-17',
    state: 'FINALIZED',
    kshetraName: 'Kshetra Tracer',
    sabhaKind: 'REGULAR_YUVAK',
    venue: 'Tracer Hall',
    reopened: false,
    lastReopenReason: null,
    ...overrides,
  };
}

function apiSpy(items: OccurrenceListItem[]): jasmine.SpyObj<OccurrenceReopenService> {
  const spy = jasmine.createSpyObj<OccurrenceReopenService>('OccurrenceReopenService', ['list', 'reopen']);
  spy.list.and.returnValue(of(items));
  spy.reopen.and.returnValue(of(undefined));
  return spy;
}

function mount(items: OccurrenceListItem[]): {
  fixture: ComponentFixture<OccurrenceReopenComponent>;
  api: jasmine.SpyObj<OccurrenceReopenService>;
} {
  const api = apiSpy(items);
  TestBed.configureTestingModule({
    imports: [OccurrenceReopenComponent],
    providers: [{ provide: OccurrenceReopenService, useValue: api }],
  });
  const fixture = TestBed.createComponent(OccurrenceReopenComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('OccurrenceReopenComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the Occurrences in scope on init', () => {
    const { fixture, api } = mount([finalized()]);

    expect(api.list).toHaveBeenCalled();
    expect(fixture.componentInstance.occurrences().length).toBe(1);
  });

  it('cannot reopen until a Finalized Occurrence is selected and a reason is entered', () => {
    const { fixture } = mount([finalized()]);
    const c = fixture.componentInstance;

    expect(c.canReopen()).toBeFalse();

    c.select('occ-1');
    expect(c.canReopen()).toBeFalse();

    c.reason = 'Forgot to mark Ravi';
    expect(c.canReopen()).toBeTrue();
  });

  it('cannot reopen an Occurrence that is not Finalized', () => {
    const { fixture } = mount([finalized({ state: 'OPEN_FOR_MARKING' })]);
    const c = fixture.componentInstance;

    c.select('occ-1');
    c.reason = 'should not matter';

    expect(c.canReopen()).toBeFalse();
  });

  it('posts the reopen with the reason and reloads the list', () => {
    const { fixture, api } = mount([finalized()]);
    const c = fixture.componentInstance;
    c.select('occ-1');
    c.reason = 'Forgot to mark Ravi';

    c.reopen();

    expect(api.reopen).toHaveBeenCalledWith('occ-1', 'Forgot to mark Ravi');
    expect(api.list).toHaveBeenCalledTimes(2);
    expect(c.reason).toBe('');
  });

  it('surfaces a 403 as an authorization message and does not reload', () => {
    const { fixture, api } = mount([finalized()]);
    api.reopen.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    const c = fixture.componentInstance;
    c.select('occ-1');
    c.reason = 'let me in';

    c.reopen();

    expect(c.error()).toContain('not authorized');
    expect(api.list).toHaveBeenCalledTimes(1);
  });

  it('labels a denormalized kind token for display', () => {
    expect(kindLabel('REGULAR_YUVAK')).toBe('Yuvak (Regular)');
  });
});
