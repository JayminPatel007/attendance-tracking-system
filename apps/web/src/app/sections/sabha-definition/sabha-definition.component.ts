import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PersonPickerComponent } from 'identity-domain';
import { KshetraView, SabhaKindView, ZoneView, demographicLabel } from 'sabha-domain';

import { errorMessageFor } from '../../shared/http-error';
import { SabhaDefinitionService } from './sabha-definition.service';
import { AppointeePayload, DAYS_OF_WEEK, DayOfWeek, DefineSabhaRequest } from './sabha-definition.types';

/** Human label for a kind dropdown option, e.g. "Yuvak Sabha (YSS)". */
export function kindLabel(kind: SabhaKindView): string {
  return `${demographicLabel(kind.demographic)} Sabha (${kind.track})`;
}

type Stage = 'editing' | 'done';

/**
 * The appointee payload from a person picker, or `null` until a Person and
 * credentials are in place. Mirrors the backend `AppointeePayload` (the inline
 * new-Person path is omitted — the standing Sabha has no Occurrence to home a
 * brand-new Person against yet).
 */
function payloadOf(picker: PersonPickerComponent): AppointeePayload | null {
  const id = picker.selectedId();
  if (!id || !picker.username.trim() || !picker.rawPassword) {
    return null;
  }
  return { existingPersonId: id, username: picker.username.trim(), rawPassword: picker.rawPassword };
}

/**
 * Sabha-definition section (ADR-0012, Slice 12): a Nirdeshak creates a standing
 * Sabha and appoints its Sanchalak in one transaction. Pick the kind + Kshetra,
 * choose the schedule shape (weekly carries a fixed day/time the system
 * materializes from; monthly ad-hoc has none — the Sanchalak adds each Occurrence
 * by hand), give the standing venue, and pick the Sanchalak (and optional
 * Sah-Sanchalak) from the Directory via the shared person picker. Authority over
 * the (Kshetra, demographic) scope is the backend's call — a denial surfaces as a
 * 403 here.
 */
@Component({
  selector: 'app-sabha-definition',
  standalone: true,
  imports: [FormsModule, PersonPickerComponent],
  templateUrl: './sabha-definition.component.html',
  styleUrl: './sabha-definition.component.scss',
})
export class SabhaDefinitionComponent implements OnInit {
  private readonly api = inject(SabhaDefinitionService);

  readonly daysOfWeek = DAYS_OF_WEEK;
  readonly kindLabel = kindLabel;

  readonly kinds = signal<SabhaKindView[]>([]);
  readonly zones = signal<ZoneView[]>([]);
  readonly kshetras = signal<KshetraView[]>([]);

  sabhaKindId = '';
  zoneId = '';
  kshetraId = '';

  readonly weekly = signal<boolean>(true);
  dayOfWeek: DayOfWeek = 'SUNDAY';
  startTime = '19:00';
  endTime = '20:30';
  standingVenue = '';

  readonly stage = signal<Stage>('editing');
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.listSabhaKinds().subscribe((k) => this.kinds.set(k));
    this.api.listZones().subscribe((z) => this.zones.set(z));
  }

  onZoneChange(zoneId: string): void {
    this.zoneId = zoneId;
    this.kshetraId = '';
    this.kshetras.set([]);
    if (!zoneId) {
      return;
    }
    this.api.listKshetras(zoneId).subscribe((k) => this.kshetras.set(k));
  }

  setWeekly(weekly: boolean): void {
    this.weekly.set(weekly);
  }

  /** Clears the error banner once an appointee is chosen. */
  onPicked(): void {
    this.error.set(null);
  }

  /**
   * Whether the form is ready to submit. A plain method, not a `computed`: it
   * reads plain template-bound fields (kind/Kshetra/venue/times) and the picker's
   * credential fields that are not signals, so memoization would capture no
   * dependencies and never refresh.
   */
  canSubmit(sanchalak: PersonPickerComponent): boolean {
    if (!this.sabhaKindId || !this.kshetraId || !this.standingVenue.trim()) {
      return false;
    }
    if (this.weekly() && (!this.dayOfWeek || !this.startTime || !this.endTime)) {
      return false;
    }
    return payloadOf(sanchalak) !== null;
  }

  submit(sanchalak: PersonPickerComponent, sah: PersonPickerComponent): void {
    this.error.set(null);
    const request = this.buildRequest(sanchalak, sah);
    if (!request) {
      return;
    }
    this.api.define(request).subscribe({
      next: () => this.stage.set('done'),
      error: (err: HttpErrorResponse) => this.error.set(this.messageFor(err)),
    });
  }

  reset(): void {
    this.sabhaKindId = '';
    this.zoneId = '';
    this.kshetraId = '';
    this.kshetras.set([]);
    this.weekly.set(true);
    this.standingVenue = '';
    this.error.set(null);
    // Toggling back to 'editing' remounts fresh pickers, clearing their state.
    this.stage.set('editing');
  }

  private buildRequest(sanchalak: PersonPickerComponent, sah: PersonPickerComponent): DefineSabhaRequest | null {
    const sanchalakPayload = payloadOf(sanchalak);
    if (!sanchalakPayload) {
      return null;
    }
    const weekly = this.weekly();
    return {
      kshetraId: this.kshetraId.trim(),
      sabhaKindId: this.sabhaKindId,
      weekly,
      dayOfWeek: weekly ? this.dayOfWeek : null,
      startTime: weekly ? this.startTime : null,
      endTime: weekly ? this.endTime : null,
      standingVenue: this.standingVenue.trim(),
      sanchalak: sanchalakPayload,
      sahSanchalak: payloadOf(sah),
    };
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byStatus: { 403: 'You are not authorized to define a Sabha in that Kshetra.' },
    });
  }
}
