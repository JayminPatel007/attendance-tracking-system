--liquibase formatted sql

-- Slice 10 — Structural creation (ADR-0009). Cities, Zones, and Sabha Kinds are
-- created by the Madhyastha Karyalaya; Kshetras by the Zone's Sanyojak. Each
-- carries a created_by audit field naming the creating User. The geographic
-- chain City → Zone → Kshetra is wired with foreign keys; the pre-existing
-- kshetras table gains its parent Zone and created_by (nullable, so the Slice 2
-- tracer seed row survives). role_assignments gains zone_id so a Sanyojak's
-- Zone scope is representable (populated by role appointment in Slice 11).

--changeset slice-10:001-cities
CREATE TABLE cities (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset slice-10:001-zones
CREATE TABLE zones (
    id          UUID PRIMARY KEY,
    city_id     UUID NOT NULL REFERENCES cities(id),
    name        TEXT NOT NULL,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_zones_city ON zones(city_id);

--changeset slice-10:001-sabha-kinds
CREATE TABLE sabha_kinds (
    id          UUID PRIMARY KEY,
    demographic TEXT NOT NULL,
    track       TEXT NOT NULL,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (demographic, track),
    CONSTRAINT sanyukta_regular_only CHECK (demographic <> 'SANYUKTA' OR track = 'REGULAR')
);

--changeset slice-10:001-kshetras-zone-createdby
ALTER TABLE kshetras ADD COLUMN zone_id UUID REFERENCES zones(id);
ALTER TABLE kshetras ADD COLUMN created_by UUID REFERENCES users(id);
CREATE INDEX idx_kshetras_zone ON kshetras(zone_id);

--changeset slice-10:001-role-assignments-zone
ALTER TABLE role_assignments ADD COLUMN zone_id UUID REFERENCES zones(id);
CREATE INDEX idx_role_assignments_zone ON role_assignments(zone_id);
