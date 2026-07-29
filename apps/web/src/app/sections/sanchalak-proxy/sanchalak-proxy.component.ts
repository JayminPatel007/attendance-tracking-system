import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import {
  ProxyOccurrenceItem,
  ProxySabhaListItem,
  SanchalakProxyBffControllerService,
} from 'shared-data-access';
import { errorMessageFor } from '../../shared/http-error';

/** Human "last seen" hint for the picker; informational only, never gates entry. */
export function lastSeenLabel(lastSeenAt: string | null): string {
  if (!lastSeenAt) {
    return 'Never seen';
  }
  return `Last seen ${new Date(lastSeenAt).toLocaleDateString()}`;
}

/** The Occurrence states a proxy may still shape (cancel / reschedule / re-venue). */
const SHAPEABLE_STATES = new Set(['SCHEDULED', 'RESCHEDULED']);

/**
 * Nirikshak Sanchalak-proxy section (ADR-0001, Slice 14). Two modes:
 *
 * <ul>
 *   <li><b>Picker</b> — the Sabhas assigned to the caller Nirikshak (server-scoped
 *       to their assignment), each with the Sanchalak's informational "last seen"
 *       hint. The hint never gates entry.</li>
 *   <li><b>Acting</b> — once a Sabha is entered, a prominent banner declares the
 *       Nirikshak is acting as its Sanchalak and that every action is audit-logged
 *       as them. The toolkit lists the Sabha's Occurrences and delegates cancel /
 *       reschedule / venue-override to the same backend operations the Sanchalak
 *       uses; authority and the on-behalf-of attribution are the backend's call, so
 *       a denial surfaces as a 403 here.</li>
 * </ul>
 */
@Component({
  selector: 'app-sanchalak-proxy',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './sanchalak-proxy.component.html',
  styleUrl: './sanchalak-proxy.component.scss',
})
export class SanchalakProxyComponent implements OnInit {
  private readonly api = inject(SanchalakProxyBffControllerService);

  readonly lastSeenLabel = lastSeenLabel;

  readonly sabhas = signal<ProxySabhaListItem[]>([]);
  readonly acting = signal<ProxySabhaListItem | null>(null);
  readonly occurrences = signal<ProxyOccurrenceItem[]>([]);
  readonly error = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);

  ngOnInit(): void {
    this.api.sabhas().subscribe((items) => this.sabhas.set(items));
  }

  enterProxy(sabha: ProxySabhaListItem): void {
    this.acting.set(sabha);
    this.error.set(null);
    this.loadOccurrences(sabha.sabhaId);
  }

  exitProxy(): void {
    this.acting.set(null);
    this.occurrences.set([]);
    this.error.set(null);
  }

  shapeable(occurrence: ProxyOccurrenceItem): boolean {
    return SHAPEABLE_STATES.has(occurrence.state);
  }

  cancel(occurrenceId: string, reason: string): void {
    if (reason.trim().length === 0) {
      return;
    }
    this.run(this.api.cancel(occurrenceId, { reason: reason.trim() }));
  }

  overrideVenue(occurrenceId: string, venue: string): void {
    if (venue.trim().length === 0) {
      return;
    }
    this.run(this.api.venueOverride(occurrenceId, { venue: venue.trim() }));
  }

  reschedule(occurrenceId: string, date: string, startTime: string, endTime: string): void {
    if (!date || !startTime || !endTime) {
      return;
    }
    this.run(this.api.reschedule(occurrenceId, { date, startTime, endTime }));
  }

  private run(action: Observable<void>): void {
    const sabha = this.acting();
    if (!sabha || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    action.subscribe({
      next: () => {
        this.submitting.set(false);
        this.loadOccurrences(sabha.sabhaId);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private loadOccurrences(sabhaId: string): void {
    this.api.occurrences(sabhaId).subscribe((items) => this.occurrences.set(items));
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byStatus: {
        403: 'You are not authorized to act on this Sabha.',
        422: 'That action is not allowed on this Occurrence.',
      },
    });
  }
}
