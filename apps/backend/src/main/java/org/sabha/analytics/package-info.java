/**
 * Analytics seam: read-model projections, dashboards, Re-engagement Candidate calculator.
 * Per ADR-0008, this seam is the most likely extraction candidate; keep dependencies
 * one-way (analytics consumes events, never reads other packages' tables directly).
 */
package org.sabha.analytics;
