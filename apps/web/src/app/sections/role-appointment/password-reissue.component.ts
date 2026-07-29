import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { suggestPassword } from 'identity-domain';

import { PasswordReissueControllerService } from 'shared-data-access';

import { errorMessageFor } from '../../shared/http-error';

type Stage = 'editing' | 'done';

/**
 * Assigner-reissue action (ADR-0004, Slice 18B) — the fallback for a User who has
 * lost their mobile entirely and so cannot self-serve a reset. The appointing
 * Karyakar (or a Madhyastha Karyalaya member, for Sants per ADR-0011) issues a
 * fresh, throwaway password the User must change on next login. An authenticated
 * admin act on the `/bff/password-reissue` session; whether this caller may
 * reissue for the target is the backend's call (403 otherwise). Lives in the
 * role-appointment section because reissue is an appointer's responsibility.
 */
@Component({
  selector: 'app-password-reissue',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './password-reissue.component.html',
  styleUrl: './password-reissue.component.scss',
})
export class PasswordReissueComponent {
  private readonly api = inject(PasswordReissueControllerService);

  targetUserId = '';
  newPassword = suggestPassword();

  readonly stage = signal<Stage>('editing');
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  /** The password actually issued, revealed on the done step to read out. */
  readonly issuedPassword = signal('');

  regeneratePassword(): void {
    this.newPassword = suggestPassword();
  }

  submit(): void {
    const targetUserId = this.targetUserId.trim();
    if (!targetUserId || !this.newPassword || this.busy()) {
      return;
    }
    const issued = this.newPassword;
    this.busy.set(true);
    this.error.set(null);
    this.api.reissue({ targetUserId, newPassword: issued }).subscribe({
      next: () => {
        this.issuedPassword.set(issued);
        this.stage.set('done');
        this.busy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(
          errorMessageFor(err, {
            byStatus: {
              403: 'You are not the appointer for that User (nor an MK member), so you cannot reissue their password.',
            },
          }),
        );
      },
    });
  }

  reset(): void {
    this.targetUserId = '';
    this.newPassword = suggestPassword();
    this.issuedPassword.set('');
    this.error.set(null);
    this.stage.set('editing');
  }
}
