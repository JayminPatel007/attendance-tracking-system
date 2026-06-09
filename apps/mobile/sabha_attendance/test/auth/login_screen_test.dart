import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/auth/auth_config.dart';
import 'package:sabha_attendance_mobile/auth/auth_service.dart';
import 'package:sabha_attendance_mobile/auth/login_screen.dart';
import 'package:sabha_attendance_mobile/auth/session.dart';

void main() {
  LoginScreen loginScreen() => LoginScreen(
        session: Session(),
        auth: AuthService(
          config: const AuthConfig(
            issuerUrl: 'http://test/realms/sabha',
            backendBaseUrl: 'http://test',
            clientId: 'sabha-mobile',
            redirectUri: 'com.sabha.app:/oauth2redirect',
          ),
        ),
        backendBaseUrl: 'http://test',
      );

  testWidgets('"Forgot password?" opens the self-service reset flow', (tester) async {
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: loginScreen())));

    await tester.tap(find.byKey(const Key('login-forgot-password')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('reset-username-field')), findsOneWidget);
  });

  testWidgets('"Who appointed me?" opens the lookup screen', (tester) async {
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: loginScreen())));

    await tester.tap(find.byKey(const Key('login-who-appointed-me')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('wam-username-field')), findsOneWidget);
  });
}
