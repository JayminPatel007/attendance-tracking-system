# OIDC Authentication via Keycloak (Separate Container)

**Status**: accepted. **Supersedes the *implementation shape* of [ADR-0004](0004-user-authentication-username-password.md)** while preserving its substantive policy.

User authentication is delegated to a **Keycloak** container that runs alongside the backend. Keycloak is the OIDC provider; the Spring Boot backend is an OAuth2 resource server that validates JWTs against Keycloak's JWKS endpoint. The mobile and web apps use Authorization Code + PKCE via the standard OIDC flow.

ADR-0004's substantive policy — *the assigning Karyakar sets the username and password; the User is forced to change the password on first login; password reset uses mobile-OTP self-service with assigner-reissue as fallback* — is unchanged. What this ADR replaces is the *implementation shape*: instead of a hand-rolled `/login` endpoint returning an opaque bearer token, we ride on OIDC primitives and let Keycloak own the credential store and the login UX.

## Why Keycloak rather than Spring Authorization Server in-process

Spring Authorization Server would have kept the backend a single deployable and matched ADR-0008/0015's "modular monolith" stance more naturally. We picked Keycloak instead because:

- **Battery-included admin surface.** Realm management, client registration, required-action workflows, password policy, account-management UI for the lost-mobile reset path — Keycloak ships these. We would have built each as bespoke Spring Authorization Server extensions.
- **Hosted forms for free.** First-login forced password change, password reset via OTP, account self-management — all these become Keycloak required-actions with hosted HTML rather than three separate Flutter / Angular screens. ADR-0004's force-change-pw requirement becomes the `UPDATE_PASSWORD` required-action; Slice 18's OTP reset becomes a configured authenticator. The mobile + web clients open Keycloak's URLs in a browser tab / `flutter_appauth` window and only get redirected back once the action is done.
- **OIDC discovery doc and JWKS endpoint out of the box** — clients use standard OIDC libraries instead of bespoke code that knows our endpoints.
- **One-way door cost is acceptable.** The team is small; running a second container locally and in CI is a tractable cost; the alternative is feature-by-feature reinvention.

We accept the cost: one extra container in `docker-compose.yml`, one extra Testcontainer in integration tests, one extra service to operate.

## Why full OIDC surface up front rather than minimal

Slice 2 only needs enough OIDC for one mobile client to authenticate. We commit to the full surface (discovery, JWKS, authorization endpoint, token endpoint, account REST, admin REST) anyway because:

- Keycloak exposes the full surface by default — there is no "minimal mode" to opt into.
- Slice 9 (web auth shell) and Slice 18 (password reset) both need surfaces Slice 2 alone wouldn't. Spending zero effort now to "hide" parts we'll need in two slices is wasted work.
- Future role-appointment slices (Slice 11) will provision users via the Admin REST API — that path needs the surface up.

## Architecture

```
                 ┌────────────────────┐
   mobile/web ──▶│      Keycloak      │      realm: sabha
                 │  (OIDC provider)   │      clients: sabha-mobile (public+PKCE),
                 │                    │               sabha-web (public+PKCE)
                 └─────┬─────────▲────┘      required-actions: UPDATE_PASSWORD
                       │ JWKS    │ Admin REST       (set when assigner provisions a User)
                       │         │
                       ▼         │
                 ┌─────┴─────────┴────┐
                 │   Spring Boot      │      identity-infrastructure:
                 │  (resource server) │      - Spring Security oauth2-resource-server
                 │                    │      - JWT decoder against Keycloak JWKS
                 └─────────┬──────────┘      - KeycloakAdminClient port (used at role-assignment
                           │                   to create users + set required-actions)
                           ▼
                 ┌────────────────────┐
                 │      Postgres      │      - app data only (users, persons, sabhas, …)
                 │                    │      - users table stores keycloak_user_id (UUID)
                 │                    │        as the link to Keycloak's user store
                 └────────────────────┘
```

### Realm and clients

