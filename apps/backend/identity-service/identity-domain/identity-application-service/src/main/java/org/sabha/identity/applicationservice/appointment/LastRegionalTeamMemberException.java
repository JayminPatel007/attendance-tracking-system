package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.sabha.common.ConflictException;

/**
 * Raised when revoking the final remaining Regional Team member of a (City,
 * demographic) (ADR-0025 §2): the tier can never be emptied, so the last-one-out
 * guard refuses. A state collision rather than an authority denial, so it
 * surfaces as HTTP 409 with a stable {@code code}; nothing is revoked. Appointing
 * a peer first (issue #85) lifts the guard.
 */
public class LastRegionalTeamMemberException extends ConflictException {

    public LastRegionalTeamMemberException(UUID cityId, String demographic) {
        super("Cannot revoke the last remaining Regional Team member for this City and demographic ("
                + demographic + "). Appoint a replacement before revoking.",
                "LAST_REGIONAL_TEAM_MEMBER");
    }
}
