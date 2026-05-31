--liquibase formatted sql

-- Slice 7: Walk-in marking. An Attendance Marking now carries a discriminator
-- separating ROSTER presence (a Person marked against their Home Sabha's Roster)
-- from a WALK_IN (a Person attending a Sabha that is not one of their Home Sabhas).
-- Walk-ins never change Home Sabha and are excluded from missed-Occurrence streak
-- analytics (re-engagement, Slice 15). Existing rows are roster marks, so the
-- column defaults to ROSTER.

--changeset slice-7:001-marking-type
ALTER TABLE attendance_markings
    ADD COLUMN marking_type TEXT NOT NULL DEFAULT 'ROSTER'
    CHECK (marking_type IN ('ROSTER', 'WALK_IN'));
