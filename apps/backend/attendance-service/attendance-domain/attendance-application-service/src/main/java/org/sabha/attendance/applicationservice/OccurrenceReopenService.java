package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.AuthorizedAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The higher-tier reopen path for a Finalized Occurrence (Slice 13 / ADR-0001):
 * moves it back to Open for Marking so on-the-day mistakes can be corrected after
 * the grace window has locked it.
 *
 * <p>Reopen is deliberately distinct from {@link OccurrenceShapingService}: its
 * authority is the Kshetra tier (Nirikshak / Nirdeshak / Sah-Nirdeshak over the
 * Sabha's own {@code (kshetra, demographic)}), never the Sanchalak who owns
 * shaping, and never the oversight tiers (Sanyojak, Sant, MK). That authority
 * difference is fully encapsulated in the {@link AuthorizationEngine}'s handling
 * of {@link AuthorizedAction#REOPEN}, so this service only owns the reopen
 * vocabulary and its reason requirement; the shared transition orchestration —
 * authorize, mutate, persist, append the reason-bearing audit row the "reopened"
 * badge is derived from, and publish events — lives in {@link
 * OccurrenceWriter}.</p>
 */
@Service
public class OccurrenceReopenService {

    private final OccurrenceWriter writer;

    public OccurrenceReopenService(OccurrenceWriter writer) {
        this.writer = writer;
    }

    @Transactional
    public void reopen(UUID keycloakSubject, UUID occurrenceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ReopenReasonRequiredException(occurrenceId);
        }
        writer.transition(occurrenceId, TransitionActor.user(keycloakSubject, AuthorizedAction.REOPEN),
                OccurrenceAction.REOPEN, reason, Occurrence::reopen);
    }
}
