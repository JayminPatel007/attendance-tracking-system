import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { demographicLabel } from 'sabha-domain';

import { suggestPassword, suggestUsername } from '../role-appointment/appointment.credentials';
import { NameCandidate, PersonResponse } from '../role-appointment/appointment.types';
import { KshetraView, SabhaKindView, ZoneView } from '../structural-admin/structural.types';
import { SabhaDefinitionService } from './sabha-definition.service';
import { AppointeePayload, DAYS_OF_WEEK, DayOfWeek, DefineSabhaRequest } from './sabha-definition.types';

/** Human label for a kind dropdown option, e.g. "Yuvak Sabha (YSS)". */
export function kindLabel(kind: SabhaKindView): string {
  return `${demographicLabel(kind.demographic)} Sabha (${kind.track})`;
}

type Stage = 'editing' | 'done';

/**
 * One appointee slot of the definition form — the Sanchalak, and the optional
 * Sah-Sanchalak. Directory-first (Slice 11): search by name within the chosen
 * Kshetra or by mobile, pick an existing Person, and the initial credentials are
 * auto-suggested from their name (editable; uniqueness is the backend's call).
 */
export class AppointeePicker {
  nameQuery = '';
  mobileQuery = '';
  readonly candidates = signal<NameCandidate[]>([]);
  readonly mobileMatch = signal<PersonResponse | null>(null);
  readonly selectedId = signal<string | null>(null);
  selectedName = '';
  username = '';
  rawPassword = '';

  pick(personId: string, fullName: string): void {
    this.selectedId.set(personId);
    this.selectedName = fullName;
    this.username = suggestUsername(fullName);
    this.rawPassword = suggestPassword();
  }

  clear(): void {
    this.selectedId.set(null);
    this.selectedName = '';
    this.username = '';
    this.rawPassword = '';
    this.candidates.set([]);
    this.mobileMatch.set(null);
  }

  /** The request payload, or `null` until a Person and credentials are in place. */
  payload(): AppointeePayload | null {
    const id = this.selectedId();
    if (!id || !this.username.trim() || !this.rawPassword) {
      return null;
    }
    return { existingPersonId: id, username: this.username.trim(), rawPassword: this.rawPassword };
  }
}

/**
 * Sabha-definition section (ADR-0012, Slice 12): a Nirdeshak creates a standing
 * Sabha and appoints its Sanchalak in one transaction. Pick the kind + Kshetra,
 * choose the schedule shape (weekly carries a fixed day/time the system
 * materializes from; monthly ad-hoc has none — the Sanchalak adds each Occurrence
 * by hand), give the standing venue, and pick the Sanchalak (and optional
 * Sah-Sanchalak) from the Directory. Authority over the (Kshetra, demographic)
 * scope is the backend's call — a denial surfaces as a 403 here.
 */
@Component({
  selector: 'app-sabha-definition',
  standalone: true,
  imports: [FormsModule],
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

  readonly sanchalak = new AppointeePicker();
  readonly sah = new AppointeePicker();

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

  searchSanchalakByName(): void {
    this.searchByName(this.sanchalak);
  }

  searchSanchalakByMobile(): void {
    this.searchByMobile(this.sanchalak);
  }

  pickSanchalak(personId: string, fullName: string): void {
    this.sanchalak.pick(personId, fullName);
    this.error.set(null);
  }

  searchSahByName(): void {
    this.searchByName(this.sah);
  }

  searchSahByMobile(): void {
    this.searchByMobile(this.sah);
  }

  pickSah(personId: string, fullName: string): void {
    this.sah.pick(personId, fullName);
  }

  clearSah(): void {
    this.sah.clear();
  }

  private searchByName(picker: AppointeePicker): void {
    const name = picker.nameQuery.trim();
    const kshetraId = this.kshetraId.trim();
    if (!name || !kshetraId) {
      return;
    }
    picker.mobileMatch.set(null);
    this.api.searchByName(kshetraId, name).subscribe((c) => picker.candidates.set(c));
  }

  private searchByMobile(picker: AppointeePicker): void {
    const mobile = picker.mobileQuery.trim();
    if (!mobile) {
      return;
    }
    picker.candidates.set([]);
    this.api.searchByMobile(mobile).subscribe({
      next: (person) => picker.mobileMatch.set(person),
      error: () => picker.mobileMatch.set(null),
    });
  }

  /**
   * Whether the form is ready to submit. A plain method, not a `computed`: it
   * reads plain template-bound fields (kind/Kshetra/venue/times) that are not
   * signals, so memoization would capture no dependencies and never refresh.
   */
  canSubmit(): boolean {
    if (!this.sabhaKindId || !this.kshetraId || !this.standingVenue.trim()) {
      return false;
    }
    if (this.weekly() && (!this.dayOfWeek || !this.startTime || !this.endTime)) {
      return false;
    }
    return this.sanchalak.payload() !== null;
  }

  submit(): void {
    this.error.set(null);
    const request = this.buildRequest();
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
    this.sanchalak.clear();
    this.sah.clear();
    this.error.set(null);
    this.stage.set('editing');
  }

  private buildRequest(): DefineSabhaRequest | null {
    const sanchalak = this.sanchalak.payload();
    if (!sanchalak) {
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
      sanchalak,
      sahSanchalak: this.sah.payload(),
    };
  }

  private messageFor(err: HttpErrorResponse): string {
    if (err.status === 409 && err.error?.code === 'USERNAME_TAKEN') {
      return 'That username is already taken — choose another before submitting.';
    }
    if (err.status === 403) {
      return 'You are not authorized to define a Sabha in that Kshetra.';
    }
    return 'Something went wrong — please try again.';
  }
}
