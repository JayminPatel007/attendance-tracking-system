import { Component, computed, signal } from '@angular/core';

import { CityChipComponent } from './city-chip.component';
import { DashboardOverviewComponent } from './dashboard-overview.component';
import { DashboardPeopleComponent } from './dashboard-people.component';
import { DashboardSabhaTreeComponent } from './dashboard-sabha-tree.component';
import { ThresholdEditorComponent } from './threshold-editor.component';

type Tab = 'overview' | 'people' | 'tree';

const TAB_LABELS: { tab: Tab; label: string }[] = [
  { tab: 'overview', label: 'Dashboard overview' },
  { tab: 'people', label: 'People analytics' },
  { tab: 'tree', label: 'Sabha analytics' },
];

/**
 * Re-engagement dashboard shell section (ADR-0010, Slice 15/17). Hosts the three
 * analytics views as internal tabs — overview (A), People (B), Sabha tree (C) —
 * and, on the overview tab, the self-gating MK threshold editor. The scope chip
 * in the header is a Sant's City picker (Slice 17): every section read is scoped
 * server-side to the Sant's chosen City, so picking a City re-reads all three by
 * remounting the body ({@link reloadToken}). A Sant who has not picked sees a
 * prompt instead of empty sections.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CityChipComponent,
    DashboardOverviewComponent,
    DashboardPeopleComponent,
    DashboardSabhaTreeComponent,
    ThresholdEditorComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly tabs = TAB_LABELS;
  readonly tab = signal<Tab>('overview');

  /** The chip's loaded scope: null until the chip reports, then drives the prompt. */
  private readonly scope = signal<{ sant: boolean; selectedCityId: string | null } | null>(null);

  /** Bumped on a City pick to remount (and so re-read) the section body. */
  readonly reloadToken = signal(0);

  /** A Sant who has not yet chosen a City: show a prompt, not empty sections. */
  readonly showPrompt = computed(() => {
    const s = this.scope();
    return !!s && s.sant && !s.selectedCityId;
  });

  select(tab: Tab): void {
    this.tab.set(tab);
  }

  onScopeLoaded(scope: { sant: boolean; selectedCityId: string | null }): void {
    this.scope.set(scope);
  }

  onCityPicked(cityId: string): void {
    this.scope.update((prev) => ({ sant: prev?.sant ?? true, selectedCityId: cityId }));
    this.reloadToken.update((n) => n + 1);
  }
}
