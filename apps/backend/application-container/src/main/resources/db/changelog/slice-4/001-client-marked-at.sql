--liquibase formatted sql

--changeset slice-4:001-attendance-markings-client-marked-at
ALTER TABLE attendance_markings
    ADD COLUMN client_marked_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE attendance_markings
    ALTER COLUMN client_marked_at DROP DEFAULT;
CREATE INDEX idx_attendance_markings_client_marked_at
    ON attendance_markings(occurrence_id, client_marked_at DESC);
