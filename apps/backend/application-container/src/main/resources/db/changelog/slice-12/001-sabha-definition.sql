--liquibase formatted sql

-- Slice 12 — Sabha definition (ADR-0012). The sabhas table predates this slice
-- (Slice 2 tracer); it now gains a typed FK to the sabha_kinds registry and a
-- created_by audit naming the Nirdeshak who defined it. Both are nullable so the
-- pre-existing seed rows (which carry only the denormalized sabha_kind token)
-- survive. The denormalized sabhas.sabha_kind (TRACK_DEMOGRAPHIC) is still written
-- on create, since the cross-context StructuralHierarchyLookup reads it.

--changeset slice-12:001-sabhas-kind-fk-createdby
ALTER TABLE sabhas ADD COLUMN sabha_kind_id UUID REFERENCES sabha_kinds(id);
ALTER TABLE sabhas ADD COLUMN created_by UUID REFERENCES users(id);
CREATE INDEX idx_sabhas_kind ON sabhas(sabha_kind_id);
