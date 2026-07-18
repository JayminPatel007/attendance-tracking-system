--liquibase formatted sql

-- Issue #89 — Role revocation with inheritance (ADR-0026). A role assignment is
-- the single record of who holds which role at which scope, so it is never
-- hard-deleted; the current holder of the appointing scope marks it revoked
-- instead. The row and its appointed_by/at survive so the appointment history
-- and any inherited structure remain intact for a successor. revoked_at NULL
-- means active; revoked_by attributes the revocation to the acting holder. Both
-- are nullable and default to NULL (active), so existing assignments remain
-- active on migration.

--changeset issue-89:001-role-assignments-revoke
ALTER TABLE role_assignments ADD COLUMN revoked_at TIMESTAMPTZ;
ALTER TABLE role_assignments ADD COLUMN revoked_by UUID REFERENCES users(id);
