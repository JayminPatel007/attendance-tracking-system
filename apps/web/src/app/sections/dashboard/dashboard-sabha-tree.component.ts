import { Component, OnInit, inject, signal } from '@angular/core';
import { DashboardService, ZoneNode } from 'analytics-domain';
import { kindLabel } from 'sabha-domain';

/** The key used for the bucket of Kshetras with no Zone (the tracer seed). */
const UNZONED_KEY = '__unzoned__';

/**
 * Sabha analytics tree (section C, ADR-0010, Slice 15): drills Zone → Kshetra →
 * Sabha with a re-engagement candidate count rolled up at every level, scoped to
 * the caller. A Kshetra with no Zone surfaces under an "Unzoned" bucket
 * (`zoneId === null`). Expansion state is held client-side.
 */
@Component({
  selector: 'app-dashboard-sabha-tree',
  standalone: true,
  imports: [],
  templateUrl: './dashboard-sabha-tree.component.html',
  styleUrl: './dashboard-sabha-tree.component.scss',
})
export class DashboardSabhaTreeComponent implements OnInit {
  private readonly api = inject(DashboardService);

  readonly kindLabel = kindLabel;

  readonly zones = signal<ZoneNode[]>([]);
  private readonly openZones = signal<ReadonlySet<string>>(new Set());
  private readonly openKshetras = signal<ReadonlySet<string>>(new Set());

  ngOnInit(): void {
    this.api.sabhaTree().subscribe((tree) => this.zones.set(tree.zones));
  }

  /** A stable key for a Zone node; the no-Zone bucket gets its own sentinel. */
  zoneKey(zone: ZoneNode): string {
    return zone.zoneId ?? UNZONED_KEY;
  }

  /** The display name for a Zone, falling back to "Unzoned" for the null bucket. */
  zoneLabel(zone: ZoneNode): string {
    return zone.zoneId === null ? 'Unzoned' : zone.zoneName;
  }

  isZoneOpen(key: string): boolean {
    return this.openZones().has(key);
  }

  toggleZone(key: string): void {
    this.openZones.set(toggled(this.openZones(), key));
  }

  isKshetraOpen(kshetraId: string): boolean {
    return this.openKshetras().has(kshetraId);
  }

  toggleKshetra(kshetraId: string): void {
    this.openKshetras.set(toggled(this.openKshetras(), kshetraId));
  }
}

/** A new Set with `key` toggled in or out — keeps the signal value immutable. */
function toggled(current: ReadonlySet<string>, key: string): ReadonlySet<string> {
  const next = new Set(current);
  if (!next.delete(key)) {
    next.add(key);
  }
  return next;
}
