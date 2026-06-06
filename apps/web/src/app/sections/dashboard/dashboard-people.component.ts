import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CandidateRow, DashboardService } from 'analytics-domain';
import { demographicLabel, kindLabel } from 'sabha-domain';

import { TierBadgeComponent } from './tier-badge.component';

/**
 * People analytics (section B, ADR-0010, Slice 15): a Directory-level table of the
 * re-engagement candidates in the caller's scope (already scoped server-side),
 * sorted by missed streak. Filtering — name search and priority-only — is purely
 * client-side over the loaded payload.
 */
@Component({
  selector: 'app-dashboard-people',
  standalone: true,
  imports: [FormsModule, TierBadgeComponent],
  templateUrl: './dashboard-people.component.html',
  styleUrl: './dashboard-people.component.scss',
})
export class DashboardPeopleComponent implements OnInit {
  private readonly api = inject(DashboardService);

  readonly kindLabel = kindLabel;
  readonly demographicLabel = demographicLabel;

  private readonly rows = signal<CandidateRow[]>([]);
  private readonly _search = signal('');
  readonly priorityOnly = signal(false);

  get search(): string {
    return this._search();
  }
  set search(value: string) {
    this._search.set(value);
  }

  /** The rows matching the active filters, sorted by missed streak (longest first). */
  readonly filtered = computed<CandidateRow[]>(() => {
    const needle = this._search().trim().toLowerCase();
    const priorityOnly = this.priorityOnly();
    return this.rows()
      .filter((r) => (priorityOnly ? r.tier === 'PRIORITY' : true))
      .filter((r) => (needle ? r.personName.toLowerCase().includes(needle) : true))
      .sort((a, b) => b.missedStreak - a.missedStreak);
  });

  ngOnInit(): void {
    this.api.people().subscribe((rows) => this.rows.set(rows));
  }
}
