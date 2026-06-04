import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { SanchalakProxyComponent, lastSeenLabel } from './sanchalak-proxy.component';
import { SanchalakProxyService } from './sanchalak-proxy.service';
import { ProxyOccurrence, ProxySabha } from './sanchalak-proxy.types';

function sabha(overrides: Partial<ProxySabha> = {}): ProxySabha {
  return {
    sabhaId: 'sabha-1',
    sabhaLabel: 'REGULAR_YUVAK · Kshetra Tracer',
    sanchalakUserId: 'user-4',
    sanchalakName: 'Proxy Sanchalak',
    lastSeenAt: '2026-06-03T19:30:00Z',
    ...overrides,
  };
}

function occurrence(overrides: Partial<ProxyOccurrence> = {}): ProxyOccurrence {
  return {
    id: 'occ-1',
    effectiveDate: '2026-08-02',
    state: 'SCHEDULED',
    venue: 'Proxy Hall',
    ...overrides,
  };
}

function apiSpy(
  sabhas: ProxySabha[],
  occurrences: ProxyOccurrence[],
): jasmine.SpyObj<SanchalakProxyService> {
  const spy = jasmine.createSpyObj<SanchalakProxyService>('SanchalakProxyService', [
    'listSabhas',
    'listOccurrences',
    'cancel',
    'overrideVenue',
    'reschedule',
  ]);
  spy.listSabhas.and.returnValue(of(sabhas));
  spy.listOccurrences.and.returnValue(of(occurrences));
  spy.cancel.and.returnValue(of(undefined));
  spy.overrideVenue.and.returnValue(of(undefined));
  spy.reschedule.and.returnValue(of(undefined));
  return spy;
}

function mount(
  sabhas: ProxySabha[],
  occurrences: ProxyOccurrence[] = [],
): { fixture: ComponentFixture<SanchalakProxyComponent>; api: jasmine.SpyObj<SanchalakProxyService> } {
  const api = apiSpy(sabhas, occurrences);
  TestBed.configureTestingModule({
    imports: [SanchalakProxyComponent],
    providers: [{ provide: SanchalakProxyService, useValue: api }],
  });
  const fixture = TestBed.createComponent(SanchalakProxyComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('SanchalakProxyComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the assignable Sabhas on init and starts in the picker', () => {
    const { fixture, api } = mount([sabha()]);

    expect(api.listSabhas).toHaveBeenCalled();
    expect(fixture.componentInstance.sabhas().length).toBe(1);
    expect(fixture.componentInstance.acting()).toBeNull();
  });

  it('entering proxy mode loads the Sabha\'s Occurrences and sets the acting banner', () => {
    const { fixture, api } = mount([sabha()], [occurrence()]);
    const c = fixture.componentInstance;

    c.enterProxy(sabha());

    expect(api.listOccurrences).toHaveBeenCalledWith('sabha-1');
    expect(c.acting()?.sabhaLabel).toBe('REGULAR_YUVAK · Kshetra Tracer');
    expect(c.occurrences().length).toBe(1);
  });

  it('exiting proxy mode returns to the picker', () => {
    const { fixture } = mount([sabha()], [occurrence()]);
    const c = fixture.componentInstance;
    c.enterProxy(sabha());

    c.exitProxy();

    expect(c.acting()).toBeNull();
  });

  it('cancels an Occurrence as proxy with the reason and reloads the toolkit', () => {
    const { fixture, api } = mount([sabha()], [occurrence()]);
    const c = fixture.componentInstance;
    c.enterProxy(sabha());

    c.cancel('occ-1', 'Sanchalak away');

    expect(api.cancel).toHaveBeenCalledWith('occ-1', 'Sanchalak away');
    expect(api.listOccurrences).toHaveBeenCalledTimes(2);
  });

  it('does not cancel without a reason', () => {
    const { fixture, api } = mount([sabha()], [occurrence()]);
    const c = fixture.componentInstance;
    c.enterProxy(sabha());

    c.cancel('occ-1', '   ');

    expect(api.cancel).not.toHaveBeenCalled();
  });

  it('surfaces a 403 from a proxy action as an authorization message', () => {
    const { fixture, api } = mount([sabha()], [occurrence()]);
    api.cancel.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    const c = fixture.componentInstance;
    c.enterProxy(sabha());

    c.cancel('occ-1', 'let me in');

    expect(c.error()).toContain('not authorized');
  });

  it('labels a missing last-seen as never seen', () => {
    expect(lastSeenLabel(null)).toBe('Never seen');
    expect(lastSeenLabel('2026-06-03T19:30:00Z')).not.toBe('Never seen');
  });
});
