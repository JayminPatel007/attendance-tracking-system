import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessionService } from 'identity-domain';

import { SECTION_NAV } from './section-nav';

/**
 * The authenticated web shell (Slice 9): a header with the account menu, a
 * sidebar whose entries are the sections the signed-in user may see, and a
 * router outlet for the section screens. The visible sections come from the BFF
 * session, so authority decides the navigation, not the client.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly sessions = inject(SessionService);

  readonly session = this.sessions.session;

  /** Sidebar entries restricted to the sections the session unlocks. */
  readonly navItems = computed(() => {
    const current = this.session();
    if (!current) {
      return [];
    }
    return SECTION_NAV.filter((item) => current.sections.includes(item.section));
  });

  readonly menuOpen = signal(false);

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  logout(): void {
    this.sessions.logout().subscribe();
  }

  changePassword(): void {
    this.sessions.changePassword();
  }
}
