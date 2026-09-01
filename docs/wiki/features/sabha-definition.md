---
type: feature
title: Sabha Definition
description: Defining a Sabha and its Sanchalak in one act, and registering or retiring the Sabha Kind it is an instance of.
aliases: [define a Sabha, Sabha Kind, Sabha Type, schedule shape, weekly recurring, monthly ad-hoc, soft-retire, retired kind, standing venue]
tags: [bff]
source_paths: [
  apps/backend/sabha-service/*/src/main/**,
  apps/backend/identity-service/*/src/main/**,
  apps/web/src/app/sections/sabha-definition/**,
  apps/web/src/app/sections/structural-admin/**,
  apps/web/projects/sabha-domain/src/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-12/**,
  apps/backend/application-container/src/main/resources/db/changelog/issue-87/**,
  docs/adr/0009-*.md,
  docs/adr/0012-*.md,
  docs/adr/0026-*.md,
  CONTEXT.md
]
issues: [13, 87, 88]
sources:
  - { id: adr-0009, title: "Structural Creation Authority Lives at the Tier Above", resource: ../../adr/0009-structural-creation-authority.md }
  - { id: adr-0012, title: "Sabha Schedule Shapes and Occurrence Materialization", resource: ../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md }
  - { id: adr-0026, title: "Deletion model: block-if-non-empty for geography, soft-retire for Sabha Kind, revoke-with-inheritance for roles", resource: ../../adr/0026-deletion-model.md }
  - { id: context, title: "CONTEXT.md — Sabha, Sabha Type (aka Sabha Kind), Schedule shapes, Nirdeshak, Sanchalak", resource: ../../../CONTEXT.md }
last_compiled: 85eaa7a00240b54e15e35da00229a19ee8c71ce7
---

# Sabha Definition

## What it does

<!-- [coverage: high -- ADR-0012 and ADR-0026 read against SabhaDefinitionService, Sabha, SabhaKind and SabhaKindLifecycleService] -->

Two acts on the same type system. The **Madhyastha Karyalaya** registers a **Sabha Kind** — a
`(demographic, track)` pair such as *Regular Yuvak* or *BSS Baal* — which is extensible data rather
than a hardcoded enum (ADR-0009). A **Nirdeshak** then **defines a Sabha** of that Kind in their
Kshetra, choosing one of two schedule shapes and naming the Sanchalak who will run it.

Defining a Sabha is deliberately **one transaction**: the Sabha and its Sanchalak appointment (and an
optional Sah-Sanchalak) land together, so no Sabha is ever left standing without someone to run it.

A Kind is never deleted. It is **soft-retired** — marked inactive so nothing new of that kind can be
created while the Sabhas already running drain naturally (ADR-0026). Retirement is reversible.

The geographic chain it hangs off is [structural-hierarchy](structural-hierarchy.md); what happens to it afterwards
is [occurrence-lifecycle](occurrence-lifecycle.md).

## Flow

<!-- [coverage: medium -- both backend paths read end to end; the web half read from the two components, not their templates] -->

**Web** — the only surface, in two places. The `sabha-definition` section holds the definition form
and the Sabha management list; the Kind registry lives on the `sabha-kinds` tab of `structural-admin`
alongside Cities, because both are MK state-scope acts.

1. `GET /bff/structure/kshetras`, `/sabha-kinds` and `/bff/sabhas/mine` fill the form and the list.
2. The form is shape-discriminated: a **weekly** Sabha asks for day-of-week plus start and end time;
   a **monthly ad-hoc** one asks for neither. Both take a standing venue and the appointee, chosen
   through the shared person picker.
3. `POST /bff/sabhas` defines. `POST /bff/structure/sabha-kinds/{id}/retire` and `/reactivate` drive
   the Kind lifecycle; `DELETE /bff/sabhas/{id}` removes an unused Sabha.

**Backend** — the definition endpoint is **identity's**, the aggregate is **sabha's**:

- `SabhaDefinitionService` resolves the caller, reads the Kind's demographic across the seam,
  authorizes the Nirdeshak, refuses a retired Kind, provisions the Sabha through `SabhaProvisioning`,
  then reuses [role-appointment](role-appointment.md)'s flow for the Sanchalak and Sah-Sanchalak.
- A duplicate-name **soft-warn** on an inline-created Person marks the transaction rollback-only and
  returns the candidates, so nothing is left half-built.
- `Sabha.weekly` / `Sabha.monthlyAdHoc` enforce the shape invariants; `SabhaKind.register` enforces
  *Sanyukta is Regular-track only*; `SabhaKindLifecycleService` owns retire and reactivate.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- SabhaDefinitionAuthorization, SabhaKindLifecycleService and the four SabhaKindRetiredException throw sites all read directly] -->

- **Who.** Define a Sabha: the Nirdeshak over that `(Kshetra, demographic)` — the same authority that
  appoints its Sanchalak. Register, retire or reactivate a Kind: the Madhyastha Karyalaya.
  Delete a Sabha: the same Nirdeshak, via sabha's own copy of the predicate —
  [authorization](../patterns/authorization.md).
- **Two shapes, no third.** `WEEKLY_RECURRING` carries the standing slot; `MONTHLY_AD_HOC` carries
  none, because a BSS/YSS Sabha genuinely has no fixed date. A unified recurrence rule was rejected
  (ADR-0012).
- **A retired Kind blocks four paths**, all in identity and all → **409**: defining a Sabha,
  appointing into a Sabha-scoped role, setting an initial Home Sabha on a new Person, and
  transferring a Home Sabha in. Existing Sabhas keep running; only new usage is refused.
- **Rejections.** Denial → **403**. An unknown Kind → **404**. A weekly Sabha with a missing or
  inverted slot, or a blank venue → domain refusal. Retiring an already-retired Kind, or reactivating
  an active one, is refused by the aggregate.
- **Deleting a Sabha is block-if-non-empty**: allowed only while it has recorded no Occurrences, with
  the count-derived reason shown before the click.

**Divergence worth knowing:** `VisibleSections` grants the `SABHA_DEFINITION` section to the
**Madhyastha Karyalaya only**, while the endpoint requires a Nirdeshak — so the MK sees a form it
is refused on, and the Nirdeshak who may define has no nav entry. Same shape as
[role-appointment](role-appointment.md)'s.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `sabhadefinition` package: `SabhaDefinitionService`,
  `SabhaDefinitionAuthorization`, `SabhaDefinitionController`, and the appointment flow it reuses.
- [backend-sabha](../structure/backend-sabha.md) — the `Sabha` and `SabhaKind` aggregates, `SabhaKindLifecycleService`,
  `JdbcSabhaProvisioning`, `SabhaListController` and the Kind routes on `StructuralCreationController`.
- [backend-common-domain](../structure/backend-common-domain.md) — `SabhaProvisioning`, `SabhaKindRetiredException`, `CallerResolver`,
  `AuthorizedAction`.
- [backend-container](../structure/backend-container.md) — `slice-12`'s `sabhas.sabha_kind_id` / `created_by` and `issue-87`'s
  `retired_at` / `retired_by`.
- [web](../structure/web.md) — the `sabha-definition` section, the Kind tab of `structural-admin`, and the
  `sabha-domain` library's kind labels and `isAllowedKind`.

## Amendments

<!-- [coverage: medium -- reconstructed from changelog headers, class javadocs and ADR consequences; the issue-to-change mapping is inferred] -->

- **Slice 12** (issue #13) — the capability: the `Sabha` aggregate with its shape discriminator, the
  `SabhaProvisioning` seam, the typed `sabha_kind_id`, and the one-transaction definition.
- **Issue #87** — the soft-retire marker and the four paths it blocks, via two cross-context lookups
  that default to *not retired*.
- **Issue #88** — Sabha deletion, whose UI sits on this section's own list, not `structural-admin`.

Worth knowing: `sabhas` still carries the **denormalized** `sabha_kind` token beside the typed FK,
because the cross-context hierarchy lookup reads it. Both are written on create.

## Method

- `SabhaDefinitionService`'s javadoc and body are the source that paid: the four numbered steps, the
  cross-seam provisioning call and the rollback-only soft-warn are the whole capability, and no ADR
  describes the transaction at all.
- The four retired-Kind refusals are only visible by grepping `SabhaKindRetiredException`'s throw
  sites — they live in four different identity packages and none of them mentions the others.
