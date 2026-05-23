/**
 * Attendance context — Data-access adapter (Clean Architecture interface-adapters ring).
 *
 * <p>JDBC implementations of the driven ports declared in
 * {@code attendance-application-service}: occurrence load/save with cascaded
 * marking persistence, optimistic-lock detection on update, and the read-side
 * current-roster projection that joins identity + sabha + attendance tables.
 * Per ADR-0019.</p>
 */
package org.sabha.attendance.dataaccess;
