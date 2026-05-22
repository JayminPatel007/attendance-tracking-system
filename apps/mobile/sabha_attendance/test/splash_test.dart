import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/auth/auth_config.dart';
import 'package:sabha_attendance_mobile/auth/auth_service.dart';
import 'package:sabha_attendance_mobile/main.dart';

class _StubAuthService extends AuthService {
  _StubAuthService(AuthConfig config) : super(config: config);

  @override
  Future<String> login() async => throw UnimplementedError('not used in this test');
}

void main() {
  testWidgets('app shows the Sabha Attendance title and a Sign in entry point when logged out', (tester) async {
    const config = AuthConfig(
      issuerUrl: 'http://localhost:58080/realms/sabha',
      backendBaseUrl: 'http://localhost:8080',
      clientId: 'sabha-mobile',
      redirectUri: 'com.sabha.app:/oauth2redirect',
    );
    await tester.pumpWidget(SabhaAttendanceApp(
      config: config,
      authService: _StubAuthService(config),
    ));

    expect(find.text('Sabha Attendance'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });
}
