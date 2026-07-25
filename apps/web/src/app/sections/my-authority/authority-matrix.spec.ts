import {
  ACTOR_TIERS,
  AuthorityItem,
  DELETE_LEGEND,
  DeleteKind,
  authorityFor,
} from './authority-matrix';

function itemsOf(tier: (typeof ACTOR_TIERS)[number]): AuthorityItem[] {
  const authority = authorityFor(tier);
  return [...authority.structures, ...authority.roles];
}

function everyItem(): AuthorityItem[] {
  return ACTOR_TIERS.flatMap(itemsOf);
}

function named(name: string): AuthorityItem | undefined {
  return everyItem().find((item) => item.name === name);
}

describe('authority matrix', () => {
  it('covers the five tiers top-down, one entry each', () => {
    expect(ACTOR_TIERS).toEqual([
      'mk',
      'regional-team',
      'sanyojak',
      'nirdeshak',
      'sah-nirdeshak',
    ]);
    ACTOR_TIERS.forEach((tier) => expect(authorityFor(tier).tier).toBe(tier));
  });

  it('gives every tier but Sah-Nirdeshak something to create', () => {
    ACTOR_TIERS.filter((tier) => tier !== 'sah-nirdeshak').forEach((tier) =>
      expect(itemsOf(tier).length).withContext(tier).toBeGreaterThan(0),
    );
  });

  it('explains every delete kind it uses in the legend', () => {
    const explained = DELETE_LEGEND.map((entry) => entry.kind);
    const used = new Set<DeleteKind>(everyItem().map((item) => item.delete));

    used.forEach((kind) => expect(explained).withContext(kind).toContain(kind));
    expect(DELETE_LEGEND.length).toBe(3);
  });

  it('names a concrete guard on every delete rule it shows', () => {
    everyItem().forEach((item) => {
      expect(item.deleteNote).withContext(item.name).toBeTruthy();
      expect(item.scope).withContext(item.name).toBeTruthy();
    });
  });

  it('deletes geographic structures by block-if-non-empty (ADR-0026)', () => {
    ACTOR_TIERS.forEach((tier) =>
      authorityFor(tier).structures.forEach((item) => {
        if (item.name !== 'Sabha Kind') {
          expect(item.delete).withContext(item.name).toBe('block-if-non-empty');
        }
      }),
    );
  });

  it('soft-retires the Sabha Kind, and only the Sabha Kind', () => {
    const retired = everyItem().filter((item) => item.delete === 'soft-retire');

    expect(retired.map((item) => item.name)).toEqual(['Sabha Kind']);
    expect(authorityFor('mk').structures).toContain(retired[0]);
  });

  it('revokes role assignments rather than deleting them', () => {
    ACTOR_TIERS.forEach((tier) =>
      authorityFor(tier).roles.forEach((item) =>
        expect(item.delete).withContext(item.name).toBe('revoke'),
      ),
    );
  });

  it('puts Zone creation on the Regional Team, not the Madhyastha Karyalaya (ADR-0024)', () => {
    expect(authorityFor('regional-team').structures.map((item) => item.name)).toContain('Zone');
    expect(authorityFor('mk').structures.map((item) => item.name)).not.toContain('Zone');
  });

  it('follows the geographic chain: City → Zone → Kshetra → Sabha', () => {
    expect(authorityFor('mk').structures.map((item) => item.name)).toContain('City');
    expect(authorityFor('sanyojak').structures.map((item) => item.name)).toEqual(['Kshetra']);
    expect(authorityFor('nirdeshak').structures.map((item) => item.name)).toEqual(['Sabha']);
  });

  it('flags the last-one-out guard wherever a Regional Team member can be revoked', () => {
    const regionalTeamRows = everyItem().filter((item) => item.name.startsWith('Regional Team'));

    expect(regionalTeamRows.length).toBeGreaterThan(0);
    regionalTeamRows.forEach((item) =>
      expect(item.deleteNote).withContext(item.name).toContain('last'),
    );
  });

  it('caps Sah-Nirdeshak appointments at two per (Kshetra, demographic)', () => {
    expect(named('Sah-Nirdeshak')?.scope).toContain('2');
  });

  it('gives Sah-Nirdeshak operational powers and no create or delete authority', () => {
    const sahNirdeshak = authorityFor('sah-nirdeshak');

    expect(sahNirdeshak.structures).toEqual([]);
    expect(sahNirdeshak.roles).toEqual([]);
    expect(sahNirdeshak.operational.length).toBeGreaterThan(0);
    expect(sahNirdeshak.noAuthorityNote).toBeTruthy();
  });

  it('leaves the acting tiers without an operational-only note', () => {
    ACTOR_TIERS.filter((tier) => tier !== 'sah-nirdeshak').forEach((tier) => {
      expect(authorityFor(tier).operational).withContext(tier).toEqual([]);
      expect(authorityFor(tier).noAuthorityNote).withContext(tier).toBeNull();
    });
  });
});
