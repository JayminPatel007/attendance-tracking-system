import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PasswordResetRestControllerService } from 'shared-data-access';

/** The step of the self-service reset the screen is showing. */
type Stage = 'username' | 'otp' | 'password' | 'done';

/**
 * Self-service password-reset screen (ADR-0004, Slice 18B) — a **public** route
 * outside the authenticated shell. A locked-out user reaches it from the login
 * screen and walks: enter username → receive an OTP on their registered mobile →
 * enter the OTP → set a new password → done. Calls the public `/api/password-reset/**`
 * chain directly (no session, no XSRF). The backend decides every failure; this
 * surfaces each — unknown user, no registered mobile (→ who-appointed-me),
 * cooldown/rate-limit, and wrong/expired/locked OTP — meaningfully.
 */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private readonly api = inject(PasswordResetRestControllerService);

  username = '';
  otpCode = '';
  newPassword = '';
  confirmPassword = '';

  readonly stage = signal<Stage>('username');
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  private resetId = '';
  private resetToken = '';

  /** Step 1: ask the backend to send an OTP to the username's registered mobile. */
  sendOtp(): void {
    const username = this.username.trim();
    if (!username || this.busy()) {
      return;
    }
    this.begin();
    this.api.request({ username }).subscribe({
      next: ({ resetId }) => {
        this.resetId = resetId;
        this.stage.set('otp');
        this.done();
      },
      error: (err: HttpErrorResponse) => this.fail(this.requestMessage(err)),
    });
  }

  /** Step 2: exchange the entered OTP for a short-lived reset token. */
  verify(): void {
    const code = this.otpCode.trim();
    if (!code || this.busy()) {
      return;
    }
    this.begin();
    this.api.verify({ resetId: this.resetId, otpCode: code }).subscribe({
      next: ({ resetToken }) => {
        this.resetToken = resetToken;
        this.stage.set('password');
        this.done();
      },
      error: (err: HttpErrorResponse) => this.fail(this.otpMessage(err)),
    });
  }

  /** Step 3: set the new password against the reset token. */
  complete(): void {
    if (this.busy()) {
      return;
    }
    if (!this.newPassword || this.newPassword !== this.confirmPassword) {
      this.error.set('The two passwords must match.');
      return;
    }
    this.begin();
    this.api.complete({ resetToken: this.resetToken, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.stage.set('done');
        this.done();
      },
      error: (err: HttpErrorResponse) => this.fail(this.completeMessage(err)),
    });
  }

  /** Start over from the username step, clearing all entered values. */
  restart(): void {
    this.username = '';
    this.otpCode = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.resetId = '';
    this.resetToken = '';
    this.error.set(null);
    this.stage.set('username');
  }

  private begin(): void {
    this.busy.set(true);
    this.error.set(null);
  }

  private done(): void {
    this.busy.set(false);
  }

  private fail(message: string): void {
    this.busy.set(false);
    this.error.set(message);
  }

  private requestMessage(err: HttpErrorResponse): string {
    if (err.status === 404) {
      return "We couldn't find that username — check the spelling and try again.";
    }
    if (err.status === 422) {
      return 'There is no mobile registered for that user. Use "Who appointed me?" to find who can reset your password.';
    }
    if (err.status === 429) {
      return this.backendMessage(err) ?? 'Too many OTP requests — please wait a moment and try again.';
    }
    return 'Something went wrong — please try again.';
  }

  private otpMessage(err: HttpErrorResponse): string {
    if (err.status === 422) {
      return this.backendMessage(err) ?? 'That OTP was wrong, expired, or used up. Try again.';
    }
    return 'Something went wrong — please try again.';
  }

  private completeMessage(err: HttpErrorResponse): string {
    if (err.status === 404 || err.status === 422) {
      return 'That reset has expired — start again from your username.';
    }
    return 'Something went wrong — please try again.';
  }

  private backendMessage(err: HttpErrorResponse): string | null {
    // RFC 9457 Problem Details (#70): the human-readable text is `detail`.
    const detail = err.error?.detail;
    return typeof detail === 'string' && detail.length > 0 ? detail : null;
  }
}
