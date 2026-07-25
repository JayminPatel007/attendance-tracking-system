package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.AuthorizedAction;

/**
 * Who is driving a write to an Occurrence, and by what authority. This is the
 * only axis on which {@link OccurrenceWriter}'s callers differ: everything else
 * about the write — retry, the audit row, event publication — is identical.
 *
 * <p>A {@link Cron} actor is the Spring-scheduled auto-Open / auto-Finalize
 * scanner (ADR-0021). It holds no role, so it bypasses the {@link
 * AuthorizationEngine} entirely and is audited as {@link ActorKind#SYSTEM}.</p>
 *
 * <p>A {@link SignedIn} actor is a request-bound user identified by their
 * Keycloak subject (ADR-0016). The writer resolves the subject to a User id and
 * checks {@code authority} against the Occurrence's Sabha before the mutation
 * runs; a Nirikshak exercising the Sanchalak proxy (Slice 14) is attributed to
 * the absent Sanchalak on the audit row.</p>
 */
public sealed interface TransitionActor {

    /** How this actor is recorded on the audit row. */
    ActorKind kind();

    static TransitionActor system() {
        return Cron.INSTANCE;
    }

    static TransitionActor user(UUID keycloakSubject, AuthorizedAction authority) {
        return new SignedIn(keycloakSubject, authority);
    }

    record Cron() implements TransitionActor {

        static final Cron INSTANCE = new Cron();

        @Override
        public ActorKind kind() {
            return ActorKind.SYSTEM;
        }
    }

    record SignedIn(UUID keycloakSubject, AuthorizedAction authority) implements TransitionActor {

        @Override
        public ActorKind kind() {
            return ActorKind.USER;
        }
    }
}
