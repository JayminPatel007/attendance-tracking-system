import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { errorMessageFor } from '../../shared/http-error';
import { AppointmentService } from './appointment.service';
import { suggestPassword, suggestUsername } from './appointment.credentials';
import { PasswordReissueComponent } from './password-reissue.component';
import {
  AppointableRole,
  AppointmentRequest,
  DEMOGRAPHICS,
  Demographic,
  Gender,
  NameCandidate,
  NewPersonPayload,
  PersonResponse,
  ROLE_SCOPE,
  ScopeKind,
} from './appointment.types';

const ROLES: readonly AppointableRole[] = [
  'SANCHALAK',
  'SAH_SANCHALAK',
  'NIRIKSHAK',
  'SAH_NIRDESHAK',
  'NIRDESHAK',
  'SANYOJAK',
  'REGIONAL_TEAM',
  'SANT',
];

const ROLE_LABELS: Record<AppointableRole, string> = {
  SANCHALAK: 'Sanchalak',
  SAH_SANCHALAK: 'Sah-Sanchalak',
  NIRIKSHAK: 'Nirikshak',
  SAH_NIRDESHAK: 'Sah-Nirdeshak',
  NIRDESHAK: 'Nirdeshak',
  SANYOJAK: 'Sanyojak',
  REGIONAL_TEAM: 'Regional Team member',
  SANT: 'Sant',
};

type Stage = 'editing' | 'done';

/**
 * Role appointment section (ADR-0011, Slice 11): the unified web flow that
 * searches the Directory first and creates a Person inline when no match exists.
 * One long form — pick the role and its scope, find the appointee by mobile or
 * name (de-dup reuse, Slice 6) or "+ Create new Person", confirm the
 * auto-suggested credentials, and submit one appointment. Authority, the mobile
 * hard block, the name soft-warn, and username uniqueness are decided by the
 * backend; this surfaces each outcome before/at commit.
 */
@Component({
  selector: 'app-role-appointment',
  standalone: true,
  imports: [FormsModule, PasswordReissueComponent],
  templateUrl: './role-appointment.component.html',
  styleUrl: './role-appointment.component.scss',
})
export class RoleAppointmentComponent {
  private readonly api = inject(AppointmentService);

  readonly roles = ROLES;
  readonly roleLabel = ROLE_LABELS;
  readonly demographics = DEMOGRAPHICS;

  readonly role = signal<AppointableRole>('SANCHALAK');
  readonly scopeKind = computed<ScopeKind>(() => ROLE_SCOPE[this.role()]);

  // Scope inputs — only the one for the current role's ScopeKind is shown.
  sabhaId = '';
  kshetraId = '';
  zoneId = '';
  cityId = '';
  demographic: Demographic | '' = '';

  // Directory-first search.
  searchMobile = '';
  searchName = '';
  searchKshetraId = '';
  readonly mobileMatch = signal<PersonResponse | null>(null);
  readonly nameCandidates = signal<NameCandidate[]>([]);

  // Appointee selection.
  readonly selectedPersonId = signal<string | null>(null);
  readonly creatingNew = signal<boolean>(false);
  newPerson: NewPersonPayload = blankNewPerson();

  // Credentials (auto-suggested, editable).
  username = '';
  rawPassword = '';

  readonly stage = signal<Stage>('editing');
  readonly error = signal<string | null>(null);
  readonly softWarn = signal<NameCandidate[]>([]);

  onRoleChange(role: AppointableRole): void {
    this.role.set(role);
    this.resetSelection();
  }

  searchByMobile(): void {
    const mobile = this.searchMobile.trim();
    if (!mobile) {
      return;
    }
    this.nameCandidates.set([]);
    this.api.searchByMobile(mobile).subscribe({
      next: (person) => this.mobileMatch.set(person),
      error: () => this.mobileMatch.set(null),
    });
  }

  searchByName(): void {
    const name = this.searchName.trim();
    const kshetraId = this.searchKshetraId.trim();
    if (!name || !kshetraId) {
      return;
    }
    this.mobileMatch.set(null);
    this.api.searchByName(kshetraId, name).subscribe((c) => this.nameCandidates.set(c));
  }

  pickExisting(personId: string): void {
    this.selectedPersonId.set(personId);
    this.creatingNew.set(false);
    this.error.set(null);
  }

  startCreateNew(): void {
    this.creatingNew.set(true);
    this.selectedPersonId.set(null);
    this.newPerson = blankNewPerson();
    this.suggestCredentials('');
  }

  /** Re-suggest the username/password from the new Person's name as it is typed. */
  onNewPersonNameChange(): void {
    this.suggestCredentials(this.newPerson.fullName);
  }

  private suggestCredentials(fullName: string): void {
    this.username = suggestUsername(fullName);
    this.rawPassword = suggestPassword();
  }

  readonly canSubmit = computed(() => {
    if (!this.username || !this.rawPassword) {
      return false;
    }
    return this.selectedPersonId() !== null || this.creatingNew();
  });

  submit(overrideDuplicateWarning = false): void {
    this.error.set(null);
    const request = this.buildRequest(overrideDuplicateWarning);
    if (!request) {
      return;
    }
    this.api.appoint(request).subscribe({
      next: (response) => {
        if (response.requiresOverride) {
          // Name soft-warn (Slice 6): show the close matches and let the appointer override.
          this.softWarn.set(response.candidates);
          return;
        }
        this.stage.set('done');
      },
      error: (err: HttpErrorResponse) => this.error.set(this.messageFor(err)),
    });
  }

  overrideSoftWarn(): void {
    this.softWarn.set([]);
    this.submit(true);
  }

  reset(): void {
    this.resetSelection();
    this.stage.set('editing');
  }

  private resetSelection(): void {
    this.mobileMatch.set(null);
    this.nameCandidates.set([]);
    this.selectedPersonId.set(null);
    this.creatingNew.set(false);
    this.softWarn.set([]);
    this.error.set(null);
    this.newPerson = blankNewPerson();
    this.username = '';
    this.rawPassword = '';
  }

  private buildRequest(overrideDuplicateWarning: boolean): AppointmentRequest | null {
    const base: AppointmentRequest = {
      role: this.role(),
      sabhaId: this.scopeKind() === 'SABHA' ? this.sabhaId.trim() : null,
      kshetraId: this.scopeKind() === 'KSHETRA' ? this.kshetraId.trim() : null,
      zoneId: this.scopeKind() === 'ZONE' ? this.zoneId.trim() : null,
      cityId: this.scopeKind() === 'CITY' ? this.cityId.trim() : null,
      demographic: this.scopeKind() === 'SABHA' ? null : (this.demographic || null),
      username: this.username.trim(),
      rawPassword: this.rawPassword,
    };

    if (this.selectedPersonId()) {
      return { ...base, existingPersonId: this.selectedPersonId() };
    }
    if (this.creatingNew()) {
      return { ...base, newPerson: { ...this.newPerson, overrideDuplicateWarning } };
    }
    return null;
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byCode: {
        USERNAME_TAKEN: 'That username is already taken — choose another before submitting.',
      },
      byStatus: {
        409: 'That mobile number already belongs to someone in the Directory.',
        403: 'You are not authorized to make this appointment in that scope.',
      },
    });
  }
}

function blankNewPerson(): NewPersonPayload {
  return {
    fullName: '',
    gender: 'MALE' as Gender,
    dateOfBirth: null,
    mobile: null,
    guardianPersonId: null,
    homeSabhaId: '',
    overrideDuplicateWarning: false,
  };
}
