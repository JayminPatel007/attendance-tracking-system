# Web session via a Backend-for-Frontend with an HTTP-only cookie

**Status**: accepted. **Supersedes the *web-client shape* of [ADR-0016](0016-oidc-auth-via-keycloak.md)** (the `sabha-web` public-client + in-SPA-token design) while preserving everything else in ADR-0016 — Keycloak remains the OIDC provider, the realm and required-actions are unchanged, and the **mobile** client stays a public Authorization-Code + PKCE client holding its own token.

The Angular web app does **not** hold OIDC tokens. Instead the Spring Boot backend acts as a **Backend-for-Frontend (BFF)**: it is an OAuth2 *client* (Spring Security `oauth2Login`) that runs the Authorization-Code flow against Keycloak, holds the resulting tokens in a **server-side HTTP session**, and hands the browser only an **HTTP-only, `SameSite=Lax`, `Secure` session cookie**. The SPA authenticates every call by sending that cookie; it never sees an access or refresh token.

## Why a BFF cookie instead of tokens in the SPA

ADR-0016 chose a `sabha-web` public client with PKCE, with the SPA holding the access token. We change that for the web tier because:

- **No token reachable from JavaScript.** An HTTP-only cookie is not readable by JS, so an XSS foothold cannot exfiltrate a bearer token. A token in `localStorage`/memory is the SPA's single most attractive XSS target.
- **Refresh-token handling leaves the browser.** Silent renew, refresh-token rotation, and revocation all live server-side where they belong, instead of being re-implemented (imperfectly) in Angular.
- **The backend is already a modular monolith.** The web BFF endpoints call the in-process application services directly — there is no second API to relay tokens to, so the usual BFF "token relay" cost does not apply here. The cookie session is the only added moving part.
- **Confidential client.** Because the credential flow now runs entirely server-side, `sabha-web` becomes a **confidential** client with a secret held by the backend, which is strictly stronger than a public client.

We accept the cost: the backend gains a server-side session (in-memory for the dev stack; a shared session store is a deployment concern out of scope here), and CSRF protection becomes mandatory for the cookie-authenticated routes (the public-client/token design did not need it).

## Architecture

Two Spring Security filter chains in `application-container`, ordered by request matcher:

```
  ┌──────────────┐   cookie (HTTP-only)   ┌────────────────────────────┐
  │ Angular SPA  │ ─────────────────────▶ │ Chain 1: /bff/**, /oauth2/**│  oauth2Login (Auth Code)
  │ (browser)    │ ◀───── 302 login ───── │   server-side session       │  ── tokens kept here
  └──────────────┘                        │   CSRF (cookie+header)      │
                                          └─────────────┬──────────────┘
                                                        │ in-process call
                                                        ▼
                                            identity/sabha/... application services
  ┌──────────────┐   Bearer JWT                         ▲
  │ Flutter app  │ ────────────────────────────────────┘
  │ (mobile)     │       ┌────────────────────────────┐
  └──────────────┘ ────▶ │ Chain 2: /api/**            │  oauth2ResourceServer (JWT)  (unchanged)
                         └────────────────────────────┘
```

- **Chain 1 (web, order 1)** matches the BFF surface (`/bff/**`, plus Spring's `/oauth2/**` and `/login/oauth2/**` endpoints). `oauth2Login` against Keycloak; tokens stored in the `HttpSession`; the browser holds only the session cookie. CSRF enabled via the `CookieCsrfTokenRepository` (double-submit: a readable `XSRF-TOKEN` cookie the SPA echoes in an `X-XSRF-TOKEN` header). For an unauthenticated **XHR/fetch** the chain returns **401** rather than a 302 redirect, so the SPA can decide when to start the login redirect.
- **Chain 2 (api, order 2)** is the existing resource-server chain for `/api/**`, unchanged. The mobile app keeps working exactly as before.

The web's "who am I + what can I see" comes from a small BFF endpoint (`GET /bff/me`) that resolves the session's Keycloak subject to the local `User` and returns username + the set of UI **sections** the user may see. First-login forced password change is unchanged from ADR-0016: it is Keycloak's `UPDATE_PASSWORD` required-action served as a hosted form *inside* the Authorization-Code redirect, so neither the SPA nor the BFF implements a change-password screen.

## Realm / client change

`sabha-web` changes from a **public** client to a **confidential** client (`publicClient: false`, a generated secret, `standardFlowEnabled: true`). Its redirect URI becomes the BFF callback (`{backend-origin}/login/oauth2/code/keycloak`) rather than an Angular route, and `post.logout.redirect.uris` points back at the SPA origin. The secret is supplied to the backend via configuration (`spring.security.oauth2.client.registration.keycloak.client-secret`); for the dev compose stack it lives in `.env.example`, consistent with ADR-0016's handling of the admin password. `sabha-mobile` and `sabha-test` are untouched.

## Consequences

- **`application-container` gains `spring-boot-starter-oauth2-client`** and a second `SecurityFilterChain` bean. ADR-0019's "SecurityFilterChain lives in application-container" is unchanged — there are now two, both here.
- **CSRF is enabled on the web chain** and disabled on the API chain (Bearer tokens are not ambient credentials). The SPA must echo the `XSRF-TOKEN` cookie on mutating requests.
- **Server-side session.** Fine as in-memory for one dev node; a multi-node deployment needs a shared session store (e.g. Spring Session + Redis). Out of scope for Slice 9; noted for the deployment slice.
- **No contradiction with the mobile flow.** ADR-0016's mobile public-client + PKCE + in-app token design stands. Only the *web* tier moves to the BFF model.
- **Keycloak still owns credentials and required-actions.** This ADR changes only *where the web tokens live and how the browser proves its session*, not the credential store, the force-change-password policy, or the OTP-reset path (Slice 18).
