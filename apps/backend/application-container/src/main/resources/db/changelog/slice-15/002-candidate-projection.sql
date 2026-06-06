--liquibase formatted sql

-- Slice 15 — Re-engagement candidate projection (ADR-0008, ADR-0010). The
-- read-model the dashboard serves, refreshed wholesale on a background cadence by
-- the projection scanner rather than computed live against the transactional
-- tables. Deliberately slim: it stores only the calculator's output (the missed
-- streak and tier per Person × Home Sabha). The dashboard joins this to the
-- structural tables (sabhas → kshetras → zones → cities) and filters by the
-- caller's role_assignments scope at read time, the same CQRS pattern the
-- Occurrence-reopen list uses.

--changeset slice-15:002-reengagement-candidates
CREATE TABLE reengagement_candidates (
    person_id      UUID NOT NULL REFERENCES persons(id),
    home_sabha_id  UUID NOT NULL REFERENCES sabhas(id),
    missed_streak  INT NOT NULL CHECK (missed_streak >= 1),
    tier           TEXT NOT NULL CHECK (tier IN ('CANDIDATE', 'PRIORITY')),
    refreshed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (person_id, home_sabha_id)
);
CREATE INDEX idx_reengagement_candidates_sabha ON reengagement_candidates(home_sabha_id);
