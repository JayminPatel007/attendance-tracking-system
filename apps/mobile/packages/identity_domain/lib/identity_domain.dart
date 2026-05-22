/// identity_domain — mobile mirror of the backend identity bounded context.
///
/// Holds User, Session, and credential-related domain types used by the mobile
/// login flow (lands in Slice 2). Pure Dart; the HTTP adapter lives in a future
/// identity_data package.
library identity_domain;

const String identityDomainPlaceholder = 'identity_domain';
