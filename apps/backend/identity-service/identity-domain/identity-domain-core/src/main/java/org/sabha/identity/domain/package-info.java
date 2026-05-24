/**
 * Identity domain layer: User, credentials, RoleAssignment aggregates; domain services
 * (authentication policy, role-scoping rules per ADR-0011); outbound ports for
 * persistence and OTP delivery.
 *
 * <p>Pure domain — no Spring, no JPA, no controllers. Per ADR-0019 the only allowed
 * dependency is common-domain.</p>
 */
package org.sabha.identity.domain;
