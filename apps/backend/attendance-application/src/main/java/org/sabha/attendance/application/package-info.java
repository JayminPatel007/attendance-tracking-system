/**
 * Attendance bounded context — application layer. Use-case interactors and the
 * REST controllers that expose them (per ADR-0017). Depends on
 * attendance-domain (ports) and shared-kernel ({@code CallerResolver}); never
 * on other bounded contexts directly.
 */
package org.sabha.attendance.application;
