--liquibase formatted sql

-- Slice 19 — Audit log surface (ADR-0023). No schema change: the viewer is a
-- read-model (a UNION) over audit columns earlier slices already populate. This
-- seed only adds the audit-bearing rows the viewer reads, since no earlier seed
-- writes any (no transitions, appointments, or created_by structural rows ship).
--
-- It reuses the Slice-16 Nirdeshak (user 00..0061, Keycloak subject 00..0062,
-- scoped to (Kshetra Tracer, YUVAK)) rather than minting a duplicate. It adds:
-- the appointed_by/at audit on that Nirdeshak's role row, so it surfaces as a
-- ROLE_ASSIGNMENT entry in Kshetra Tracer; two Occurrence transitions on the
-- Slice-14 proxy occurrence (00..00b6, in Kshetra Tracer), one direct and one
-- carrying an on_behalf_of attribution (the proxy filter target); and a
-- State-level Sabha-kind creation whose geography is NULL, so a scoped caller
-- must not see it but the State-wide MK must. The transitions land on 00..00b6
-- (not the reopen tracer occurrences 00..0021/0022), leaving the reopen test's
-- "never reopened" rows untouched.

--changeset slice-19:001-nirdeshak-appointment-audit
UPDATE role_assignments
   SET appointed_by = '00000000-0000-0000-0000-000000000051',
       appointed_at = TIMESTAMPTZ '2026-05-01 10:00:00+00'
 WHERE id = '00000000-0000-0000-0000-000000000064';

--changeset slice-19:001-occurrence-transitions
INSERT INTO occurrence_state_transitions
    (id, occurrence_id, from_state, to_state, action, actor_kind, actor_user_id, reason, at_timestamp, on_behalf_of_user_id) VALUES
    ('00000000-0000-0000-0000-0000000000d5',
     '00000000-0000-0000-0000-0000000000b6',
     'SCHEDULED', 'OPEN_FOR_MARKING', 'OPEN', 'USER',
     '00000000-0000-0000-0000-000000000051',
     'Audit seed — direct action',
     TIMESTAMPTZ '2026-05-02 09:00:00+00',
     NULL),
    ('00000000-0000-0000-0000-0000000000d6',
     '00000000-0000-0000-0000-0000000000b6',
     'OPEN_FOR_MARKING', 'CANCELLED', 'CANCEL', 'USER',
     '00000000-0000-0000-0000-000000000051',
     'Audit seed — proxy action',
     TIMESTAMPTZ '2026-05-03 09:00:00+00',
     '00000000-0000-0000-0000-0000000000b2');

-- A state-level act (no geography): BAAL/BSS is a valid domain kind unused by any
-- integration test, so this seed never collides with the sabha_kinds UNIQUE the
-- StructuralCreation / SabhaDefinition tests rely on.
--changeset slice-19:001-state-level-sabha-kind
INSERT INTO sabha_kinds (id, demographic, track, created_by, created_at) VALUES
    ('00000000-0000-0000-0000-0000000000d7',
     'BAAL', 'BSS',
     '00000000-0000-0000-0000-000000000051',
     TIMESTAMPTZ '2026-05-04 08:00:00+00');
