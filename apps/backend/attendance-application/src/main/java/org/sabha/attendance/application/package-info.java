/**
 * Attendance bounded context — application layer. Use-case interactors and the
 * REST controllers that expose them (per ADR-0019; controllers move to
 * attendance-application proper in Sub-PR B). Depends on attendance-domain
 * (ports) and common-domain ({@code CallerResolver}); never on other bounded
 * contexts directly.
 */
package org.sabha.attendance.application;
