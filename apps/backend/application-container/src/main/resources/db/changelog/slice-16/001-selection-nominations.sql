--liquibase formatted sql

-- Slice 16 — BSS/YSS selection nomination + approval (ADR-0006, CONTEXT.md).
--
-- A Regular Sanchalak nominates a Roster Person for the selective track; the
-- demographic Nirdeshak approves (adding an additional selective Home Sabha,
-- leaving the Regular one intact) or rejects. The nomination row doubles as the
-- audit trail — it carries who nominated, who decided, when, and any rejection
-- reason — so Slice 19's audit viewer reads it directly. Deselection moves an
-- APPROVED nomination to DESELECTED, the inverse that removes the selective Home
-- Sabha. The Kshetra / demographic / track are denormalized onto the row so the
-- Nirdeshak's pending queue scopes cheaply without re-walking the hierarchy.

--changeset slice-16:001-selection-nominations
CREATE TABLE selection_nominations (
    id                  UUID PRIMARY KEY,
    person_id           UUID NOT NULL REFERENCES persons(id),
    regular_sabha_id    UUID NOT NULL REFERENCES sabhas(id),
    selective_sabha_id  UUID NOT NULL REFERENCES sabhas(id),
    kshetra_id          UUID NOT NULL REFERENCES kshetras(id),
    demographic         TEXT NOT NULL,
    track               TEXT NOT NULL CHECK (track IN ('BSS', 'YSS')),
    nominated_by        UUID NOT NULL REFERENCES users(id),
    nominated_at        TIMESTAMPTZ NOT NULL,
    status              TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DESELECTED')),
    decided_by          UUID REFERENCES users(id),
    decided_at          TIMESTAMPTZ,
    rejection_reason    TEXT
);

-- The Nirdeshak's pending queue is scoped by (kshetra, demographic).
--changeset slice-16:001-selection-nominations-queue-index
CREATE INDEX idx_selection_nominations_queue
    ON selection_nominations (kshetra_id, demographic, status);

-- At most one open nomination per Person per track — backs the duplicate guard.
--changeset slice-16:001-selection-nominations-pending-unique
CREATE UNIQUE INDEX idx_selection_nominations_one_pending
    ON selection_nominations (person_id, track)
    WHERE status = 'PENDING';

-- A YSS Yuvak Sabha in the tracer Kshetra (00..0001), so a Roster Person of the
-- REGULAR_YUVAK tracer Sabha can be nominated and approved into it end-to-end.
--changeset slice-16:001-selective-sabha-seed
INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue) VALUES
    ('00000000-0000-0000-0000-000000000016',
     '00000000-0000-0000-0000-000000000001',
     'YSS_YUVAK',
     'MONTHLY_AD_HOC',
     'Tracer Hall, Kshetra Tracer');

-- The demographic Nirdeshak for (Kshetra Tracer, YUVAK) — track-shared across
-- Regular and YSS (ADR-0006), so this same row authorizes selective decisions.
-- keycloak_user_id matches the "nirdeshak" user in infra/keycloak/realm-sabha.json.
--changeset slice-16:001-nirdeshak-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000063', 'Nirdeshak Tracer', 'MALE', '+910000000063');

--changeset slice-16:001-nirdeshak-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-000000000061',
     '00000000-0000-0000-0000-000000000063',
     'nirdeshak',
     '00000000-0000-0000-0000-000000000062');

--changeset slice-16:001-nirdeshak-role
INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES
    ('00000000-0000-0000-0000-000000000064',
     '00000000-0000-0000-0000-000000000061',
     'NIRDESHAK',
     '00000000-0000-0000-0000-000000000001',
     'YUVAK');
