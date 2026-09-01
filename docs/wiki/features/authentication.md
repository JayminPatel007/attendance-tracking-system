---
type: feature
title: Authentication
description: Signing in to the mobile and web apps, and resetting a forgotten password.
aliases: [login, sign-in, OIDC, Keycloak, session, password reset, forgot password, OTP, who appointed me, reissue]
tags: [bff]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/backend/application-container/src/main/java/**,
  apps/mobile/sabha_attendance/lib/auth/**,
  apps/mobile/sabha_attendance/lib/password_reset/**,
  apps/web/src/app/password-reset/**,
  apps/web/src/app/shell/**,
  apps/web/projects/identity-domain/src/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-2/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-18/**,
  apps/backend/application-container/src/main/resources/db/changelog/issue-77/**,
  docs/adr/0004-*.md,
  docs/adr/0016-*.md,
  docs/adr/0022-*.md,
  CONTEXT.md
]
issues: [3, 10, 19, 77, 130]
sources:
  - { id: adr-0004, title: "User Authentication: Custom Username + Password, Set by Assigner", resource: ../../adr/0004-user-authentication-username-password.md }
  - { id: adr-0016, title: "OIDC Authentication via Keycloak (Separate Container)", resource: ../../adr/0016-oidc-auth-via-keycloak.md }
  - { id: adr-0022, title: "Web session via a Backend-for-Frontend with an HTTP-only cookie", resource: ../../adr/0022-web-session-via-bff-http-only-cookie.md }
  - { id: context, title: "CONTEXT.md — Karyakar, Roles (each tier has its own role)", resource: ../../../CONTEXT.md }
last_compiled: 6e43fd984ca097e05d67237d341afc11c0bf41ea
---

# Authentication

## What it does

<!-- [coverage: high -- ADR-0016 and ADR-0022 read against SecurityConfig, auth_service.dart and BffSessionController] -->

A **Karyakar** proves who they are, and recovers when they cannot. Credentials live in **Keycloak** —
`users` holds a `keycloak_user_id` link and nothing more. Roles stay ours: Keycloak is the credential
store, not the permission model.

The two clients authenticate differently on purpose (ADR-0022). **Mobile** is a public
Authorization-Code + PKCE client holding its own access token. **Web** holds no token — the backend is
a Backend-for-Frontend that runs the code flow, keeps the tokens in a server-side session, and hands
the browser only an HTTP-only cookie.

A forgotten password is **self-served over OTP**; a lost *mobile* falls back to the appointer
reissuing it. Both are ADR-0004 policy, preserved when ADR-0016 replaced its implementation.

## Flow

<!-- [coverage: medium -- the backend chains and the reset service read end to end; the OIDC round trip is read from configuration and ADRs, not exercised] -->

**Mobile** — `AuthService` wraps `flutter_appauth`: discovery, Authorization Code + PKCE, and back
with an access token in an in-memory `Session` that does not survive a restart. Keycloak's hosted
`UPDATE_PASSWORD` required-action runs *inside* that redirect, so first-login password change never
reaches Flutter.

**Web** — `SessionService` resolves `GET /bff/me`: username, MK and Regional Team membership, and the
**sections** the shell may render. A session-less XHR gets `401`, not a redirect, so the SPA chooses
when to start the login. Logout is `POST /bff/logout`, CSRF-protected.

**Password reset** — three unauthenticated calls, the same on both clients:

1. `POST /api/password-reset/request` → an OTP to the registered mobile, and a `resetId`.
2. `POST /api/password-reset/verify` → a short-lived **reset token** in exchange for the code.
3. `POST /api/password-reset/complete` → the new password, against the token.

The OTP never travels on the final call. `GET /api/who-appointed-me` is the escape hatch for a lost
mobile: keyed only on a username, it returns the appointer's contacts — or the MK's for a Sant, who
has none. The authenticated fallback is `POST /bff/password-reissue`.

**Backend** — two Spring Security chains: `/bff/**` with `oauth2Login` and a cookie session,
`/api/**` as a resource server. `PasswordResetService` supplies only what is its own — which User,
where the code goes, what verify and complete mean — and delegates the OTP halves to
`OtpGuardedFlow`. `PasswordReissueService` is a different path: authenticated, no OTP, caller-supplied
password, audited.

## Rules & authority

<!-- [coverage: high -- SecurityConfig's matchers, OtpChallenge, WindowedOtpSendPolicy and PasswordReissueService read directly] -->

- **Public surface.** `/api/password-reset/**` and `/api/who-appointed-me` are the only `permitAll`
  application routes.
- **The OTP guard is load-bearing, not incidental.** `OtpGuardedFlow.consume` **owns** its
  transaction, because the rollback rules that let a rejected code keep its consequence live there.
  Neither `PasswordResetService.verify` nor its transfer twin is `@Transactional`, and an ArchUnit
  rule holds callers to it. Wrap it and the attempt counter resets on every wrong guess.
- **Challenge budget.** 6 digits, **5-minute** TTL, **5** attempts then locked; **3** sends per mobile
  per rolling hour, **30-second** resend cooldown. Breaches **429**; wrong / expired / exhausted **422**.
- **Codes are hashed at rest** (issue #77): HMAC-SHA256, keyed by a server secret and salted per
  challenge id, compared in constant time. A bare digest was not enough — the 6-digit space is
  trivially brute-forced offline.
- **Reset reveals.** Unknown username is **404**, no registered mobile **422** — deliberately
  distinguishable, so a locked-out user is told which wall they hit.
- **Reissue authority.** The target's appointer, or an MK member for a Sant. Denial is **403** through
  [authorization](../patterns/authorization.md); the new password forces a change on next login.
- **Section visibility is a nav gate, not a check.** `VisibleSections` decides what the shell shows;
  each endpoint re-decides for itself.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `otp`, `passwordreset` and `session` feature packages; the four
  controllers; `HmacOtpHasher` and `KeycloakAdminRestClient` in `identity-messaging`.
- [backend-common-domain](../structure/backend-common-domain.md) — `CallerResolver`, `AuditReadAccess`, `AuthorizationDeniedException`.
- [backend-container](../structure/backend-container.md) — `SecurityConfig`'s two chains and public matchers, the ProblemDetail mapping,
  and `slice-18` / `issue-77` (`password_resets`, then the scrub-and-expire).
- [mobile-shell](../structure/mobile-shell.md) — `lib/auth/` and `lib/password_reset/`; the reset client is hand-rolled `http`,
  outside the generated client because it carries no auth.
- [web](../structure/web.md) — the public `password-reset` routes, `PUBLIC_PATHS`, the shell guard, and `SessionService`.

## Amendments

<!-- [coverage: medium -- slice and issue attribution reconstructed from changelog headers and javadocs; the ADR supersession chain is read directly] -->

- **ADR-0004 → ADR-0016** — the hand-rolled `/login` was replaced by Keycloak. The *policy* is
  unchanged: appointer sets the credentials, forced change on first login, OTP self-service with
  appointer reissue as fallback.
- **ADR-0016 → ADR-0022** (issue #10) — web only: `sabha-web` became a confidential client and the
  SPA stopped holding tokens. Mobile is untouched by it.
- **Slice 18** (issue #19) — the two-step reset, `password_resets` carrying both methods, and
  who-appointed-me.
- **Issue #130** — `OtpGuardedFlow` extracted; reset and transfer share one orchestration.
- **Issue #77** — hashing at rest, with in-flight `PENDING` rows expired and stored codes scrubbed to
  `REDACTED` rather than a hash-on-read fallback.

## Method

- The `OtpGuardedFlow` class javadoc is the source that paid, and it is the only one that could: it
  states *why* `consume` owns the transaction and what breaks if a caller wraps it. Neither ADR
  mentions transactions, and the two call sites only show the absence of an annotation, which reads
  as an oversight until this file tells you it is the design.
- `SecurityConfig`'s matcher list settles the public surface in three lines; the ADRs describe the
  two chains but not which paths escape them.
