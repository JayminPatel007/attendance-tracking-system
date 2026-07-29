/**
 * identity-domain — frontend mirror of the backend identity bounded context.
 * The BFF-backed SessionService used by the web shell (Slice 9), the appointment
 * credential helpers, and the shared person-picker every appointing flow
 * composes (Slice 11/12, issue #81). The session and Directory wire shapes are
 * the generated models in `shared-data-access` (issue #131).
 */
export * from './lib/session.service';
export * from './lib/appointment-credentials';
export * from './lib/person-picker.component';
