---
type: note
title: HTTP Edge Traps
description: Traps in the request edge — the argument resolver that identifies the caller, and what springdoc makes of it.
bears_on: [backend-container, module-ring]
source_paths: [
  apps/backend/common-application/src/main/**,
  apps/backend/common-application/pom.xml,
  apps/backend/application-container/src/main/java/org/sabha/container/OpenApiConfig.java,
  apps/backend/application-container/src/main/java/org/sabha/container/SecurityConfig.java
]
last_verified: 6eeb09c6fa8db613f003d3539bacc2d9bfb41c16
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
