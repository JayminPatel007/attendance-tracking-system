package org.sabha.identity.applicationservice.selection;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the demographic Nirdeshak's currently-selected list (ADR-0006): a
 * Person whose nomination was APPROVED and who therefore holds the selective Home
 * Sabha. Carries {@code personId} and {@code selectiveSabhaId} so the web can drive
 * a deselect without further lookups, plus who approved them and when for context.
 */
public record SelectedPersonItem(
        UUID nominationId,
        UUID personId,
        String personName,
        UUID selectiveSabhaId,
        String demographic,
        String track,
        UUID decidedBy,
        String decidedByName,
        Instant decidedAt) {
}
