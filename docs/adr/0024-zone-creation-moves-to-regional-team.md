# Zone creation moves from Madhyastha Karyalaya to the Regional Team

**Status**: accepted. **Supersedes the Zone row of [ADR-0009](0009-structural-creation-authority.md)**; the rest of ADR-0009 (Kshetra ← Sanyojak, Sabha ← Nirdeshak, City and Sabha Kind ← MK) stands.

The Madhyastha Karyalaya is being narrowed to genuinely State-wide concerns — registering Cities and Sabha Kinds, and the administrative user records (first Regional Team member per City×demographic, Sant credentials). Creating Zones is a City-level operational decision, so it moves down to the **Regional Team**, which already sits at the (City, demographic) tier and is close enough to the ground to know when a City needs a new Zone.

## Why move it down (and remove the MK fallback entirely)

ADR-0009's principle — "the tier above creates the structure below" — is preserved, just applied one rung lower for Zones. Leaving MK a fallback "just in case" would reintroduce the very operational load we are removing and muddy the audit story (two possible creators for the same entity). MK therefore has **no** Zone-creation path at all.

## Consequences

- **Bootstrap order for a new City:** MK creates the City and the *first* Regional Team member for each relevant (City, demographic); that Regional Team member then creates the Zones. There is a deliberate window where a City has no Zones until its Regional Team exists.
- `zones.created_by` now points to a Regional Team member User, not an MK member.
- The Sant/Regional-Team coordination story is unaffected — Zone creation is a write the Sant never had.
