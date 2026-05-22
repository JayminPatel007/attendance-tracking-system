/**
 * Identity bounded context — application layer. Use-case interactors and the
 * REST controllers that expose them (per ADR-0017). Knows about HTTP and JWTs
 * through narrow Spring dependencies (spring-web, spring-security-oauth2-jose)
 * but not about persistence, messaging, or external services — those live in
 * identity-infrastructure behind ports declared in identity-domain.
 */
package org.sabha.identity.application;
