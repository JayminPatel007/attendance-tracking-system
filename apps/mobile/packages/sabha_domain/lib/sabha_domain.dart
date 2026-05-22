/// sabha_domain — mobile mirror of the backend sabha bounded context.
///
/// Holds Sabha, Occurrence, and Roster types as the mobile app sees them.
/// The mobile permission model is narrow — own Sabha(s) only (ADR-0003) — so
/// this package only exposes the read shapes the Sanchalak / Sah-Sanchalak need.
library sabha_domain;

const String sabhaDomainPlaceholder = 'sabha_domain';
