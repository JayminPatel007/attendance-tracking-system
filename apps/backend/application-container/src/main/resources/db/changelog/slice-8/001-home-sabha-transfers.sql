--liquibase formatted sql

-- Slice 8: Verified Home Sabha Transfer (ADR-0002). A transfer holds the OTP
-- consent state between initiate and confirm; on confirm the orchestrator swaps
-- the Person's Home Sabha for the destination's demographic in home_sabhas. The
-- OTP code is stored as-is for v1 (hashing-at-rest is noted as later hardening).

--changeset slice-8:001-home-sabha-transfers
CREATE TABLE home_sabha_transfers (
    id                   UUID PRIMARY KEY,
    person_id            UUID NOT NULL REFERENCES persons(id),
    mobile               TEXT NOT NULL,
    destination_sabha_id UUID NOT NULL REFERENCES sabhas(id),
    initiating_user_id   UUID NOT NULL REFERENCES users(id),
    otp_code             TEXT NOT NULL,
    initiated_at         TIMESTAMPTZ NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL,
    status               TEXT NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'LOCKED')),
    attempts             INT NOT NULL DEFAULT 0
);

-- Backs the per-mobile OTP rate limit (3/hour) and resend cooldown (30s).
--changeset slice-8:001-transfers-mobile-index
CREATE INDEX idx_home_sabha_transfers_mobile ON home_sabha_transfers (mobile, initiated_at);

-- Seed a lateral-transfer subject: a Person with their own mobile whose
-- REGULAR_YUVAK Home Sabha is in a different Kshetra than the tracer destination,
-- so a transfer into the tracer Yuvak Sabha is exercisable end-to-end.
--changeset slice-8:001-transfer-seed-kshetra
INSERT INTO kshetras (id, name) VALUES
    ('00000000-0000-0000-0000-000000000301', 'Kshetra Across');

--changeset slice-8:001-transfer-seed-sabha
INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, day_of_week, start_time, end_time, standing_venue) VALUES
    ('00000000-0000-0000-0000-000000000302',
     '00000000-0000-0000-0000-000000000301',
     'REGULAR_YUVAK',
     'WEEKLY_RECURRING',
     0,
     '19:00:00',
     '20:00:00',
     'Across Hall, Kshetra Across');

--changeset slice-8:001-transfer-seed-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000303', 'Transfer Subject', 'MALE', '+910000000303');

--changeset slice-8:001-transfer-seed-home-sabha
INSERT INTO home_sabhas (person_id, sabha_id) VALUES
    ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000302');
