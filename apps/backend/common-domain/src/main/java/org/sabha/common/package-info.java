/**
 * Common domain: cross-context value objects, identifiers, AggregateRoot base,
 * DomainEvent + publisher port, domain exception base classes, and cross-context
 * ports (CallerResolver). Pure Java; no Spring, no JPA, no framework
 * dependencies.
 *
 * <p>Per ADR-0019, every bounded context may depend on common-domain; the
 * converse is forbidden. Anything that ends up here must be genuinely
 * cross-context — when in doubt, leave it inside the context that originated
 * it.</p>
 */
package org.sabha.common;
