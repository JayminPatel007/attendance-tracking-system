--liquibase formatted sql

-- Slice 17 — Sant landing + universal-read scope (issue #18, ADR-0010, ADR-0011).
-- A Sant reads analytics for any City in the State, filtered to a City they pick;
-- the chosen City IS their persisted default, stored on the User record. The
-- dashboard (analytics) owns this preference read/write directly against the
-- users row. Nullable — empty until the Sant first picks, in which case the
-- dashboard shows nothing.

--changeset slice-17:001-users-default-city
ALTER TABLE users ADD COLUMN default_city_id UUID REFERENCES cities(id);

-- A Sant login for the dev stack and manual testing. Sant is recorded as a
-- role_assignments row with role = 'SANT' (outside the operational Role enum,
-- ADR-0011); the universal-read exception ignores any formal City/demographic on
-- the row, so the seed leaves the scope null. keycloak_user_id matches the "sant"
-- user in infra/keycloak/realm-sabha.json.
--changeset slice-17:001-sant-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000073', 'Sant Tracer', 'MALE', '+910000000073');

--changeset slice-17:001-sant-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-000000000071',
     '00000000-0000-0000-0000-000000000073',
     'sant',
     '00000000-0000-0000-0000-000000000072');

--changeset slice-17:001-sant-role
INSERT INTO role_assignments (id, user_id, role) VALUES
    ('00000000-0000-0000-0000-000000000074',
     '00000000-0000-0000-0000-000000000071',
     'SANT');
