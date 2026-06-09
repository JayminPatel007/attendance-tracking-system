import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/password_reset/password_reset_api.dart';
import 'package:sabha_attendance_mobile/password_reset/password_reset_controller.dart';
import 'package:sabha_attendance_mobile/password_reset/password_reset_screen.dart';

class _FakeApi extends PasswordResetApi {
  _FakeApi({this.onRequest, this.onVerify, this.onComplete}) : super(baseUrl: 'http://test');

  Future<String> Function(String username)? onRequest;
  Future<String> Function(String resetId, String otpCode)? onVerify;
  Future<void> Function(String resetToken, String newPassword)? onComplete;

  @override
  Future<String> requestReset(String username) => onRequest!(username);

  @override
  Future<String> verify({required String resetId, required String otpCode}) =>
      onVerify!(resetId, otpCode);

  @override
  Future<void> complete({required String resetToken, required String newPassword}) =>
      onComplete!(resetToken, newPassword);
}

void main() {
  Future<void> pump(WidgetTester tester, PasswordResetController controller) {
    return tester.pumpWidget(MaterialApp(
      home: Scaffold(body: PasswordResetScreen(controller: controller)),
    ));
  }

  testWidgets('full flow: username → OTP → new password → done', (tester) async {
    final controller = PasswordResetController(
      api: _FakeApi(
        onRequest: (_) async => 'r-1',
        onVerify: (_, __) async => 'tok-1',
        onComplete: (_, __) async {},
      ),
    );
    await pump(tester, controller);

    await tester.enterText(find.byKey(const Key('reset-username-field')), 'ramesh.bhai');
    await tester.pump();
    await tester.tap(find.byKey(const Key('reset-send-button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('reset-otp-field')), '123456');
    await tester.pump();
    await tester.tap(find.byKey(const Key('reset-verify-button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('reset-password-field')), 'NewPass123');
    await tester.pump();
    await tester.tap(find.byKey(const Key('reset-complete-button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('reset-done')), findsOneWidget);
  });

  testWidgets('shows the backend error when the username is unknown', (tester) async {
    final controller = PasswordResetController(
      api: _FakeApi(onRequest: (_) async => throw UnknownUsernameException('no user')),
    );
    await pump(tester, controller);

    await tester.enterText(find.byKey(const Key('reset-username-field')), 'ghost');
    await tester.pump();
    await tester.tap(find.byKey(const Key('reset-send-button')));
    await tester.pumpAndSettle();

    expect(find.textContaining("couldn't find", findRichText: true), findsOneWidget);
    expect(find.byKey(const Key('reset-otp-field')), findsNothing);
  });
}
