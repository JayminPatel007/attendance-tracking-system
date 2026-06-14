import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardService } from 'analytics-domain';
import { SessionService } from 'identity-domain';

import { errorMessageFor } from '../../shared/http-error';

/**
 * MK threshold editor (ADR-0010, Slice 15). Self-gating: it renders nothing — and
 * reads nothing — unless the session belongs to a Madhyastha Karyalaya member, the
 * same authority the BFF enforces on the PUT (403 otherwise). The domain invariant
 * `priority >= candidate >= 1` is checked client-side for fast feedback and again
 * server-side (422); both surface the same message.
 */
@Component({
  selector: 'app-threshold-editor',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './threshold-editor.component.html',
  styleUrl: './threshold-editor.component.scss',
})
export class ThresholdEditorComponent implements OnInit {
  private readonly api = inject(DashboardService);
  private readonly sessions = inject(SessionService);

  readonly isMk = computed(() => this.sessions.session()?.madhyasthaKaryalaya ?? false);

  candidate = 0;
  priority = 0;
  readonly error = signal<string | null>(null);
  readonly saved = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  ngOnInit(): void {
    if (!this.isMk()) {
      return;
    }
    this.api.thresholds().subscribe((current) => {
      this.candidate = current.candidate;
      this.priority = current.priority;
    });
  }

  /** The MK-owned invariant: a candidate needs at least one miss, priority is stricter. */
  valid(): boolean {
    return this.candidate >= 1 && this.priority >= this.candidate;
  }

  save(): void {
    this.saved.set(false);
    if (!this.valid()) {
      this.error.set('Priority must be at least the candidate threshold, which must be at least 1.');
      return;
    }
    if (this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.api.updateThresholds({ candidate: this.candidate, priority: this.priority }).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byStatus: {
        422: 'Priority must be at least the candidate threshold, which must be at least 1.',
        403: 'Only the Madhyastha Karyalaya may change the thresholds.',
      },
    });
  }
}
