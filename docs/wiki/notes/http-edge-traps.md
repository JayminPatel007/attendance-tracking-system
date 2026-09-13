---
type: note
title: HTTP Edge Traps
description: Traps in the request edge — the argument resolver that identifies the caller, and what springdoc makes of it.
bears_on: [backend-container, module-ring]
source_paths: [
  apps/backend/common/common-application/src/main/**,
  apps/backend/common/common-application/pom.xml,
  apps/backend/application-container/src/main/java/org/sabha/container/OpenApiConfig.java,
  apps/backend/application-container/src/main/java/org/sabha/container/SecurityConfig.java,
  apps/backend/application-container/src/test/java/org/sabha/container/IntraModuleArchitectureRulesTest.java
]
last_verified: 0bbc9ab1874f015c13decf8b7785261de4efb123
---

# HTTP Edge Traps

## A custom argument-resolver parameter becomes a required query parameter in the OpenAPI document

**Symptom** · `@CurrentUser UserId caller` renders in `openapi.json` as
`{"name":"caller","in":"query","required":true}` on *every* authenticated endpoint — a parameter no
client can send and the server never reads. The generated Dart and TypeScript clients then demand it
as a mandatory argument.

**Cause** · springdoc documents controller parameters it does not recognise. It has no way to know a
parameter is bound by a `HandlerMethodArgumentResolver` from the security context rather than from
the request.

**Fix** · Register the annotation on springdoc's global ignore list —
`SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class)`, in `OpenApiConfig`'s static
block, which runs on class load before the first document is built. Any future custom resolver needs
the same line. The drift gate (issue #73) catches it, so the symptom is a failing contract test
rather than a shipped broken client — but only if you regenerate.

**Discovered** · 2026-09-13, issue #203 / ADR-0030.

## An exception message raised at the edge lands in the client's response body

**Symptom** · A 403 from the edge carried a serialized `AnonymousAuthenticationToken` — principal and
authorities — in its RFC 9457 `detail`.

**Cause** · `GlobalExceptionHandler.forbidden` maps `AuthorizationDeniedException` by putting
`ex.getMessage()` directly into `detail`. Anything interpolated into a message thrown below that
handler is on the wire.

**Fix** · Interpolate only values you would publish. For the no-credential case that is a fixed
string, not the `Authentication`.

**Discovered** · 2026-09-13, issue #203 review.

## A handler that omits `@CurrentUser` is invisible to every gate in the build

**Symptom** · `GET /api/sabhas/{sabhaId}/monthly-compliance` shipped in Slice 12 taking no principal
at all, and stayed that way for four months. `SecurityConfig` is `anyRequest().authenticated()` on
both chains and there is no `@PreAuthorize` in the codebase, so any authenticated user could ask
about any Sabha id.

**Cause** · Nothing fails when a parameter is *absent*. The compiler is happy, springdoc documents
the endpoint like any other, and the drift gate (issue #73) compares the document to the controllers
— it has no opinion about what the controllers should say. ADR-0030's sweep converted the endpoints
that parsed a subject; this one had nothing to convert, so it was never visited. An omission is not a
diff.

**Fix** · `every_handler_resolves_its_caller` in `IntraModuleArchitectureRulesTest` requires a
`@CurrentUser` parameter on every HTTP-mapped method in a `*-application` module, against a named
exemption list. The list fails three ways, not one: an unexempt handler without a caller, an exempt
handler that has *since gained* one, and an entry matching no handler. The second and third are what
stop it becoming a list that only grows.

**Also worth knowing** · Reaching a query port straight from a handler is *not* the smell here —
nine controllers do it and that is the accepted shape (see issue #203's wrapper rule, which pushes
in the same direction). The invariant is narrower: the handler resolves a caller and passes it down.

**Discovered** · 2026-09-13, issue #209.

## ADR-0012's Nirdeshak-side view of the Compliance Nudge was never built

Not a trap — a fact that reads like one. ADR-0012 says the nudge is surfaced "to the Sanchalak (and
visibility for the Nirdeshak)". Only the Sanchalak half exists, via `GET /api/sanchalak/monthly-sabhas`.
The per-Sabha `monthly-compliance` endpoint deleted in issue #209 *looked* like the Nirdeshak half
and was not — nothing ever called it, and it resolved no caller, so it could not have scoped a read
to a Nirdeshak anyway. If you are building that view, you are starting from zero, not restoring
something.

**Discovered** · 2026-09-13, issue #209.
