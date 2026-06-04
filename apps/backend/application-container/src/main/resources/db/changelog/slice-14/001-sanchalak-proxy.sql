--liquibase formatted sql

-- Slice 14 — Nirikshak Sanchalak-proxy mode (ADR-0001, CONTEXT.md).
--
-- Three structural additions:
--  1. nirikshak_sabha_assignments — the explicit, mutable set of 3–4 Sabhas a
--     Nirdeshak assigns to a Nirikshak. This is the scope the Authorization Engine
--     checks to grant the Sanchalak proxy toolkit; it is distinct from the
--     (kshetra, demographic) Nirikshak role row that ADR-0011 stores for the
--     Kshetra-tier reopen authority.
--  2. occurrence_state_transitions.on_behalf_of_user_id — the absent Sanchalak a
--     Nirikshak proxy action is attributed to, recorded alongside the acting
--     Nirikshak (actor_user_id). A partial index supports the "find all proxy
--     actions" query (on_behalf_of_user_id IS NOT NULL).
--  3. user_activity — the per-User login / sync timestamps that, together with the
--     last attendance marking, feed the picker's informational "last seen" hint.

--changeset slice-14:001-nirikshak-sabha-assignments
CREATE TABLE nirikshak_sabha_assignments (
    id                  UUID PRIMARY KEY,
    nirikshak_user_id   UUID NOT NULL REFERENCES users(id),
    sabha_id            UUID NOT NULL REFERENCES sabhas(id),
    assigned_by         UUID REFERENCES users(id),
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (nirikshak_user_id, sabha_id)
);
CREATE INDEX idx_nirikshak_assignments_nirikshak ON nirikshak_sabha_assignments(nirikshak_user_id);
CREATE INDEX idx_nirikshak_assignments_sabha ON nirikshak_sabha_assignments(sabha_id);

--changeset slice-14:001-on-behalf-of-user-id
ALTER TABLE occurrence_state_transitions ADD COLUMN on_behalf_of_user_id UUID REFERENCES users(id);
CREATE INDEX idx_occurrence_state_transitions_on_behalf_of
    ON occurrence_state_transitions(on_behalf_of_user_id)
    WHERE on_behalf_of_user_id IS NOT NULL;

--changeset slice-14:001-user-activity
CREATE TABLE user_activity (
    user_id         UUID PRIMARY KEY REFERENCES users(id),
    last_login_at   TIMESTAMPTZ,
    last_synced_at  TIMESTAMPTZ
);
