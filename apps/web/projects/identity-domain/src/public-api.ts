/**
 * identity-domain — frontend mirror of the backend identity bounded context.
 * Session types and the BFF-backed SessionService used by the web shell
 * (Slice 9), plus the Directory search adapter, person DTOs, appointment
 * credential helpers, and the shared person-picker every appointing flow
 * composes (Slice 11/12, issue #81).
 */
export * from './lib/section';
export * from './lib/web-session';
export * from './lib/session.service';
export * from './lib/directory.types';
export * from './lib/directory.service';
export * from './lib/appointment-credentials';
export * from './lib/person-picker.component';
