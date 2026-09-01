---
type: feature
title: Home Sabha Transfer
description: Moving a Person's Home Sabha into another Sabha, confirmed by an OTP to the Person themselves.
aliases: [Verified Home Sabha Transfer, HSAT, transfer, Roster move, Sanchalak pull, OTP consent]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/mobile/sabha_attendance/lib/home_sabha_transfer/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-2/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-8/**,
  apps/backend/application-container/src/main/resources/db/changelog/issue-77/**,
  apps/backend/application-container/src/main/resources/db/changelog/issue-87/**,
  docs/adr/0002-*.md,
  docs/adr/0007-*.md,
  docs/adr/0013-*.md,
  docs/adr/0026-*.md,
  CONTEXT.md
]
issues: [9, 77, 87, 130]
sources:
  - { id: adr-0002, title: "Sanchalak-Initiated Home Sabha Transfer Requires Person Verification", resource: ../../adr/0002-home-sabha-transfer-requires-person-verification.md }
  - { id: adr-0007, title: "Mobile App is Offline-Capable for Attendance Marking Only", resource: ../../adr/0007-offline-capable-attendance-marking.md }
  - { id: adr-0013, title: "Directory De-duplication on Person Add", resource: ../../adr/0013-directory-de-duplication-on-person-add.md }
  - { id: adr-0026, title: "Deletion Model", resource: ../../adr/0026-deletion-model.md }
  - { id: context, title: "CONTEXT.md — Home Sabha, Sabha Kind, Roster, Sanchalak", resource: ../../../CONTEXT.md }
last_compiled: 6e43fd984ca097e05d67237d341afc11c0bf41ea
---

# Home Sabha Transfer

## What it does

<!-- [coverage: high -- ADR-0002 read against HomeSabhaTransferService, HomeSabhaSwap and the slice-8 migration] -->

A **Sanchalak** or **Sah-Sanchalak** pulls a Person into their own Sabha as that Person's new **Home
Sabha** — but the move only lands after the **Person themselves** confirms it, with an OTP to their
registered mobile (ADR-0002).

The verification is the whole point. Without it a Sanchalak could quietly inflate their Roster by
capturing a one-off Walk-in, and every analytics number derived from the Roster would inherit the
distortion. The OTP makes the Person an explicit party to the decision.

A Person holds one Home Sabha **per Sabha Kind** they qualify for, so a transfer replaces only the
one whose kind matches the destination's. The other kinds are untouched.

## Flow

<!-- [coverage: high -- HomeSabhaTransferService, HomeSabhaSwap and OtpGuardedFlow read end to end; the mobile path read from its api and controller] -->

**Mobile** — the only surface, four steps in `HomeSabhaTransferController`:

1. **Find** — `GET /api/directory/persons?mobile=…`. The mobile is the Directory's system-wide key
   (ADR-0013), so this reaches a Person transferring in from another Kshetra just as well.
2. **Confirm the direction** — this Person, into your Sabha — before any code is sent.
3. **OTP** — `POST /api/home-sabha-transfers` sends the code to *the Person's* mobile and returns a
   transfer id. `403` means the caller is not this Sabha's Sanchalak; `429` is the rate limit or the
   resend cooldown.
4. **Commit** — `POST /api/home-sabha-transfers/{id}/confirm` carries the code the Person read out.

**Backend** — `HomeSabhaTransferService` keeps only what is its own: who may pull a Person in, and
the Roster swap consent unlocks. `initiate` checks the caller's roles on the destination Sabha, the
retired-kind guard, and that the Person has a mobile at all, then hands off to `OtpGuardedFlow.begin`.
`confirm` runs inside `OtpGuardedFlow.consume`: the code check, then `HomeSabhaSwap.selectPrevious`
picks the Home Sabha of the destination's kind, `JdbcPersonDirectory.replaceHomeSabha` deletes and
inserts, and the transfer records the swap.

**Web** — `_none_`. There is no web transfer surface; ADR-0002's untested top-down path for
higher-tier Karyakars has no implementation on either client.

## Rules & authority

<!-- [coverage: high -- the service, OtpChallenge and slice-8's schema read directly against ADR-0002] -->

- **Who initiates.** Only a `SANCHALAK` or `SAH_SANCHALAK` **of the destination Sabha**, checked
  through `RoleAssignmentLookup.rolesForUserOnSabha`. A refusal is `TransferNotAuthorizedException`,
  a subclass of the shared denial in [authorization](../patterns/authorization.md) → **403**. This is
  a role predicate on one Sabha, not a geographic walk — there is no Authorization Engine here.
- **Who consents.** The Person, and only the Person. The OTP goes to `persons.mobile`; a
  guardian-linked Person with no mobile of their own is `PersonHasNoMobileException` → **422**, which
  is ADR-0002's known gap, not a bug.
- **The OTP guard is load-bearing.** `confirm` is deliberately **not** `@Transactional` —
  `OtpGuardedFlow.consume` owns the boundary, and its narrow `noRollbackFor` is what lets a wrong code
  keep its incremented attempt count while a failure in the *swap* still rolls the whole thing back.
  Adding an annotation here would substitute this method's rollback rules for those. Shared budget:
  5-minute TTL, 5 attempts, 3 sends per mobile per hour, 30-second cooldown — see
  [authentication](authentication.md).
- **Which Home Sabha moves.** The one whose `sabha_kind` matches the destination's. No match at all
  is `NoMatchingHomeSabhaException` → **422**, raised *after* the OTP is consumed and rolled back
  with it.
- **Retired Sabha Kind.** A destination whose kind is soft-retired refuses the transfer (**409**),
  per ADR-0026's drain-don't-delete rule.
- **Always online.** ADR-0007 permits offline for marking only; nothing here is ever queued.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `transfer` and `otp` feature packages, `HomeSabhaTransfer` and
  `HomeSabhaSwap` in domain-core, `HomeSabhaTransferRestController`, and the
  `JdbcHomeSabhaTransferRepository` / `JdbcPersonDirectory` adapters.
- [backend-common-domain](../structure/backend-common-domain.md) — `CallerResolver`, `Role`, `RoleAssignmentLookup`, `StructuralHierarchyLookup`,
  `SabhaKindRetiredException`.
- [backend-sabha](../structure/backend-sabha.md) — implements the hierarchy lookup the retired-kind guard reads.
- [backend-container](../structure/backend-container.md) — `slice-2` (`home_sabhas`), `slice-8` (`home_sabha_transfers` and its
  per-mobile index), `issue-77` (the scrub), and the ProblemDetail mapping.
- [mobile-shell](../structure/mobile-shell.md) — `lib/home_sabha_transfer/`, a hand-rolled `http` client.
- [person-directory](person-directory.md) — the mobile lookup step 1 runs through, and the adapter both share.

## Amendments

<!-- [coverage: medium -- reconstructed from changelog headers and javadocs; the issue-to-change mapping is inferred, not read from the PRs] -->

- **Slice 8** (issue #9) — the capability: `home_sabha_transfers` holding OTP consent between
  initiate and confirm, and the swap on confirm.
- **Issue #130** — the OTP state machine and orchestration were **extracted out** of this aggregate.
  `OtpChallenge` came out of `HomeSabhaTransfer`'s inline logic first, then `OtpGuardedFlow` from the
  two services — so this feature is where the shared machinery originated, and password reset was its
  second caller.
- **Issue #77** — codes hashed at rest; `slice-8`'s stated *"stored as-is for v1"* is closed, with
  in-flight `PENDING` transfers expired at deploy rather than migrated.
- **Issue #87** — a retired Sabha Kind became a blocking path here, one of four.

## Method

- `HomeSabhaTransferService.confirm`'s javadoc is the source that paid: it explains the *absent*
  `@Transactional`, which every other reading makes look like an oversight. Pair it with
  `OtpGuardedFlow.consume` — one states the rule, the other the reason — and note that a recompile
  that reads only the service will get this backwards.
- `HomeSabhaSwap` is fifteen lines and settles the whole per-kind question that ADR-0002 never
  raises; `slice-8`'s header supplies the schema half and admits the v1 plaintext that #77 later closed.
