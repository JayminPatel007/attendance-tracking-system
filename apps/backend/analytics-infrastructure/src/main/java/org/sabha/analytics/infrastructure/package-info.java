/**
 * Analytics infrastructure: read-model projection storage, dashboard REST
 * endpoints. Per ADR-0015 this seam is the most likely future extraction
 * candidate — projections must be event-driven, not ad-hoc joins on the
 * transactional tables of other contexts.
 */
package org.sabha.analytics.infrastructure;
