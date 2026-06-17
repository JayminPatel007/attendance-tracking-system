import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  Gender,
  NameCandidate,
  PersonPickerComponent,
  suggestPassword,
  suggestUsername,
} from 'identity-domain';

import { errorMessageFor } from '../../shared/http-error';
import { AppointmentService } from './appointment.service';
import { PasswordReissueComponent } from './password-reissue.component';
import {
  AppointableRole,
  AppointmentRequest,
  DEMOGRAPHICS,
  Demographic,
  NewPersonPayload,
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
 * Pick the role and its scope, then either find the appointee with the shared
 * person picker (Directory-first, with auto-suggested credentials) or "+ Create
 * new Person" — the inline-create and its name soft-warn (Slice 6) stay
 * feature-private here. Authority, the mobile hard block, the name soft-warn, and
 * username uniqueness are decided by the backend; this surfaces each outcome
 * before/at commit.
 */
@Component({
  selector: 'app-role-appointment',
  standalone: true,
  imports: [FormsModule, PersonPickerComponent, PasswordReissueComponent],
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

  /** The Kshetra the directory picker searches names within (distinct from a Kshetra-scoped role's `kshetraId`). */
  searchKshetraId = '';

  /** The shared Directory picker, rendered only while not creating a new Person. */
  readonly picker = viewChild(PersonPickerComponent);

  // Inline new-Person create (feature-private), with its own auto-suggested credentials.
  readonly creatingNew = signal<boolean>(false);
  newPerson: NewPersonPayload = blankNewPerson();
  newUsername = '';
  newPassword = '';

  readonly stage = signal<Stage>('editing');
  readonly error = signal<string | null>(null);
  readonly softWarn = signal<NameCandidate[]>([]);

  onRoleChange(role: AppointableRole): void {
    this.role.set(role);
    this.resetSelection();
  }

  /** Clears the error banner once an appointee is picked from the Directory. */
  onPicked(): void {
    this.error.set(null);
  }

  startCreateNew(): void {
    this.creatingNew.set(true);
    this.newPerson = blankNewPerson();
    this.suggestNewCredentials('');
  }

  cancelCreateNew(): void {
    this.creatingNew.set(false);
    this.newPerson = blankNewPerson();
    this.newUsername = '';
    this.newPassword = '';
  }

  /** Re-suggest the username/password from the new Person's name as it is typed. */
  onNewPersonNameChange(): void {
    this.suggestNewCredentials(this.newPerson.fullName);
  }

  private suggestNewCredentials(fullName: string): void {
    this.newUsername = suggestUsername(fullName);
    this.newPassword = suggestPassword();
  }

  canSubmit(): boolean {
    if (this.creatingNew()) {
      return !!this.newUsername.trim() && !!this.newPassword;
    }
    const picker = this.picker();
    return !!picker && picker.selectedId() !== null && !!picker.username.trim() && !!picker.rawPassword;
  }

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
    this.picker()?.clear();
    this.searchKshetraId = '';
    this.cancelCreateNew();
    this.softWarn.set([]);
    this.error.set(null);
  }

  private buildRequest(overrideDuplicateWarning: boolean): AppointmentRequest | null {
    if (this.creatingNew()) {
      return {
        ...this.scope(),
        username: this.newUsername.trim(),
        rawPassword: this.newPassword,
        newPerson: { ...this.newPerson, overrideDuplicateWarning },
      };
    }
    const picker = this.picker();
    if (picker?.selectedId()) {
      return {
        ...this.scope(),
        username: picker.username.trim(),
        rawPassword: picker.rawPassword,
        existingPersonId: picker.selectedId(),
      };
    }
    return null;
  }

  /** The role + scope half of the request, shared by both the existing and new-Person paths. */
  private scope(): Omit<AppointmentRequest, 'username' | 'rawPassword'> {
    return {
      role: this.role(),
      sabhaId: this.scopeKind() === 'SABHA' ? this.sabhaId.trim() : null,
      kshetraId: this.scopeKind() === 'KSHETRA' ? this.kshetraId.trim() : null,
      zoneId: this.scopeKind() === 'ZONE' ? this.zoneId.trim() : null,
      cityId: this.scopeKind() === 'CITY' ? this.cityId.trim() : null,
      demographic: this.scopeKind() === 'SABHA' ? null : (this.demographic || null),
    };
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
