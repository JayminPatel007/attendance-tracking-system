import { Component, computed, inject, signal } from '@angular/core';
import { SessionService } from 'identity-domain';

import {
  ACTOR_TIERS,
  ActorTier,
  AuthorityItem,
  DELETE_LEGEND,
  DeleteKind,
  authorityFor,
} from './authority-matrix';

/** One rendered column of the matrix. */
interface AuthorityColumn {
  key: 'structures' | 'roles';
  title: string;
  items: AuthorityItem[];
  emptyText: string;
}

/**
 * The authority matrix (issue #90): a read-only answer to "what can I create or
 * delete here?", one tier at a time, with the delete rule spelled out against
 * every item. Purely presentational — the rules come from {@link authorityFor},
 * a client-side mirror of the engines the backend already enforces, so the screen
 * needs no endpoint of its own.
 *
 * The switcher spans all five tiers rather than only the viewer's, because the
 * question a role-holder actually has is comparative: what does the tier above me
 * hold that I don't? It opens on the viewer's own tier where the session names it
 * (Madhyastha Karyalaya, Regional Team) and on the Nirdeshak otherwise — the
 * session carries no finer role breakdown.
 */
@Component({
  selector: 'app-my-authority',
  standalone: true,
  templateUrl: './my-authority.component.html',
  styleUrl: './my-authority.component.scss',
})
export class MyAuthorityComponent {
  private readonly sessions = inject(SessionService);

  readonly tiers = ACTOR_TIERS;
  readonly legend = DELETE_LEGEND;
  readonly labelOf = (tier: ActorTier): string => authorityFor(tier).label;

  private readonly ownTier = computed<ActorTier>(() => {
    const session = this.sessions.session();
    if (session?.madhyasthaKaryalaya) {
      return 'mk';
    }
    if (session?.regionalTeam) {
      return 'regional-team';
    }
    return 'nirdeshak';
  });

  readonly selected = signal<ActorTier | null>(null);
  readonly actor = computed(() => authorityFor(this.selected() ?? this.ownTier()));

  /** Both columns share a shape, so the template renders them from one loop. */
  readonly columns = computed<AuthorityColumn[]>(() => [
    {
      key: 'structures',
      title: 'Structures',
      items: this.actor().structures,
      emptyText: 'No structural-creation authority at this tier.',
    },
    {
      key: 'roles',
      title: 'Roles',
      items: this.actor().roles,
      emptyText: 'No appointment authority at this tier.',
    },
  ]);

  select(tier: ActorTier): void {
    this.selected.set(tier);
  }

  isSelected(tier: ActorTier): boolean {
    return this.actor().tier === tier;
  }

  /** The legend label for a delete kind — the badge and the legend must read alike. */
  deleteLabel(kind: DeleteKind): string {
    return this.legend.find((entry) => entry.kind === kind)!.label;
  }
}
