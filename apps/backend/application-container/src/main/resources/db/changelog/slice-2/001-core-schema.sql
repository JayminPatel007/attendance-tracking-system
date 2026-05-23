--liquibase formatted sql

--changeset slice-2:001-kshetras
CREATE TABLE kshetras (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset slice-2:001-sabhas
CREATE TABLE sabhas (
    id              UUID PRIMARY KEY,
    kshetra_id      UUID NOT NULL REFERENCES kshetras(id),
    sabha_kind      TEXT NOT NULL,
    schedule_shape  TEXT NOT NULL CHECK (schedule_shape IN ('WEEKLY_RECURRING', 'MONTHLY_AD_HOC')),
    day_of_week     SMALLINT CHECK (day_of_week BETWEEN 0 AND 6),
    start_time      TIME,
    end_time        TIME,
    standing_venue  TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sabhas_kshetra ON sabhas(kshetra_id);

--changeset slice-2:001-persons
CREATE TABLE persons (
    id              UUID PRIMARY KEY,
    full_name       TEXT NOT NULL,
    gender          TEXT NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
    date_of_birth   DATE,
    mobile          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset slice-2:001-home-sabhas
CREATE TABLE home_sabhas (
    person_id   UUID NOT NULL REFERENCES persons(id),
    sabha_id    UUID NOT NULL REFERENCES sabhas(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (person_id, sabha_id)
);
CREATE INDEX idx_home_sabhas_sabha ON home_sabhas(sabha_id);

--changeset slice-2:001-users
CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    person_id           UUID NOT NULL REFERENCES persons(id),
    username            TEXT NOT NULL UNIQUE,
    keycloak_user_id    UUID NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset slice-2:001-role-assignments
CREATE TABLE role_assignments (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    role        TEXT NOT NULL,
    sabha_id    UUID REFERENCES sabhas(id),
    kshetra_id  UUID REFERENCES kshetras(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_role_assignments_user ON role_assignments(user_id);
CREATE INDEX idx_role_assignments_sabha ON role_assignments(sabha_id);

--changeset slice-2:001-occurrences
CREATE TABLE occurrences (
    id              UUID PRIMARY KEY,
    sabha_id        UUID NOT NULL REFERENCES sabhas(id),
    occurrence_date DATE NOT NULL,
    state           TEXT NOT NULL CHECK (state IN ('SCHEDULED', 'RESCHEDULED', 'CANCELLED', 'OPEN_FOR_MARKING', 'FINALIZED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sabha_id, occurrence_date)
);
CREATE INDEX idx_occurrences_sabha ON occurrences(sabha_id);
CREATE INDEX idx_occurrences_date ON occurrences(occurrence_date);

--changeset slice-2:001-attendance-markings
CREATE TABLE attendance_markings (
    id                  UUID PRIMARY KEY,
    occurrence_id       UUID NOT NULL REFERENCES occurrences(id),
    person_id           UUID NOT NULL REFERENCES persons(id),
    present             BOOLEAN NOT NULL,
    marked_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    marked_by_user_id   UUID NOT NULL REFERENCES users(id),
    UNIQUE (occurrence_id, person_id)
);
CREATE INDEX idx_attendance_markings_occurrence ON attendance_markings(occurrence_id);
