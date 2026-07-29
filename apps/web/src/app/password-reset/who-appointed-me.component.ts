import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AppointerContact, WhoAppointedMeRestControllerService } from 'shared-data-access';

/**
 * "Who appointed me?" lookup (ADR-0004, Slice 18B) — a **public** route reachable
 * from the login screen without authentication. A user who has lost their mobile
 * (and so cannot self-serve a reset) finds, keyed only on their username, the
 * contact details of whoever can reissue their password: their appointer, or a
 * Madhyastha Karyalaya member for Sants. Calls `/api/who-appointed-me` directly;
 * an unknown username is a 404.
 */
@Component({
  selector: 'app-who-appointed-me',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './who-appointed-me.component.html',
  styleUrl: './who-appointed-me.component.scss',
})
export class WhoAppointedMeComponent {
  private readonly api = inject(WhoAppointedMeRestControllerService);

  username = '';

  readonly contacts = signal<AppointerContact[] | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  lookup(): void {
    const username = this.username.trim();
    if (!username || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.contacts.set(null);
    this.api.whoAppointedMe(username).subscribe({
      next: ({ contacts }) => {
        this.contacts.set(contacts);
        this.busy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(
          err.status === 404
            ? "We couldn't find that username — check the spelling and try again."
            : 'Something went wrong — please try again.',
        );
      },
    });
  }
}
