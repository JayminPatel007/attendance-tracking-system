---
kind: structure
slug: mobile-app
source_paths: [apps/mobile/sabha_attendance/**]
decisions: [ADR-0003, ADR-0007, ADR-0014, ADR-0015, ADR-0016]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — App Shell (`sabha_attendance`)

## Purpose

<!-- [coverage: high] -->

The Flutter app for the Sanchalak and Sah-Sanchalak (ADR-0003 splits the platforms by role: the
field roles get mobile, the oversight roles get web). It is where **all** the mobile behaviour
actually lives — the five `packages/*_domain` mirrors are still scaffolds.

## Layout

<!-- [coverage: high] -->

`lib/` is split by **feature directory**, not by layer, with a consistent `*_api` / `*_controller` /
`*_screen` triple inside each:

| Directory | What it covers |
|---|---|
| `auth/` | `auth_config`, `auth_service`, `login_screen`, `session` — AppAuth/OIDC (ADR-0016). |
| `roster/` | The current-roster marking screen. |
| `walk_in/` | Walk-in attendee capture. |
| `add_person/` | Adding a Person to the Directory. |
| `monthly_occurrence/` | Creating the monthly ad-hoc Occurrence. |
| `occurrence_control/` | Cancel / reschedule / revert / venue override. |
| `selection/` | BSS / YSS nomination. |
| `home_sabha_transfer/` | The OTP-confirmed transfer flow. |
| `password_reset/` | Reset + `who_appointed_me_screen`. |
| `sync/` | `attendance_store`, `pending_marking`, `sync_engine`, `sync_status_screen` — the offline half. |
| `api/` | `api_error.dart` only — the shared error dispatcher. |

`main.dart` wires every feature's api + controller + screen by hand; there is no DI container.

## Exposes

<!-- [coverage: high] -->

_none_

A client app. It consumes `/api/*` (never `/bff/*`, which is web's cookie-session surface).

## Talks To

<!-- [coverage: medium -- package edges from pubspec.yaml are exact; which backend routes each feature calls is inferred from the directory names matching the `/api/*` prefixes, not from reading the api files. ] -->

**Outbound** — depends on `sabha_api` ([[mobile-sabha-api]]) for the typed client, plus
`flutter_appauth` (OIDC), `sqflite` + `path_provider` (the offline store), and `http`.

It reaches the backend's `/api/*` surface: [[backend-identity]] (directory, transfer, password
reset, selection, whoami) and [[backend-attendance]] (occurrences, roster, walk-ins, sync).

Notably it does **not** depend on `shared_kernel`, `identity_domain`, `sabha_domain` or
`attendance_domain` — the four pure-Dart mirror packages.

**Inbound** — _none_. Nothing depends on the app shell.

## Data

<!-- [coverage: low -- inferred from the sqflite dependency and the `sync/` file names; the local schema was not read. ] -->

A local **SQLite** database via `sqflite`, owned by `sync/attendance_store.dart` and holding
`pending_marking` rows — the offline queue that `sync_engine` drains against `POST /api/sync`
(ADR-0007). Table names and schema were not verified; read `attendance_store.dart` before acting on
them.

## Gotchas

<!-- [coverage: medium -- the scaffold state of the mirror packages is directly observable; that this is drift rather than deliberate is an inference. ] -->

The workspace advertises a five-package domain split, but the app shell imports none of it. Every
domain type the app uses comes from the generated `sabha_api` models instead. If you are looking for
"where the mobile identity model lives," the answer is currently *nowhere* — see
[[mobile-identity-domain]].

`melos bootstrap` is required after regenerating `sabha_api`; the generator is driven by
`melos run generate:api`, not by a plain `flutter pub get`.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [sabha_attendance](../../../apps/mobile/sabha_attendance) — `pubspec.yaml`, `lib/` inventory, `main.dart` wiring
- [melos.yaml](../../../apps/mobile/melos.yaml) — workspace membership and the `generate:api` script
- [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md)
