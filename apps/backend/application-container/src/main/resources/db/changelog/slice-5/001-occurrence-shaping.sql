--liquibase formatted sql

-- Slice 5: Sanchalak-only Sabha-shaping overrides on an Occurrence (ADR-0001).
-- Per-Occurrence venue override + rescheduled date/time, none of which touch
-- the Sabha's standing schedule or venue.

--changeset slice-5:001-occurrence-shaping-columns
ALTER TABLE occurrences ADD COLUMN venue_override        TEXT;
ALTER TABLE occurrences ADD COLUMN rescheduled_date      DATE;
ALTER TABLE occurrences ADD COLUMN rescheduled_start_time TIME;
ALTER TABLE occurrences ADD COLUMN rescheduled_end_time   TIME;

-- A Sah-Sanchalak on the tracer Sabha, so the Authorization Engine's
-- Sabha-shaping exclusion can be exercised end-to-end. The keycloak_user_id
-- matches the sah-sanchalak user in infra/keycloak/realm-sabha.json.
--changeset slice-5:001-sah-sanchalak-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000006', 'Sah-Sanchalak Tracer', 'MALE', '+910000000006');

--changeset slice-5:001-sah-sanchalak-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-000000000007',
     '00000000-0000-0000-0000-000000000006',
     'sah-sanchalak',
     '00000000-0000-0000-0000-000000000008');

--changeset slice-5:001-sah-sanchalak-role
INSERT INTO role_assignments (id, user_id, role, sabha_id) VALUES
    ('00000000-0000-0000-0000-000000000009',
     '00000000-0000-0000-0000-000000000007',
     'SAH_SANCHALAK',
     '00000000-0000-0000-0000-000000000002');
