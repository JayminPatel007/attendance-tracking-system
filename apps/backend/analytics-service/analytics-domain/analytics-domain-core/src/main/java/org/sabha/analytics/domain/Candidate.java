package org.sabha.analytics.domain;

import java.util.UUID;

/**
 * A Person who is drifting from one of their Home Sabhas (ADR-0010). The streak
 * is independent per Home Sabha, so a Person can appear as a Candidate for one
 * Home Sabha and not another — hence {@code homeSabhaId} is part of the identity.
 */
public record Candidate(UUID personId, int missedStreak, UUID homeSabhaId, Tier tier) {
}
