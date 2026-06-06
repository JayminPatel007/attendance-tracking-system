import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { kindLabel } from 'sabha-domain';

import { OccurrenceReopenService } from './occurrence-reopen.service';
import { OccurrenceListItem } from './occurrence-reopen.types';

/**
 * Occurrence-reopen section (ADR-0001, Slice 13). Two-pane: the Occurrences the
 * caller may reopen on the left (server-scoped to their Kshetra-tier authority),
 * the selected Occurrence's detail on the right. A Finalized Occurrence shows a
 * reason-required reopen card; a reopened one carries a visible badge and its
 * last reason. The reopen authority itself (Nirikshak / Nirdeshak / Sah-Nirdeshak,
 * never the Sanchalak or the oversight tiers) is the backend's call — a denial
 * surfaces as a 403 here.
 */
@Component({
  selector: 'app-occurrence-reopen',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './occurrence-reopen.component.html',
  styleUrl: './occurrence-reopen.component.scss',
})
export class OccurrenceReopenComponent implements OnInit {
  private readonly api = inject(OccurrenceReopenService);

  readonly kindLabel = kindLabel;

  readonly occurrences = signal<OccurrenceListItem[]>([]);
  readonly selectedId = signal<string | null>(null);
  reason = '';
  readonly error = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);

  readonly selected = computed<OccurrenceListItem | null>(() => {
    const id = this.selectedId();
    return this.occurrences().find((o) => o.occurrenceId === id) ?? null;
  });

  ngOnInit(): void {
    this.load();
  }

  select(occurrenceId: string): void {
    this.selectedId.set(occurrenceId);
    this.reason = '';
    this.error.set(null);
  }

  canReopen(): boolean {
    const occurrence = this.selected();
    return occurrence !== null && occurrence.state === 'FINALIZED' && this.reason.trim().length > 0;
  }

  reopen(): void {
    const occurrence = this.selected();
    if (!occurrence || !this.canReopen() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.api.reopen(occurrence.occurrenceId, this.reason.trim()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.reason = '';
        this.load(occurrence.occurrenceId);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private load(reselect?: string): void {
    this.api.list().subscribe((items) => {
      this.occurrences.set(items);
      if (reselect && items.some((o) => o.occurrenceId === reselect)) {
        this.selectedId.set(reselect);
      } else if (!items.some((o) => o.occurrenceId === this.selectedId())) {
        this.selectedId.set(null);
      }
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    if (err.status === 403) {
      return 'You are not authorized to reopen this Occurrence.';
    }
    if (err.status === 422) {
      return 'A reason is required to reopen.';
    }
    return 'Something went wrong — please try again.';
  }
}
