import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Stand-in screen for a shell section until its real screens land in later
 * slices (Slice 9 ships only the navigable shell). The heading comes from the
 * route's `data.label`.
 */
@Component({
  selector: 'app-section-placeholder',
  standalone: true,
  template: `
    <section class="placeholder">
      <h1>{{ label }}</h1>
      <p>This section arrives in a later slice.</p>
    </section>
  `,
  styles: [
    `
      .placeholder h1 {
        margin-top: 0;
      }
      .placeholder p {
        color: #6b7280;
      }
    `,
  ],
})
export class SectionPlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly label = (this.route.snapshot.data['label'] as string) ?? '';
}
