--liquibase formatted sql

-- Slice 14 tracer seed. To exercise the proxy flow end-to-end without disturbing
-- the tracer Sabha (00..0002) — whose Sanchalak's "current shapeable Occurrence"
-- other slices' integration tests assert on — this seeds a dedicated Proxy Sabha
-- (00..00b0) in Kshetra Tracer with its own Sanchalak, and assigns the Slice 13
-- Nirikshak (00..0051) to it. The Nirikshak can then proxy that Sabha's
-- Occurrences, attributed on behalf of its absent Sanchalak, while remaining
-- unassigned to — and therefore rejected on — the tracer Sabha.

--changeset slice-14:002-proxy-sabha
INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, day_of_week, start_time, end_time, standing_venue) VALUES
    ('00000000-0000-0000-0000-0000000000b0',
     '00000000-0000-0000-0000-000000000001',
     'REGULAR_YUVAK',
     'WEEKLY_RECURRING',
     0,
     '19:00:00',
     '20:00:00',
     'Proxy Hall, Kshetra Tracer');

--changeset slice-14:002-proxy-sanchalak-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-0000000000b1', 'Proxy Sanchalak', 'MALE', '+9100000000b1');

--changeset slice-14:002-proxy-sanchalak-user
INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES
    ('00000000-0000-0000-0000-0000000000b2',
     '00000000-0000-0000-0000-0000000000b1',
     'proxy-sanchalak',
     '00000000-0000-0000-0000-0000000000b5');

--changeset slice-14:002-proxy-sanchalak-role
INSERT INTO role_assignments (id, user_id, role, sabha_id) VALUES
    ('00000000-0000-0000-0000-0000000000b7',
     '00000000-0000-0000-0000-0000000000b2',
     'SANCHALAK',
     '00000000-0000-0000-0000-0000000000b0');

-- A "last seen" trail for the Proxy Sanchalak so the picker hint renders. The last
-- attendance marking is derived from attendance_markings; login + sync are here.
--changeset slice-14:002-proxy-sanchalak-activity
INSERT INTO user_activity (user_id, last_login_at, last_synced_at) VALUES
    ('00000000-0000-0000-0000-0000000000b2',
     TIMESTAMPTZ '2026-06-01 08:00:00+00',
     TIMESTAMPTZ '2026-06-03 19:30:00+00');

--changeset slice-14:002-nirikshak-assignment
INSERT INTO nirikshak_sabha_assignments (id, nirikshak_user_id, sabha_id, assigned_by) VALUES
    ('00000000-0000-0000-0000-000000000060',
     '00000000-0000-0000-0000-000000000051',
     '00000000-0000-0000-0000-0000000000b0',
     '00000000-0000-0000-0000-0000000000b2');

-- Scheduled Occurrences on the Proxy Sabha for the toolkit to act on: one the
-- Nirikshak cancels as proxy, one the Sanchalak shapes directly (so the audit
-- attribution of proxy vs direct can be contrasted end-to-end). Dated well into the
-- future so the auto-open/finalize cron leaves them Scheduled.
--changeset slice-14:002-proxy-occurrences
INSERT INTO occurrences (id, sabha_id, occurrence_date, state) VALUES
    ('00000000-0000-0000-0000-0000000000b3',
     '00000000-0000-0000-0000-0000000000b0',
     DATE '2026-08-02',
     'SCHEDULED'),
    ('00000000-0000-0000-0000-0000000000b6',
     '00000000-0000-0000-0000-0000000000b0',
     DATE '2026-08-09',
     'SCHEDULED');
