/**
 * Attendance seam: AttendanceMarking capture, Walk-ins, sync protocol.
 * Per ADR-0008, cross-package communication uses application services or domain events;
 * other packages MUST NOT reach into this package's aggregates directly.
 */
package org.sabha.attendance;
