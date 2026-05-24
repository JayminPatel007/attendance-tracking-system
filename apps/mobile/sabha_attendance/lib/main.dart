import 'package:flutter/material.dart';

import 'auth/auth_config.dart';
import 'auth/auth_service.dart';
import 'auth/login_screen.dart';
import 'auth/session.dart';
import 'roster/roster_api.dart';
import 'roster/roster_screen.dart';

void main() {
  final config = AuthConfig.fromDartDefines();
  runApp(SabhaAttendanceApp(config: config));
}

class SabhaAttendanceApp extends StatelessWidget {
  const SabhaAttendanceApp({super.key, AuthConfig? config, AuthService? authService})
      : _config = config,
        _authService = authService;

  final AuthConfig? _config;
  final AuthService? _authService;

  @override
  Widget build(BuildContext context) {
    final config = _config ?? AuthConfig.fromDartDefines();
    final session = Session();
    final auth = _authService ?? AuthService(config: config);

    return MaterialApp(
      title: 'Sabha Attendance',
      theme: ThemeData(useMaterial3: true),
      home: AppShell(session: session, auth: auth, config: config),
    );
  }
}

class AppShell extends StatelessWidget {
  const AppShell({super.key, required this.session, required this.auth, required this.config});

  final Session session;
  final AuthService auth;
  final AuthConfig config;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sabha Attendance')),
      body: ValueListenableBuilder<String?>(
        valueListenable: session.accessToken,
        builder: (context, token, _) {
          if (token == null) {
            return LoginScreen(session: session, auth: auth);
          }
          return RosterScreen(
            api: RosterApi(baseUrl: config.backendBaseUrl, accessToken: token),
            onSignOut: session.clear,
          );
        },
      ),
    );
  }
}
