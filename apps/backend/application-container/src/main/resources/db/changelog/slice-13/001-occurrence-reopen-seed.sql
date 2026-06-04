--liquibase formatted sql

-- Slice 13 — Occurrence reopen (ADR-0001). No schema change: the reopen
-- transition reuses the occurrences.state machine and the Slice 3
-- occurrence_state_transitions audit log (action = 'REOPEN'), and the "reopened"
-- badge is derived from that log rather than a denormalized column.
--
-- This seed gives the tracer Kshetra a Nirikshak so the reopen authority and the
-- web two-pane screen can be exercised end-to-end. The Kshetra-tier reopen roles
-- are stored against (kshetra_id, demographic) per ADR-0011 appointment; the
-- tracer Sabha is REGULAR_YUVAK in Kshetra Tracer, so the Nirikshak is scoped to
-- (Kshetra Tracer, YUVAK). A Finalized Occurrence is seeded for it to act on. The
-- keycloak_user_id matches the nirikshak user in infra/keycloak/realm-sabha.json.

--changeset slice-13:001-nirikshak-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000050', 'Nirikshak Tracer', 'MALE', '+910000000050');

--changeset slice-13:001-nirikshak-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-000000000051',
     '00000000-0000-0000-0000-000000000050',
     'nirikshak',
     '00000000-0000-0000-0000-000000000052');

--changeset slice-13:001-nirikshak-role
INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES
    ('00000000-0000-0000-0000-000000000053',
     '00000000-0000-0000-0000-000000000051',
     'NIRIKSHAK',
     '00000000-0000-0000-0000-000000000001',
     'YUVAK');

-- Two Finalized Occurrences so tests that read a never-reopened row and tests
-- that perform the reopen mutation do not race over shared state.
--changeset slice-13:001-finalized-occurrences
INSERT INTO occurrences (id, sabha_id, occurrence_date, state) VALUES
    ('00000000-0000-0000-0000-000000000021',
     '00000000-0000-0000-0000-000000000002',
     DATE '2026-05-17',
     'FINALIZED'),
    ('00000000-0000-0000-0000-000000000022',
     '00000000-0000-0000-0000-000000000002',
     DATE '2026-05-10',
     'FINALIZED');
