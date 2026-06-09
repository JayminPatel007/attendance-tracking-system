import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

/** Contact of whoever can reissue a locked-out user's password (their appointer,
 * or a Madhyastha Karyalaya member for Sants). */
export interface AppointerContact {
  name: string;
  mobile: string;
}

/**
 * Outbound adapter for the **public** self-service password-reset surfaces
 * (ADR-0004, Slice 18). Every call rides the public API chain (`/api/**`,
 * `permitAll`, CSRF disabled, no session): a locked-out user reaches them before
 * authenticating, so they are plain JSON calls with no session and no XSRF
 * header. The authenticated assigner-reissue is an appointer action and lives on
 * {@link AppointmentService} instead.
 */
@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly http = inject(HttpClient);

  /** Step 1 (public): send an OTP to the user's registered mobile. */
  requestReset(username: string): Observable<string> {
    return this.http
      .post<{ resetId: string }>('/api/password-reset/request', { username })
      .pipe(map((r) => r.resetId));
  }

  /** Step 2 (public): exchange a correct OTP for a short-lived reset token. */
  verifyOtp(resetId: string, otpCode: string): Observable<string> {
    return this.http
      .post<{ resetToken: string }>('/api/password-reset/verify', { resetId, otpCode })
      .pipe(map((r) => r.resetToken));
  }

  /** Step 3 (public): set the new password against the reset token. */
  completeReset(resetToken: string, newPassword: string): Observable<void> {
    return this.http
      .post<void>('/api/password-reset/complete', { resetToken, newPassword })
      .pipe(map(() => undefined));
  }

  /** Public lost-mobile lookup: who can reissue this username's password. */
  whoAppointedMe(username: string): Observable<AppointerContact[]> {
    return this.http
      .get<{ contacts: AppointerContact[] }>('/api/who-appointed-me', { params: { username } })
      .pipe(map((r) => r.contacts));
  }
}
