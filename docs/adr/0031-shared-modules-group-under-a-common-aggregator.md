# Shared backend modules group under a `common` aggregator

**Status**: accepted (issue #212). **Amends [ADR-0019](0019-bounded-context-module-taxonomy.md)** — the layout diagram there shows `common-domain` flat at `apps/backend/`, and [ADR-0030](0030-caller-identity-resolved-at-the-http-edge.md) added `common-application` beside it. Both now live under `apps/backend/common/`. Nothing else in ADR-0019 changes: the ring taxonomy, the dependency rules, and every artifactId are untouched.

ADR-0019 gave each bounded context two aggregator poms (`<ctx>-service`, `<ctx>-domain`) and hung its five leaf modules off them. The two shared modules never got the same treatment — they sat flat in the reactor, so the root `<modules>` list read as two loose leaves followed by four nested aggregators. This groups them:

```
apps/backend/
├── common/                  <- new: packaging=pom, artifactId `common`
│   ├── common-domain/
│   └── common-application/
├── identity-service/  sabha-service/  attendance-service/  analytics-service/
├── application-container/
└── coverage-aggregate/
```

**This is a no-op on the build output.** Same 23 jars, same coordinates, same classpath, same `org.sabha.common` / `org.sabha.common.web` packages. No Java file moved package; no import changed; the ArchUnit rules in `IntraModuleArchitectureRulesTest` name *packages* and were not touched. The 15 consumer poms reference these modules by artifactId, not path, so none of them changed either.

Two things motivated it. The reactor list now reads as *four contexts + one common group + the container*. And `mvn -f common/pom.xml verify` builds the shared kernel alone, the way `mvn -f identity-service/pom.xml verify` already built a context alone — a seam that did not exist before, because there was no pom that owned both shared modules.

## Why `common` and not `common-service`

The `-service` suffix everywhere else names a **bounded context**. `common` is not one. ADR-0019 is explicit that it "depends on nothing in the project" and sits at the entities ring for every other context — giving it a context-shaped name would assert a peer relationship with identity/sabha/attendance/analytics that the dependency rules specifically deny. Reviving `shared-kernel`, the name ADR-0015 used and ADR-0019 deliberately renamed away, was also rejected: that decision is settled and re-opening it buys nothing.

## The naming collision, accepted rather than fixed

`common/common-domain` is a **leaf** module. Everywhere else in this repo, `<ctx>-domain` is an *aggregator* over a `<ctx>-domain-core` / `<ctx>-application-service` pair. A reader who learned the convention from the four contexts will reasonably expect `common/common-domain/common-domain-core/` and not find it.

We considered renaming the leaf to `common-domain-core` to remove the ambiguity. Rejected: it would ripple through the root `dependencyManagement`, 15 consumer poms, two ADRs and six wiki pages, to rename a module whose name ADR-0019 chose on purpose — and `common-domain` has no application-service sibling to disambiguate it *from*, which is the only reason the aggregator layer exists in a context. The collision is documented here, and `common/pom.xml`'s `<description>` points at this ADR rather than restating it. That is the whole mitigation.

**The leaf directory names keep their `common-` prefix** (`common/common-domain`, not `common/domain`) so that the repo-wide `dirname == artifactId` invariant holds for all 22 leaf modules. That invariant is what lets a reader — or an agent — map a stack frame or a pom coordinate back to a directory without a lookup table. `identity-service/identity-domain/identity-domain-core/` already pays the same repetition for the same reason.

## Consequences

- Aggregators in this repo are **`<modules>` groupings, never parents**. Every module, leaf and aggregator alike, declares `backend-parent` as its `<parent>` and repeats the `spring-boot-maven-plugin` `<skip>` block. `common/pom.xml` follows that convention rather than hoisting the shared plugin config, which would have made it the only aggregator that is also a parent.
- **`<relativePath>` is declared only where Maven's default is wrong.** The six top-level modules omit it, because the default `../pom.xml` already resolves to `backend-parent`; every nested module states it explicitly. `common/pom.xml` is top-level and so omits it.
- The two leaf poms previously relied on that default, correctly, while they were top-level. At their new depth it resolves to `common/pom.xml` — the wrong parent. Both now declare `<relativePath>../../pom.xml</relativePath>`, as every other nested module already did. This is the one edit in the move that is not cosmetic, and the build fails loudly without it.
- ADR-0019's tree diagram is now stale on these two paths. Per this repo's convention its body is left as written; this ADR is the forward pointer, linked from its Status line.
