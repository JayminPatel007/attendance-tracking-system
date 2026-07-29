/**
 * shared-data-access — HTTP infrastructure: typed client wrappers, interceptors,
 * error mapping. Web's outbound adapter to the backend's REST surface.
 *
 * The whole of that surface is generated from the committed OpenAPI document
 * (issue #73) and re-exported here: one API module per controller, one model per
 * wire shape, plus `provideApi` for the app's base-path configuration. It lives
 * in a library rather than under `src/app` because the domain libraries consume
 * it too — `identity-domain`'s session and person picker, `analytics-domain`'s
 * dashboard — and a library cannot reach into the application (issue #131).
 *
 * Regenerate with `npm run generate:api`. The committed spec is itself pinned to
 * the live controllers by the backend's drift gate, so this client can neither
 * lead nor lag the contract.
 */
export * from './lib/generated';
