import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SessionService } from 'identity-domain';
import {
  CityView,
  DEMOGRAPHICS,
  Demographic,
  KshetraView,
  SabhaKindView,
  TRACKS,
  Track,
  ZoneView,
  isAllowedKind,
} from 'sabha-domain';

import { StructuralService } from './structural.service';

type Tab = 'cities' | 'zones' | 'sabha-kinds' | 'kshetras';

const TAB_LABELS: Record<Tab, string> = {
  cities: 'Cities',
  zones: 'Zones',
  'sabha-kinds': 'Sabha Kinds',
  kshetras: 'Kshetras',
};

/**
 * Structural admin section (ADR-0009, ADR-0024): role-scoped tabs over the BFF.
 * A Madhyastha Karyalaya member creates Cities and Sabha Kinds; a Regional Team
 * member creates Zones within a City they belong to (ADR-0024); a Sanyojak
 * creates Kshetras within their own Zone. The tab set is decided by the session's
 * authority — the same authority the backend enforces — not the client.
 */
@Component({
  selector: 'app-structural-admin',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './structural-admin.component.html',
  styleUrl: './structural-admin.component.scss',
})
export class StructuralAdminComponent implements OnInit {
  private readonly sessions = inject(SessionService);
  private readonly api = inject(StructuralService);

  readonly tabLabel = TAB_LABELS;
  readonly demographics = DEMOGRAPHICS;
  readonly tracks = TRACKS;

  readonly isMk = computed(() => this.sessions.session()?.madhyasthaKaryalaya ?? false);
  readonly isRegionalTeam = computed(() => this.sessions.session()?.regionalTeam ?? false);
  readonly tabs = computed<Tab[]>(() => {
    if (this.isMk()) {
      return ['cities', 'sabha-kinds'];
    }
    if (this.isRegionalTeam()) {
      return ['zones'];
    }
    return ['kshetras'];
  });
  readonly activeTab = signal<Tab>('cities');

  readonly cities = signal<CityView[]>([]);
  readonly zones = signal<ZoneView[]>([]);
  readonly sabhaKinds = signal<SabhaKindView[]>([]);
  readonly myCities = signal<CityView[]>([]);
  readonly myZones = signal<ZoneView[]>([]);
  readonly kshetras = signal<KshetraView[]>([]);

  newCityName = '';
  newZoneName = '';
  newZoneCityId = '';
  newKindDemographic: Demographic | '' = '';
  newKindTrack: Track = 'REGULAR';
  newKshetraName = '';
  selectedZoneId = '';

  ngOnInit(): void {
    this.activeTab.set(this.tabs()[0]);
    if (this.isMk()) {
      this.refreshCities();
      this.refreshSabhaKinds();
    } else if (this.isRegionalTeam()) {
      this.refreshZones();
      this.api.myCities().subscribe((c) => {
        this.myCities.set(c);
        this.newZoneCityId = c[0]?.id ?? '';
      });
    } else {
      this.api.myZones().subscribe((z) => {
        this.myZones.set(z);
        this.selectedZoneId = z[0]?.id ?? '';
        this.refreshKshetras();
      });
    }
  }

  selectTab(tab: Tab): void {
    this.activeTab.set(tab);
  }

  // --- Cities ---
  createCity(): void {
    const name = this.newCityName.trim();
    if (!name) {
      return;
    }
    this.api.createCity(name).subscribe(() => {
      this.newCityName = '';
      this.refreshCities();
    });
  }

  private refreshCities(): void {
    this.api.listCities().subscribe((c) => this.cities.set(c));
  }

  // --- Zones ---
  createZone(): void {
    const name = this.newZoneName.trim();
    if (!name || !this.newZoneCityId) {
      return;
    }
    this.api.createZone(this.newZoneCityId, name).subscribe(() => {
      this.newZoneName = '';
      this.newZoneCityId = '';
      this.refreshZones();
    });
  }

  private refreshZones(): void {
    this.api.listZones().subscribe((z) => this.zones.set(z));
  }

  // --- Sabha Kinds ---
  canRegisterKind(): boolean {
    return this.newKindDemographic !== '' && isAllowedKind(this.newKindDemographic, this.newKindTrack);
  }

  createSabhaKind(): void {
    if (!this.canRegisterKind()) {
      return;
    }
    this.api.createSabhaKind(this.newKindDemographic as Demographic, this.newKindTrack).subscribe(() => {
      this.newKindDemographic = '';
      this.newKindTrack = 'REGULAR';
      this.refreshSabhaKinds();
    });
  }

  private refreshSabhaKinds(): void {
    this.api.listSabhaKinds().subscribe((k) => this.sabhaKinds.set(k));
  }

  // --- Kshetras ---
  createKshetra(): void {
    const name = this.newKshetraName.trim();
    if (!name || !this.selectedZoneId) {
      return;
    }
    this.api.createKshetra(this.selectedZoneId, name).subscribe(() => {
      this.newKshetraName = '';
      this.refreshKshetras();
    });
  }

  private refreshKshetras(): void {
    if (!this.selectedZoneId) {
      this.kshetras.set([]);
      return;
    }
    this.api.listKshetras(this.selectedZoneId).subscribe((k) => this.kshetras.set(k));
  }
}
