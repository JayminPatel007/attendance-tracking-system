package org.sabha.attendance.applicationservice;

import java.util.UUID;

public record TransitionActor(ActorKind kind, UUID userId) {

    public static TransitionActor system() {
        return new TransitionActor(ActorKind.SYSTEM, null);
    }

    public static TransitionActor user(UUID userId) {
        return new TransitionActor(ActorKind.USER, userId);
    }
}
