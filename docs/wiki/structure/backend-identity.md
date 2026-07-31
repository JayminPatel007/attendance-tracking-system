---
kind: structure
slug: backend-identity
source_paths: [apps/backend/identity-service/**]
decisions: [ADR-0011, ADR-0016, ADR-0017, ADR-0018, ADR-0019, ADR-0020, ADR-0025, ADR-0027, ADR-0029]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Identity Context

## Purpose

<!-- [coverage: high] -->

Who a person is and what authority they hold: the Person Directory, User accounts and credentials,
Role Assignments and the rules for appointing and revoking them, plus the OTP-guarded flows
(password reset, Verified Home Sabha Transfer) and BSS/YSS Selection. It is the authority oracle
for the whole backend — the other three contexts ask it who the caller is and what roles they hold.

## Layout

<!-- [coverage: high] -->

Five Maven modules per ADR-0019, under the `identity-service` / `identity-domain` aggregators:

| Module | Ring | What lives in it |
|---|---|---|
| `identity-domain-core` | Entities | `User`, credentials, `RoleAssignment`, domain services, domain-rule exceptions. Pure Java. |
| `identity-application-service` | Use cases | Application services + driven-port interfaces. The largest module here. |
| `identity-data-access` | Interface adapters | 24 `Jdbc*` adapters (repositories, lookups). |
| `identity-messaging` | Interface adapters | OTP gateway + code generator, `HmacOtpHasher`, the Keycloak Admin REST client. |
| `identity-application` | Interface adapters | REST/BFF controllers and their DTOs (ADR-0017). |

The ring is identical in every context per ADR-0019. What is specific
here is the **feature-package axis inside `identity-application-service`**, which is the real
navigation surface: `directory`, `appointment`, `selection`, `passwordreset`, `otp`, `transfer`,
`sabhadefinition`, `bootstrap`, `session`. Genuinely cross-cutting driven ports
(`IdentityProviderGateway`, `UserRepository`) deliberately stay in the root package rather than
being forced into one feature.

## Exposes

<!-- [coverage: high] -->

**`/api/*` — mobile (ADR-0003):** `/api/whoami`, `/api/directory/*`, `/api/home-sabha-transfers/*`,
`/api/password-reset/*`, `/api/sanchalak/nominations`, `/api/who-appointed-me`.

**`/bff/*` — web, cookie-session (ADR-0022):** `/bff/me`, `/bff/directory/*`, `/bff/appointments/*`,
`/bff/password-reissue`, `/bff/selection/*`, `/bff/sabhas` (POST — Sabha *definition*, see Gotchas).

Individual endpoints and their contracts belong to a feature dossier, not here.

## Talks To

<!-- [coverage: medium -- edges derived from `import org.sabha.common.*` and `implements` scans, not from call-graph analysis; a port imported but only referenced in a test would look like a live edge. ] -->

**Outbound** — only two, both into [[backend-sabha]] via common-domain ports:
`SabhaProvisioning` (create a Sabha when one is defined) and `StructuralHierarchyLookup`
(City → Zone → Kshetra resolution for scoping an appointment).

**Inbound** — nine common-domain ports this context *implements* for everyone else, all as `Jdbc*`
adapters in `identity-data-access`: `CallerResolver`, `RoleAssignmentLookup`, `SantLookup`,
`MadhyasthaKaryalayaLookup`, `NirdeshakScopeLookup`, `NirikshakAssignmentLookup`,
`RegionalTeamCityLookup`, `SanyojakZoneLookup`, `UserActivityRecorder`.

That 2-out / 9-in asymmetry is the load-bearing fact about this context: it is mostly a *provider*.
Per ADR-0027 there is no shared granted-scope module behind those ports — each context runs its own
authorization engine over them.

`identity-messaging` also talks outward to **Keycloak's Admin REST API** to provision Users
(ADR-0016).

## Data

<!-- [coverage: low -- ownership inferred from which module issues INSERT/UPDATE against each table; there is no schema-ownership manifest, and the changelog is partitioned by slice/issue rather than by context. Verify before acting. ] -->

Written here: `users`, `persons`, `role_assignments`, `home_sabhas`, `home_sabha_transfers`,
`password_resets`, `selection_nominations`, `user_activity`.

Read-only here: `sabhas`, `nirikshak_sabha_assignments`.

`nirikshak_sabha_assignments` has **no writer in any context's main sources** — this context and
[[backend-attendance]] both only read it. Confirm against the changelog before assuming it is
populated at runtime rather than seeded.

## Gotchas

<!-- [coverage: medium -- the route collision is directly observable in the two controllers; whether it is deliberate is an inference from the ADR-0017 adapter-placement rule. ] -->

`POST /bff/sabhas` is served from **this** context (`SabhaDefinitionController`), while
`GET /bff/sabhas/{id}` and `/bff/sabhas/mine` are served from [[backend-sabha]]. The prefix is
shared across two contexts because *defining* a Sabha seeds its founding role appointments, which
is identity's business. Grepping one module for the route will find half the surface.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; this is structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [identity-service](../../../apps/backend/identity-service) — module layout, `package-info.java` in every ring
- [ADR-0011](../../adr/0011-role-appointment-authority.md), [ADR-0017](../../adr/0017-rest-adapters-live-in-application-modules.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0027](../../adr/0027-no-shared-granted-scope-module-behind-the-authorization-engines.md)
- [CONTEXT.md](../../../CONTEXT.md) — Karyakar, Nirdeshak, Sanchalak, Selection vocabulary
