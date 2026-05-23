--liquibase formatted sql

--changeset slice-2:003-occurrence-version
ALTER TABLE occurrences ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
