# User Authentication: Custom Username + Password, Set by Assigner

**Status**: substantive policy stands; implementation shape **superseded by [ADR-0016](0016-oidc-auth-via-keycloak.md)** (OIDC via Keycloak). The "assigner sets username + password," "force change on first login," and "mobile-OTP reset with assigner-reissue fallback" rules below all remain in force — only the wire protocol and credential store are now delegated to Keycloak.

Users authenticate with a custom username and password. The assigning Karyakar chooses both at role assignment time and hands them to the new User; the User is forced to change the password on first login. OTP remains in the system for a different purpose entirely — Person-side verification of Home Sabha transfers (ADR-0002) — but is not used for User login.

## Why not mobile + OTP (the obvious alternative)

OTP infrastructure already exists for Verified Home Sabha Transfer, so OTP-based login would have been free to build. We chose password-based login anyway because:
- The user explicitly preferred a username + password flow.
- Many Users (especially senior tier Karyakars on the web app) are doing analytical / administrative work in long sessions where re-OTP every login adds friction.
- A custom username decouples login identity from mobile number, which is helpful when a User changes phones.

## Why the assigner sets both username and password

- Centralizes the act of bringing a new User onboard into a single action by their appointing Karyakar — no separate invitation flow to build.
- The forced first-login password change closes the accountability gap (the assigner doesn't permanently know the User's credentials), so audit logs stay trustworthy.

## Password reset: mobile-OTP self-service, with assigner-reissue as fallback

A User who's forgotten their password clicks "forgot password," receives an OTP at their registered mobile (same channel as ADR-0002's Home Sabha verification), enters it, and sets a new password. Self-service, no Karyakar intervention needed.

If the User has lost their mobile entirely (and updated their Person record's mobile is therefore unreachable), the **assigner-reissue path** is the fallback: the User contacts their original appointing Karyakar, who generates a fresh password in the system (force-change on next login per the policy above). For Sants — who have no appointer per ADR-0011 — Madhyastha Karyalaya members handle the reissue, since they created the User record.

This combines the strengths of both options: OTP infrastructure already exists (no new build); senior tier Karyakars doing long analytical sessions get a low-friction recovery; the assigner path covers the genuinely-lost-mobile edge.

Note: OTP being used for password reset does not change ADR-0004's core stance — *login* is still username + password. OTP only appears at reset time, not on every login.

## Consequences

- Username uniqueness must be enforced at assignment time, with the assigning Karyakar seeing collisions before committing.
- No SSO / external IdP — accepted; can be added later if a partner organization needs it.
- Password reset OTP must be rate-limited per mobile number to prevent abuse (e.g., 3 attempts per hour).
- The "lost mobile" path needs surfaceable contact info: the User must be able to find their assigner's contact details from the login page even when they can't log in. Solve via a static directory page or a "who appointed me?" lookup keyed on username.
