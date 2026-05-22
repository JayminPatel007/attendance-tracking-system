/**
 * Identity infrastructure: REST controllers, JPA repositories implementing
 * domain ports, password hashing, OTP delivery client. May depend on Spring
 * and JPA. Per ADR-0015 nothing in the domain or application layer above
 * is allowed to import from this package.
 */
package org.sabha.identity.infrastructure;
