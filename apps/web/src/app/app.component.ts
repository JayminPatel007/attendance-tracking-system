import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SessionService } from 'identity-domain';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly sessions = inject(SessionService);

  /** True when Keycloak authenticated the user but no local account is linked. */
  readonly unlinked = computed(() => this.sessions.status() === 'unlinked');
}
