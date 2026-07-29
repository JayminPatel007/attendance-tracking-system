import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { demographicLabel } from 'sabha-domain';
import { Observable } from 'rxjs';

import { errorMessageFor } from '../../shared/http-error';
import { PendingNominationItem, SelectedPersonItem, SelectionBffControllerService } from 'shared-data-access';

/**
 * A human label for the selective Sabha a Person joins, e.g. `Yuvak (YSS)` —
 * reuses the canonical {@link demographicLabel} decoder and mirrors the
 * `Demographic (Track)` shape `kindLabel` uses elsewhere.
 */
export function trackLabel(demographic: string, track: string): string {
  return `${demographicLabel(demographic)} (${track})`;
}

/**
 * Demographic Nirdeshak's selection section (ADR-0006, Slice 16). Two lists:
 *
 * <ul>
 *   <li><b>Pending queue</b> — the nominations awaiting a decision (scoped
 *       server-side to the caller's NIRDESHAK rows). Each is approved (adds the
 *       selective Home Sabha additively) or rejected with a comment.</li>
 *   <li><b>Currently selected</b> — the People already approved into a selective
 *       Sabha, each deselectable (removes only the selective Home Sabha).</li>
 * </ul>
 *
 * Authority is the backend's call, so a denial surfaces as a 403 here; a 409 means
 * the nomination was already decided or the Person already selected.
 */
@Component({
  selector: 'app-selection',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './selection.component.html',
  styleUrl: './selection.component.scss',
})
export class SelectionComponent implements OnInit {
  private readonly api = inject(SelectionBffControllerService);

  readonly trackLabel = trackLabel;

  readonly pending = signal<PendingNominationItem[]>([]);
  readonly selected = signal<SelectedPersonItem[]>([]);
  readonly error = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);

  ngOnInit(): void {
    this.reload();
  }

  approve(nominationId: string): void {
    this.run(this.api.approve(nominationId));
  }

  reject(nominationId: string, reason: string): void {
    if (reason.trim().length === 0) {
      return;
    }
    this.run(this.api.reject(nominationId, { reason: reason.trim() }));
  }

  deselect(person: SelectedPersonItem): void {
    this.run(this.api.deselect({ personId: person.personId, selectiveSabhaId: person.selectiveSabhaId }));
  }

  private run(action: Observable<void>): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    action.subscribe({
      next: () => {
        this.submitting.set(false);
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private reload(): void {
    this.api.queue().subscribe((items) => this.pending.set(items));
    this.api.selected().subscribe((items) => this.selected.set(items));
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byStatus: {
        403: 'You are not authorized to decide this nomination.',
        409: 'That Person has already been selected, or the nomination was already decided.',
        422: 'That action can\'t be completed for this nomination.',
      },
    });
  }
}
