--liquibase formatted sql

-- Slice 18: Password reset via OTP (ADR-0004). One table backs both reset paths
-- so the audit log (Slice 19) reads them uniformly:
--  * SELF_SERVICE_OTP rows carry the OTP consent state between request / verify /
--    complete (code, expiry, attempts, derived status, and the post-verify reset
--    token). The (mobile, initiated_at) index backs the 3/hour rate limit and the
--    30-second resend cooldown, mirroring home_sabha_transfers (Slice 8).
--  * ASSIGNER_REISSUE rows are audit-only: an appointing Karyakar (or an MK member,
--    for Sants) reset a User's password out-of-band. No OTP columns; the actor and
--    timestamp are the audit payload.
-- The OTP code and reset token are stored as-is for v1 (hashing-at-rest is noted
-- as later hardening, as with home_sabha_transfers).

--changeset slice-18:001-password-resets
CREATE TABLE password_resets (
    id                       UUID PRIMARY KEY,
    target_user_id           UUID NOT NULL REFERENCES users(id),
    keycloak_user_id         UUID,
    method                   TEXT NOT NULL CHECK (method IN ('SELF_SERVICE_OTP', 'ASSIGNER_REISSUE')),
    mobile                   TEXT,
    otp_code                 TEXT,
    initiated_at             TIMESTAMPTZ NOT NULL,
    otp_expires_at           TIMESTAMPTZ,
    attempts                 INT NOT NULL DEFAULT 0,
    status                   TEXT CHECK (status IN ('PENDING', 'VERIFIED', 'COMPLETED', 'EXPIRED', 'LOCKED')),
    reset_token              TEXT,
    reset_token_expires_at   TIMESTAMPTZ,
    actor_user_id            UUID REFERENCES users(id),
    completed_at             TIMESTAMPTZ
);

-- Backs the per-mobile OTP rate limit (3/hour) and resend cooldown (30s); only
-- self-service rows carry a mobile.
--changeset slice-18:001-password-resets-mobile-index
CREATE INDEX idx_password_resets_mobile ON password_resets (mobile, initiated_at)
    WHERE mobile IS NOT NULL;

-- Backs the complete-step lookup by the post-verify reset token.
--changeset slice-18:001-password-resets-token-index
CREATE INDEX idx_password_resets_reset_token ON password_resets (reset_token)
    WHERE reset_token IS NOT NULL;

-- Backs the audit log's per-User history.
--changeset slice-18:001-password-resets-target-index
CREATE INDEX idx_password_resets_target ON password_resets (target_user_id);
