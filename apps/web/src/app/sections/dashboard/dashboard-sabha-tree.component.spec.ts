import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DashboardBffControllerService, SabhaTree } from 'shared-data-access';

import { DashboardSabhaTreeComponent } from './dashboard-sabha-tree.component';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';

function tree(): SabhaTree {
  return {
    zones: [
      {
        zoneId: 'z-1',
        zoneName: 'Mumbai West',
        candidateCount: 5,
        kshetras: [
          {
            kshetraId: 'k-1',
            kshetraName: 'Andheri-7',
            candidateCount: 5,
            sabhas: [{ sabhaId: 's-1', sabhaKind: 'REGULAR_YUVAK', candidateCount: 5 }],
          },
        ],
      },
      {
        zoneId: null,
        zoneName: '',
        candidateCount: 2,
        kshetras: [
          {
            kshetraId: 'k-9',
            kshetraName: 'Tracer Kshetra',
            candidateCount: 2,
            sabhas: [{ sabhaId: 's-9', sabhaKind: 'REGULAR_BAAL', candidateCount: 2 }],
          },
        ],
      },
    ],
  };
}

function apiSpy(data: SabhaTree): ApiStub<DashboardBffControllerService> {
  const spy = apiStub<DashboardBffControllerService>('DashboardBffControllerService', ['sabhaTree']);
  spy.sabhaTree.and.returnValue(of(data));
  return spy;
}

function mount(data: SabhaTree): {
  fixture: ComponentFixture<DashboardSabhaTreeComponent>;
  api: ApiStub<DashboardBffControllerService>;
} {
  const api = apiSpy(data);
  TestBed.configureTestingModule({
    imports: [DashboardSabhaTreeComponent],
    providers: [{ provide: DashboardBffControllerService, useValue: api }],
  });
  const fixture = TestBed.createComponent(DashboardSabhaTreeComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('DashboardSabhaTreeComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the tree on init', () => {
    const { fixture, api } = mount(tree());

    expect(api.sabhaTree).toHaveBeenCalled();
    expect(fixture.componentInstance.zones().length).toBe(2);
  });

  it('renders a candidate count at the zone level', () => {
    const { fixture } = mount(tree());

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Mumbai West');
    expect(text).toContain('5');
  });

  it('labels the no-zone bucket as Unzoned', () => {
    const { fixture } = mount(tree());

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Unzoned');
  });

  it('keys the no-zone bucket without colliding with real zones', () => {
    const { fixture } = mount(tree());
    const c = fixture.componentInstance;

    expect(c.zoneKey(c.zones()[0])).not.toBe(c.zoneKey(c.zones()[1]));
  });

  it('toggles a zone open and closed', () => {
    const { fixture } = mount(tree());
    const c = fixture.componentInstance;
    const key = c.zoneKey(c.zones()[0]);

    expect(c.isZoneOpen(key)).toBeFalse();
    c.toggleZone(key);
    expect(c.isZoneOpen(key)).toBeTrue();
    c.toggleZone(key);
    expect(c.isZoneOpen(key)).toBeFalse();
  });

  it('toggles a kshetra open and closed', () => {
    const { fixture } = mount(tree());
    const c = fixture.componentInstance;

    expect(c.isKshetraOpen('k-1')).toBeFalse();
    c.toggleKshetra('k-1');
    expect(c.isKshetraOpen('k-1')).toBeTrue();
  });
});
