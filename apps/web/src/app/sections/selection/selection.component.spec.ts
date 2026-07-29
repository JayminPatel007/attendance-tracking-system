import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { PendingNominationItem, SelectedPersonItem, SelectionBffControllerService } from 'shared-data-access';
import { SelectionComponent } from './selection.component';
import { ApiStub, apiStub } from '../../shared/api-stub.testing';

function nomination(overrides: Partial<PendingNominationItem> = {}): PendingNominationItem {
  return {
    nominationId: 'nom-1',
    personId: 'person-1',
    personName: 'Roster Person',
    regularSabhaId: 'sabha-2',
    selectiveSabhaId: 'sabha-16',
    demographic: 'YUVAK',
    track: 'YSS',
    nominatedBy: 'user-3',
    nominatedByName: 'Tracer Sanchalak',
    nominatedAt: '2026-06-05T10:00:00Z',
    ...overrides,
  };
}

function selected(overrides: Partial<SelectedPersonItem> = {}): SelectedPersonItem {
  return {
    nominationId: 'nom-1',
    personId: 'person-1',
    personName: 'Roster Person',
    selectiveSabhaId: 'sabha-16',
    demographic: 'YUVAK',
    track: 'YSS',
    decidedBy: 'user-9',
    decidedByName: 'Tracer Nirdeshak',
    decidedAt: '2026-06-06T10:00:00Z',
    ...overrides,
  };
}

function apiSpy(
  pending: PendingNominationItem[],
  selectedPeople: SelectedPersonItem[],
): ApiStub<SelectionBffControllerService> {
  const spy = apiStub<SelectionBffControllerService>('SelectionBffControllerService', [
    'queue',
    'selected',
    'approve',
    'reject',
    'deselect',
  ]);
  spy.queue.and.returnValue(of(pending));
  spy.selected.and.returnValue(of(selectedPeople));
  spy.approve.and.returnValue(of(undefined));
  spy.reject.and.returnValue(of(undefined));
  spy.deselect.and.returnValue(of(undefined));
  return spy;
}

function mount(
  pending: PendingNominationItem[] = [],
  selectedPeople: SelectedPersonItem[] = [],
): { fixture: ComponentFixture<SelectionComponent>; api: ApiStub<SelectionBffControllerService> } {
  const api = apiSpy(pending, selectedPeople);
  TestBed.configureTestingModule({
    imports: [SelectionComponent],
    providers: [{ provide: SelectionBffControllerService, useValue: api }],
  });
  const fixture = TestBed.createComponent(SelectionComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('SelectionComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the pending queue and the selected list on init', () => {
    const { fixture, api } = mount([nomination()], [selected()]);

    expect(api.queue).toHaveBeenCalled();
    expect(api.selected).toHaveBeenCalled();
    expect(fixture.componentInstance.pending().length).toBe(1);
    expect(fixture.componentInstance.selected().length).toBe(1);
  });

  it('approves a nomination and reloads both lists', () => {
    const { fixture, api } = mount([nomination()]);

    fixture.componentInstance.approve('nom-1');

    expect(api.approve).toHaveBeenCalledWith('nom-1');
    expect(api.queue).toHaveBeenCalledTimes(2);
    expect(api.selected).toHaveBeenCalledTimes(2);
  });

  it('rejects a nomination with the comment and reloads', () => {
    const { fixture, api } = mount([nomination()]);

    fixture.componentInstance.reject('nom-1', 'Not ready');

    expect(api.reject).toHaveBeenCalledWith('nom-1', { reason: 'Not ready' });
    expect(api.queue).toHaveBeenCalledTimes(2);
  });

  it('does not reject without a comment', () => {
    const { fixture, api } = mount([nomination()]);

    fixture.componentInstance.reject('nom-1', '   ');

    expect(api.reject).not.toHaveBeenCalled();
  });

  it('deselects a selected Person with their person and selective Sabha and reloads', () => {
    const { fixture, api } = mount([], [selected()]);

    fixture.componentInstance.deselect(selected());

    expect(api.deselect).toHaveBeenCalledWith({ personId: 'person-1', selectiveSabhaId: 'sabha-16' });
    expect(api.selected).toHaveBeenCalledTimes(2);
  });

  it('surfaces a 403 from an action as an authorization message', () => {
    const { fixture, api } = mount([nomination()]);
    api.approve.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    fixture.componentInstance.approve('nom-1');

    expect(fixture.componentInstance.error()).toContain('not authorized');
  });

  it('surfaces a 409 from an action as an already-decided message', () => {
    const { fixture, api } = mount([nomination()]);
    api.approve.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    fixture.componentInstance.approve('nom-1');

    expect(fixture.componentInstance.error()).toContain('already');
  });
});
