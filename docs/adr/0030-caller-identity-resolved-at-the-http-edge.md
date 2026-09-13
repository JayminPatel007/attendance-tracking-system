# Caller identity is resolved at the HTTP edge; below it, signatures speak `UserId`

**Status**: accepted. **Amends [ADR-0019](0019-bounded-context-module-taxonomy.md)** by adding a sixth shared module, `common-application`, which ADR-0019's Q11.3 had deliberately dropped. Extends [ADR-0016](0016-oidc-auth-via-keycloak.md) and completes the `requireUserId` consolidation from issue #78.

Every authenticated endpoint opened by parsing the Keycloak subject out of the credential and threading it downward:

```java
UUID keycloakSubject = UUID.fromString(jwt.getSubject());   // mobile, Bearer JWT
UUID subject = UUID.fromString(authentication.getName());   // web, BFF session (ADR-0022)
```

**39 non-test call sites** did this across all four contexts. The subject was then passed through layers that never read it — `GetCurrentRosterUseCase`, `OccurrenceShapingService`, `TransitionActor.SignedIn`, `OccurrenceWriter` — until, several frames deep, someone called `CallerResolver.requireUserId(...)` and converted it into the thing the domain actually wanted: the local `users.id`.

That is a pass-through parameter on the hottest path in the system, and it leaks a transport decision ("a caller is a Keycloak subject, which is a UUID, parsed from the `sub` claim") into 24 files of domain-facing signatures. `CONTEXT.md` names the system's identity concept **User** — "a Person who can log into the system". "Keycloak subject" is not in the ubiquitous language, yet it was in the method signatures.

**Decision: resolve the caller once, in an argument resolver at the HTTP edge. Everything below takes a `UserId`.**

```java
@GetMapping("/api/sanchalak/current-roster")
ResponseEntity<CurrentRoster> currentRoster(@CurrentUser UserId caller) {
    return getCurrentRoster.execute(caller) ...
}
```

## What this is not

It is not a new pattern. Four controllers — `SabhaListController`, `StructuralCreationController`, `StructuralDeletionController`, `DashboardBffController` — **already** resolved at the edge by hand (`callers.requireUserId(UUID.fromString(authentication.getName()))`), so sabha-service and analytics-service were already clean below the edge. The leak was confined to attendance and identity. This ADR records making the existing pattern uniform and **unskippable**, not inventing one.

## The shape

| Type | Module | Why there |
| --- | --- | --- |
| `UserId` (record wrapping `UUID`) | `common-domain` | Must be visible to `*-domain-core` and `*-application-service`, which may not depend on anything Spring-web-flavoured. Pure Java, like the other VOs. |
| `@CurrentUser` | `common-application` **(new)** | Presentation-ring annotation, referenced by all four `*-application` modules. |
| `CurrentUserArgumentResolver` + its `WebMvcConfigurer` | `common-application` **(new)** | Needs `spring-web` and `spring-security`; self-registering. |

`common-application` depends on `common-domain`, `spring-web`, `spring-webmvc` and `spring-security-core`/`-oauth2-resource-server`. Each `*-application` module may depend on it. Nothing else may.

The resolver reads `Authentication`, not `Jwt`, so **one** resolver serves both edges: the mobile Bearer resource server and the web BFF's server-side OIDC session (ADR-0022).

An **ArchUnit rule** (`IntraModuleArchitectureRulesTest`, issue #69) forbids `Jwt`, `getSubject()` and `Authentication` outside `common-application` and `application-container`. Without it this regresses — the four already-correct controllers are the evidence that the right pattern does not survive on convention alone here.

## Considered and rejected

- **A `Caller` record carrying `userId` plus the subject and authorities.** A wrapper justified by fields it does not have. Authorities are not on the JWT in this system — they come from `role_assignments` via four separate engines, and [ADR-0027](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md) already refused to unify them. `Caller` would have re-imported the leak it was meant to remove, and smuggled an authorization decision into the edge.
- **Registering the resolver from `application-container`** (keeping ADR-0019's module count intact). A module whose entire purpose is one resolver, which silently does nothing unless the container remembers to wire it, is a trap. Self-registration is worth the sixth module.
- **One resolver per `*-application` module.** Four copies of the one decision this ADR exists to centralise.
- **Promoting `TransitionActor` to a shared sealed `Caller { User, System }`.** `TransitionActor` also carries `AuthorizedAction` — attendance's authority axis — and its `ActorKind` maps to an attendance audit column. Promoting it drags attendance's authorization vocabulary into `common-domain`: the same mistake ADR-0027 declined. `SignedIn` simply takes a `UserId` now.
- **Typing every `users.id`-shaped `UUID`** (lookup ports, audit read-model rows, `appointedBy`). That is where the type safety fully pays off, but it touches persistence mapping, every lookup port and the audit UNION ([ADR-0023](0023-audit-log-read-model-and-viewer-authority.md)). Filed as a follow-up rather than fused into this change.

## Consequences

- **`CallerResolver` narrows.** `resolveUserId` stays in `common-domain` for the two genuinely non-request-bound consumers — `LoginActivityListener` (event-driven) and the BFF. `requireUserId`, created by issue #78 as the single home for the `orElseThrow` every application service was repeating, moves **into the argument resolver**: once no application service calls it, its home *is* the edge. `CallerUnknownException` stays in `common-domain` (issue #78 moved it there to fix identity's 500) and still maps to 403.
- **Three endpoints change status code.** `GET /api/sanchalak/current-roster`, `/current-occurrence` and `/monthly-sabhas` today collapse "unknown caller" and "nothing to show" into one `Optional` → `404`. Unknown caller now fails at the edge with `403`. The mobile roster and monthly-sabhas clients throw on any non-200, so this is a message change; `occurrence_control_api.dart` returns `null` on `404`, and that legitimate empty state is preserved — only the unknown-caller branch moves. **Nothing in the type system or the OpenAPI drift gate catches this**: those paths document only `200`, so the generated clients are untyped on both codes.
- **`CallerUnknownException` is raised at the boundary** rather than five frames in, which is where a broken session should be detected.
