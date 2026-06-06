import { Component, signal } from '@angular/core';

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
 * Re-engagement dashboard shell section (ADR-0010, Slice 15). Hosts the three
 * analytics views as internal tabs — overview (A), People (B), Sabha tree (C) —
 * and, on the overview tab, the self-gating MK threshold editor. All data is
 * scoped to the caller's tier server-side; each view loads its own slice.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
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

  select(tab: Tab): void {
    this.tab.set(tab);
  }
}