- **Realm**: `sabha`. Bootstrapped from a realm-import JSON file mounted into the Keycloak container, identical between docker-compose and Testcontainers.
- **Clients**:
  - `sabha-mobile` — public client, Authorization Code + PKCE, deep-link redirect URI (`com.sabha.app:/oauth2redirect`).
  - `sabha-web` — public client, Authorization Code + PKCE, redirect URI under the Angular app's origin. Added in Slice 9; defined in the realm JSON now to keep the file stable.
- **Realm roles**: not used for backend authorization. The backend reads the JWT `sub`, looks up the corresponding `users.keycloak_user_id`, and authorises from our own `role_assignments` table. Keycloak is the credential store, not the permission model.

### User provisioning and the assigner flow

When a Karyakar appoints a new User (this lands in Slice 11 — anticipated here so the model is consistent):

1. Karyakar enters chosen username + password in our UI.
2. Our backend calls Keycloak Admin REST: `POST /admin/realms/sabha/users` with username, temporary password, and `requiredActions: ["UPDATE_PASSWORD"]`.
3. We persist the returned Keycloak user ID in `users.keycloak_user_id`, link to the `persons` row, and record the `role_assignments` row — all in the same transaction. If Keycloak returns an error (e.g., username collision), the transaction rolls back and the Karyakar sees the error before committing.

On first login, Keycloak sees `UPDATE_PASSWORD` on the user, serves its hosted change-password page in the OIDC flow, then redirects back to the client with an authorization code. The mobile / web app never sees the password.

### Force-change-password on Slice 2

The seed migration calls Keycloak Admin REST once at first boot to ensure the seeded Sanchalak exists with `UPDATE_PASSWORD` set. The behavior is then exactly the assigner flow above, with the install bootstrap acting as the implicit "assigner."

### Token validation

`identity-infrastructure` configures Spring Security as an OAuth2 resource server with `issuer-uri` pointing at the realm. All API endpoints except `/actuator/health` require a Bearer token. A custom `JwtAuthenticationConverter` looks up the local `users` row by `keycloak_user_id` and exposes the resolved `User` as the principal so downstream code uses our domain type rather than Spring's `Jwt`.

## Consequences

- **docker-compose adds a Keycloak service.** Realm config is imported from `infra/keycloak/realm-sabha.json` (committed). Keycloak uses its embedded H2 for the dev compose stack — its admin password lives in `.env.example`. Not the source of truth for production deployment; that's out of scope for Slice 2.
- **Testcontainers spins up Keycloak alongside Postgres** via `com.github.dasniko:testcontainers-keycloak`. Same realm JSON is imported, so tests and the local stack agree on client IDs and required-actions.
- **`users.keycloak_user_id` is the canonical foreign key into Keycloak.** Our `users` table no longer stores a password hash; that column does not exist. Username is mirrored in both stores for diagnostics, but Keycloak is authoritative for credentials.
- **The Karyakar's role-assignment transaction now spans two systems.** Keycloak provisioning is the first step; if it fails the local transaction rolls back. The reverse — Keycloak succeeded but the local insert failed — is handled by a compensating delete via Admin REST inside the same try/catch. Two-phase commit is not used; the compensating-delete path is documented in the role-assignment slice's tests.
- **Slice 18 (password reset) becomes a Keycloak configuration task** rather than custom code: enable the "Forgot password" flow on the realm and point its OTP authenticator at the user's mobile number stored on the Person record. The assigner-reissue fallback becomes "Karyakar opens our admin UI → we call Keycloak Admin REST to reset the password and re-set `UPDATE_PASSWORD`."
- **The backend remains one Spring Boot deployable.** Keycloak is operational infrastructure (like Postgres), not part of the modular monolith. ADR-0015 is unchanged.
- **No ADR-0004 contradiction.** Username + password remains the user-facing credential; the assigner-sets-both rule is preserved; first-login force-change is preserved. Only the wire protocol and credential store change.
