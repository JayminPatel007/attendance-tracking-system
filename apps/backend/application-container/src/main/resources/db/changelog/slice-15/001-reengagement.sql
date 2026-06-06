--liquibase formatted sql

-- Slice 15 — Re-engagement (ADR-0010). The MK-owned thresholds at which a Person
-- becomes a re-engagement candidate (consecutive missed Home Sabha Occurrences)
-- and then a priority. A single-row config table (id pinned to 1) the MK reads and
-- updates; the Calculator reads it once per run. Seeded with the ADR defaults 3/6.

--changeset slice-15:001-analytics-thresholds
CREATE TABLE analytics_thresholds (
    id                  SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    candidate_threshold INT NOT NULL CHECK (candidate_threshold >= 1),
    priority_threshold  INT NOT NULL CHECK (priority_threshold >= candidate_threshold),
    updated_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO analytics_thresholds (id, candidate_threshold, priority_threshold)
VALUES (1, 3, 6);
