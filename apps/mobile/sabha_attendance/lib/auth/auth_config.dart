/// Wire-level configuration for the OIDC flow + backend.
///
/// All values are overridable via --dart-define at build time. Defaults match
/// `docker-compose up` running on the same host as the simulator.
///
/// Defaults match the `sabha-mobile` client and the realm in
/// `infra/keycloak/realm-sabha.json`. For Android emulators, replace
/// `localhost` with `10.0.2.2` via --dart-define.
class AuthConfig {
  const AuthConfig({
    required this.issuerUrl,
    required this.backendBaseUrl,
    required this.clientId,
    required this.redirectUri,
  });

  final String issuerUrl;
  final String backendBaseUrl;
  final String clientId;
  final String redirectUri;

  factory AuthConfig.fromDartDefines() {
    return const AuthConfig(
      issuerUrl: String.fromEnvironment(
        'OIDC_ISSUER',
        defaultValue: 'http://localhost:58080/realms/sabha',
      ),
      backendBaseUrl: String.fromEnvironment(
        'BACKEND_BASE_URL',
        defaultValue: 'http://localhost:8080',
      ),
      clientId: String.fromEnvironment(
        'OIDC_CLIENT_ID',
        defaultValue: 'sabha-mobile',
      ),
      redirectUri: String.fromEnvironment(
        'OIDC_REDIRECT_URI',
        defaultValue: 'com.sabha.app:/oauth2redirect',
      ),
    );
  }
}
