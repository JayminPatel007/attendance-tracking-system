--liquibase formatted sql

--changeset slice-3:001-occurrence-state-transitions
CREATE TABLE occurrence_state_transitions (
    id              UUID PRIMARY KEY,
    occurrence_id   UUID NOT NULL REFERENCES occurrences(id),
    from_state      TEXT NOT NULL,
    to_state        TEXT NOT NULL,
    action          TEXT NOT NULL,
    actor_kind      TEXT NOT NULL CHECK (actor_kind IN ('USER', 'SYSTEM')),
    actor_user_id   UUID REFERENCES users(id),
    reason          TEXT,
    at_timestamp    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_occurrence_state_transitions_occurrence
    ON occurrence_state_transitions(occurrence_id);
CREATE INDEX idx_occurrence_state_transitions_at
    ON occurrence_state_transitions(at_timestamp);
