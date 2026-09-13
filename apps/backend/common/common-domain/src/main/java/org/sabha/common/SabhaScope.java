package org.sabha.common;

import java.util.UUID;

/**
 * The structural scope a Sabha sits in, as seen across the bounded-context seam
 * (ADR-0019): the Kshetra it belongs to and its {@code (demographic, track)}
 * kind. Demographic and track are carried as string tokens rather than the sabha
 * context's enums, so the identity context's appointment Authorization Engine can
 * match scope without depending on sabha's domain types.
 */
public record SabhaScope(UUID kshetraId, String demographic, String track) {
}
