import 'package:flutter_appauth/flutter_appauth.dart';

import 'auth_config.dart';

/// Wraps `flutter_appauth` so the rest of the app stays unaware of the package.
/// Returns the access token on success; throws on failure. Keycloak's hosted
/// flow handles the UPDATE_PASSWORD required-action transparently before
/// returning the auth code to us (ADR-0016).
class AuthService {
  AuthService({required this.config, FlutterAppAuth? appAuth})
      : _appAuth = appAuth ?? const FlutterAppAuth();

  final AuthConfig config;
  final FlutterAppAuth _appAuth;

  Future<String> login() async {
    final result = await _appAuth.authorizeAndExchangeCode(
      AuthorizationTokenRequest(
        config.clientId,
        config.redirectUri,
        discoveryUrl: '${config.issuerUrl}/.well-known/openid-configuration',
        scopes: const ['openid', 'profile', 'email'],
      ),
    );
    final token = result.accessToken;
    if (token == null) {
      throw StateError('OIDC flow returned no access token');
    }
    return token;
  }
}
