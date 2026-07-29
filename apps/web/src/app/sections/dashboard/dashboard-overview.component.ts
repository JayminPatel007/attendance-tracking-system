import { Component, OnInit, inject, signal } from '@angular/core';
import { CandidateRow, DashboardBffControllerService, Kpis } from 'shared-data-access';
import { kindLabel } from 'sabha-domain';

import { TierBadgeComponent } from './tier-badge.component';

/**
 * Dashboard overview (section A, ADR-0010, Slice 15): the KPI strip computed over
 * the caller's in-scope candidate set and the re-engagement candidate headline
 * list, both already scoped to the caller's tier by the BFF. The MK threshold
 * editor sits beside this section but is its own self-gating component.
 */
@Component({
  selector: 'app-dashboard-overview',
  standalone: true,
  imports: [TierBadgeComponent],
  templateUrl: './dashboard-overview.component.html',
  styleUrl: './dashboard-overview.component.scss',
})
export class DashboardOverviewComponent implements OnInit {
  private readonly api = inject(DashboardBffControllerService);

  readonly kindLabel = kindLabel;

  readonly kpis = signal<Kpis | null>(null);
  readonly headlineCandidates = signal<CandidateRow[]>([]);

  ngOnInit(): void {
    this.api.overview().subscribe((overview) => {
      this.kpis.set(overview.kpis);
      this.headlineCandidates.set(overview.headlineCandidates);
    });
  }
}
