/**
 * analytics-domain — frontend mirror of the backend analytics bounded context.
 *
 * Empty again (issue #131): the re-engagement dashboard's read-model types and
 * its HTTP adapter were a hand-written restatement of the contract, and are now
 * the generated models and API module in `shared-data-access`. The library stays
 * scaffolded per ADR-0014 for the analytics behaviour a later slice gives it —
 * something the wire shape does not already say.
 */
export const __ANALYTICS_DOMAIN__ = 'analytics-domain';
