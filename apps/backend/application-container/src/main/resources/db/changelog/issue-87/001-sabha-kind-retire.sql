--liquibase formatted sql

-- Issue #87 — Sabha Kind soft-retire (ADR-0026). A Sabha Kind is reference data
-- the whole type system hangs off, so it is never hard-deleted; the MK marks it
-- inactive instead so no new Sabhas, roles, or Home Sabhas of that kind can be
-- created while existing ones drain. retired_at NULL means active; retired_by
-- attributes the retirement to the acting MK member. Both are nullable and
-- default to NULL (active), so existing kinds remain active on migration.

--changeset issue-87:001-sabha-kinds-retire
ALTER TABLE sabha_kinds ADD COLUMN retired_at TIMESTAMPTZ;
ALTER TABLE sabha_kinds ADD COLUMN retired_by UUID REFERENCES users(id);
