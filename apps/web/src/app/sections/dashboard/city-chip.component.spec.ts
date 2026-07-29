import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CityChip, CityOption, DashboardBffControllerService } from 'shared-data-access';

import { CityChipComponent } from './city-chip.component';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';

function apiSpy(scope: CityChip): ApiStub<DashboardBffControllerService> {
  const spy = apiStub<DashboardBffControllerService>('DashboardBffControllerService', ['scope', 'chooseCity']);
  spy.scope.and.returnValue(of(scope));
  spy.chooseCity.and.returnValue(of(undefined));
  return spy;
}

function mount(scope: CityChip): {
  fixture: ComponentFixture<CityChipComponent>;
  api: ApiStub<DashboardBffControllerService>;
} {
  const api = apiSpy(scope);
  TestBed.configureTestingModule({
    imports: [CityChipComponent],
    providers: [{ provide: DashboardBffControllerService, useValue: api }],
  });
  const fixture = TestBed.createComponent(CityChipComponent);
  fixture.detectChanges();
  return { fixture, api };
}

const CITIES: CityOption[] = [
  { id: 'city-1', name: 'Ahmedabad' },
  { id: 'city-2', name: 'Surat' },
];

describe('CityChipComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('shows an interactive City picker for a Sant', () => {
    const { fixture } = mount({ sant: true, selectedCityId: 'city-1', cities: CITIES });

    const select = (fixture.nativeElement as HTMLElement).querySelector('select');
    expect(select).not.toBeNull();
    expect(fixture.componentInstance.cities()).toEqual(CITIES);
    expect(fixture.componentInstance.selectedCityId()).toBe('city-1');
  });

  it('shows a non-interactive scope indicator (no picker) for a non-Sant', () => {
    const { fixture } = mount({ sant: false, selectedCityId: null, cities: [] });

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('select')).toBeNull();
    expect(el.textContent?.trim()).not.toBe('');
  });

  it('persists a pick via the BFF and announces it', () => {
    const { fixture, api } = mount({ sant: true, selectedCityId: 'city-1', cities: CITIES });
    const picked: string[] = [];
    fixture.componentInstance.cityPicked.subscribe((id) => picked.push(id));

    fixture.componentInstance.choose('city-2');

    expect(api.chooseCity).toHaveBeenCalledWith({ cityId: 'city-2' });
    expect(fixture.componentInstance.selectedCityId()).toBe('city-2');
    expect(picked).toEqual(['city-2']);
  });

  it('announces the loaded scope so the shell can prompt an unpicked Sant', () => {
    const loaded: { sant: boolean; selectedCityId: string | null }[] = [];
    const api = apiSpy({ sant: true, selectedCityId: null, cities: CITIES });
    TestBed.configureTestingModule({
      imports: [CityChipComponent],
      providers: [{ provide: DashboardBffControllerService, useValue: api }],
    });
    const fixture = TestBed.createComponent(CityChipComponent);
    fixture.componentInstance.scopeLoaded.subscribe((s) => loaded.push(s));
    fixture.detectChanges();

    expect(loaded).toEqual([{ sant: true, selectedCityId: null }]);
  });
});
