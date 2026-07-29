import { Component, computed, input } from '@angular/core';
import { CandidateRow } from 'shared-data-access';

/**
 * The re-engagement tier pill shown wherever a candidate is listed (overview
 * headline, People table). Owns the single mapping from {@link CandidateRow.TierEnum} to its
 * label and visual treatment so the two lists can't drift apart.
 */
@Component({
  selector: 'app-tier-badge',
  standalone: true,
  template: `<span class="tier-badge" [class.tier-badge--priority]="tier() === 'PRIORITY'">{{ label() }}</span>`,
  styles: `
    .tier-badge {
      padding: 0.1rem 0.55rem;
      border-radius: 999px;
      background: #eef4ff;
      color: #1d4ed8;
      font-size: 0.72rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .tier-badge--priority {
      background: #fdecec;
      color: #b00020;
    }
  `,
})
export class TierBadgeComponent {
  readonly tier = input.required<CandidateRow.TierEnum>();
  readonly label = computed(() => (this.tier() === 'PRIORITY' ? 'Priority' : 'Candidate'));
}
