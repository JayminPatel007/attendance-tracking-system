package org.sabha.identity.domain;

import java.util.UUID;

/**
 * One of a Person's current Home Sabhas, paired with the {@code sabha_kind} that
 * fixes its demographic+track dimension (CONTEXT.md). The Verified Home Sabha
 * Transfer swap matches on {@code kind} so only the affected demographic's Home
 * Sabha moves and the others are left untouched.
 */
public record HomeSabhaRef(UUID sabhaId, String kind) {
}
