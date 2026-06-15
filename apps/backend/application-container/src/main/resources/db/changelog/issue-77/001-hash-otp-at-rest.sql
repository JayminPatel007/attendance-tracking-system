--liquibase formatted sql

-- Issue #77: hash OTP codes at rest. From this deploy the application writes an
-- HMAC-SHA256 digest (keyed by sabha.otp.hash-secret, salted per row by the
-- challenge id) into otp_code instead of the plaintext 6-digit code; OtpChallenge
-- verifies by hashing the candidate and comparing digests.
--
-- In-flight handling (documented choice): rows are at most minutes old (5-minute
-- TTL), so rather than a hash-on-read fallback we retire the pre-deploy plaintext:
--  * PENDING rows are expired, so a stale plaintext code can no longer be
--    confirmed/verified (verify rejects on expiry before any compare) — a legit
--    user simply re-requests an OTP.
--  * every existing otp_code is scrubbed to REDACTED so no plaintext survives the
--    deploy. Terminal rows (CONFIRMED / COMPLETED / VERIFIED / EXPIRED / LOCKED)
--    never re-verify, so losing their code is harmless.

--changeset issue-77:001-expire-inflight-transfers
UPDATE home_sabha_transfers SET status = 'EXPIRED' WHERE status = 'PENDING';

--changeset issue-77:001-redact-transfer-codes
UPDATE home_sabha_transfers SET otp_code = 'REDACTED';

--changeset issue-77:001-expire-inflight-resets
UPDATE password_resets SET status = 'EXPIRED'
    WHERE status = 'PENDING' AND method = 'SELF_SERVICE_OTP';

--changeset issue-77:001-redact-reset-codes
UPDATE password_resets SET otp_code = 'REDACTED' WHERE otp_code IS NOT NULL;
