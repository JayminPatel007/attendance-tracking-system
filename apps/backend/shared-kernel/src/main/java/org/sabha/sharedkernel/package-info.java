/**
 * Shared kernel: cross-context value objects, identifiers, audit SPI, and domain
 * event base classes. Pure Java; no Spring, no JPA, no framework dependencies.
 *
 * <p>Per ADR-0015, every bounded context may depend on shared-kernel; the converse
 * is forbidden. Anything that ends up here must be genuinely cross-context — when in
 * doubt, leave it inside the context that originated it.</p>
 */
package org.sabha.sharedkernel;
