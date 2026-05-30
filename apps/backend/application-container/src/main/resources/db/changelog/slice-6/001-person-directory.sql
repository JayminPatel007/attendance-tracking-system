--liquibase formatted sql

-- Slice 6: Person Directory de-duplication (ADR-0013). Mobile becomes the
-- system-wide-unique identity key; a child without their own phone links to a
-- guardian's Person record (sharing that mobile through the link) rather than
-- duplicating a mobile field. The name soft-warn matches phonetically
-- (double-metaphone) and by edit-distance (levenshtein), scoped for now to the
-- Kshetra of the Person's Home Sabha (true City scope arrives with Slice 10).

--changeset slice-6:001-fuzzy-extensions
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

--changeset slice-6:001-guardian-link
ALTER TABLE persons ADD COLUMN guardian_person_id UUID REFERENCES persons(id);

-- Mobile is system-wide unique. NULLs (guardian-linked children) are distinct in
-- Postgres, so multiple children sharing a guardian don't collide here.
--changeset slice-6:001-mobile-unique
ALTER TABLE persons ADD CONSTRAINT persons_mobile_unique UNIQUE (mobile);

--changeset slice-6:001-mobile-or-guardian
ALTER TABLE persons ADD CONSTRAINT persons_mobile_or_guardian
    CHECK (mobile IS NOT NULL OR guardian_person_id IS NOT NULL);

-- Defence-in-depth on top of Kshetra scoping so the < 500ms candidate query
-- stays fast as the Directory grows: phonetic equality and trigram prefilter.
--changeset slice-6:001-name-phonetic-index
CREATE INDEX idx_persons_name_phonetic ON persons (dmetaphone(full_name));

--changeset slice-6:001-name-trgm-index
CREATE INDEX idx_persons_name_trgm ON persons USING gin (full_name gin_trgm_ops);

-- A second Sabha in Kshetra Tracer so the dedup-seed Person is in the Kshetra
-- (the name-search scope) without joining the Yuvak tracer Sabha's Roster.
--changeset slice-6:001-dedup-seed-sabha
INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, day_of_week, start_time, end_time, standing_venue) VALUES
    ('00000000-0000-0000-0000-000000000111',
     '00000000-0000-0000-0000-000000000001',
     'REGULAR_BAAL',
     'WEEKLY_RECURRING',
     6,
     '10:00:00',
     '11:00:00',
     'Tracer Hall, Kshetra Tracer');

-- A Directory member in Kshetra Tracer whose name is close to "Ramish Shah" so
-- the phonetic/edit-distance soft-warn is exercisable end-to-end in tests.
--changeset slice-6:001-dedup-seed-person
INSERT INTO persons (id, full_name, gender, mobile) VALUES
    ('00000000-0000-0000-0000-000000000110', 'Ramesh Shah', 'MALE', '+910000000110');

--changeset slice-6:001-dedup-seed-home-sabha
INSERT INTO home_sabhas (person_id, sabha_id) VALUES
    ('00000000-0000-0000-0000-000000000110', '00000000-0000-0000-0000-000000000111');
