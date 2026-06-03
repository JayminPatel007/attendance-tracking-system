--liquibase formatted sql

-- Slice 11 — Role appointment (ADR-0011). role_assignments becomes the single
-- record of who holds which role at which scope, written in one transaction with
-- the Person / User it appoints. It gains the appointedBy / appointedAt audit the
-- ADR mandates, a demographic token for the per-(scope, demographic) tiers
-- (Nirikshak / Nirdeshak / Sah-Nirdeshak / Sanyojak / Regional Team / Sant), and
-- a city_id for the City-level tiers. Sabha-scoped roles (Sanchalak /
-- Sah-Sanchalak) keep using sabha_id; Kshetra / Zone tiers use the existing
-- kshetra_id / zone_id columns. New role strings REGIONAL_TEAM and SANT live
-- outside the operational Role enum, alongside MADHYASTHA_KARYALAYA.

--changeset slice-11:001-role-assignments-appointment-audit
ALTER TABLE role_assignments ADD COLUMN appointed_by UUID REFERENCES users(id);
ALTER TABLE role_assignments ADD COLUMN appointed_at TIMESTAMPTZ;

--changeset slice-11:001-role-assignments-scope
ALTER TABLE role_assignments ADD COLUMN demographic TEXT;
ALTER TABLE role_assignments ADD COLUMN city_id UUID REFERENCES cities(id);
CREATE INDEX idx_role_assignments_city ON role_assignments(city_id);
