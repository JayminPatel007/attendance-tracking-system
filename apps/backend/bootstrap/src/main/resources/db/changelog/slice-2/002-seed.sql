--liquibase formatted sql

-- Slice 2 tracer seed. IDs are fixed so integration tests can assert against
-- known references. The Sanchalak's keycloak_user_id matches the user in
-- infra/keycloak/realm-sabha.json. The "today's Occurrence" uses CURRENT_DATE
-- and runs once per database (Liquibase tracks it).

--changeset slice-2:002-kshetra
INSERT INTO kshetras (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Kshetra Tracer');

--changeset slice-2:002-sabha
INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, day_of_week, start_time, end_time, standing_venue) VALUES
    ('00000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-000000000001',
     'REGULAR_YUVAK',
     'WEEKLY_RECURRING',
     0,
     '19:00:00',
     '20:00:00',
     'Tracer Hall, Kshetra Tracer');

--changeset slice-2:002-sanchalak-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000003', 'Sanchalak Tracer', 'MALE', '+910000000003');

--changeset slice-2:002-sanchalak-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-000000000004',
     '00000000-0000-0000-0000-000000000003',
     'sanchalak',
     '00000000-0000-0000-0000-000000000005');

--changeset slice-2:002-sanchalak-role
INSERT INTO role_assignments (id, user_id, role, sabha_id) VALUES
    ('00000000-0000-0000-0000-000000000006',
     '00000000-0000-0000-0000-000000000004',
     'SANCHALAK',
     '00000000-0000-0000-0000-000000000002');

--changeset slice-2:002-roster-people
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000101', 'Roster Person 1', 'MALE', '+910000000101'),
    ('00000000-0000-0000-0000-000000000102', 'Roster Person 2', 'MALE', '+910000000102'),
    ('00000000-0000-0000-0000-000000000103', 'Roster Person 3', 'MALE', '+910000000103'),
    ('00000000-0000-0000-0000-000000000104', 'Roster Person 4', 'MALE', '+910000000104'),
    ('00000000-0000-0000-0000-000000000105', 'Roster Person 5', 'MALE', '+910000000105');

--changeset slice-2:002-roster-home-sabhas
INSERT INTO home_sabhas (person_id, sabha_id) VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000002'),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000002'),
    ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000002'),
    ('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000002'),
    ('00000000-0000-0000-0000-000000000105', '00000000-0000-0000-0000-000000000002');

--changeset slice-2:002-todays-occurrence
INSERT INTO occurrences (id, sabha_id, occurrence_date, state) VALUES
    ('00000000-0000-0000-0000-000000000020',
     '00000000-0000-0000-0000-000000000002',
     CURRENT_DATE,
     'OPEN_FOR_MARKING');
