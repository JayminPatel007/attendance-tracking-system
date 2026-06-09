import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/password_reset/password_reset_api.dart';
import 'package:sabha_attendance_mobile/password_reset/password_reset_controller.dart';

/// Overrides the network methods so the controller's state machine can be driven
/// without HTTP. Each test wires only the closures it needs.
class _FakeApi extends PasswordResetApi {
  _FakeApi({this.onRequest, this.onVerify, this.onComplete})
      : super(baseUrl: 'http://test');

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
  PasswordResetController controllerWith(_FakeApi api) => PasswordResetController(api: api);

  test('requesting an OTP for a known username advances to the OTP step', () async {
    final controller = controllerWith(_FakeApi(onRequest: (_) async => 'r-1'));

    await controller.requestOtp('ramesh.bhai');

    expect(controller.state.view, PasswordResetView.otp);
    expect(controller.state.error, isNull);
  });

  test('an unknown username keeps the username step with an error', () async {
    final controller = controllerWith(
        _FakeApi(onRequest: (_) async => throw UnknownUsernameException('no user')));

    await controller.requestOtp('ghost');

    expect(controller.state.view, PasswordResetView.username);
    expect(controller.state.error, isNotNull);
  });

  test('a no-registered-mobile user is pointed at the who-appointed-me lookup', () async {
    final controller = controllerWith(
        _FakeApi(onRequest: (_) async => throw NoRegisteredMobileException('no mobile')));

    await controller.requestOtp('ramesh.bhai');

    expect(controller.state.view, PasswordResetView.username);
    expect(controller.state.error!.toLowerCase(), contains('who appointed me'));
  });

  test('a rate-limited request surfaces the backend message', () async {
    final controller = controllerWith(_FakeApi(
        onRequest: (_) async => throw ResetRateLimitedException('Wait 30s before retrying.')));

    await controller.requestOtp('ramesh.bhai');

    expect(controller.state.view, PasswordResetView.username);
    expect(controller.state.error, contains('Wait 30s'));
  });

  test('a correct OTP advances to the new-password step', () async {
    final controller = controllerWith(_FakeApi(
      onRequest: (_) async => 'r-1',
      onVerify: (_, __) async => 'tok-1',
    ));
    await controller.requestOtp('ramesh.bhai');

    await controller.verify('123456');

    expect(controller.state.view, PasswordResetView.password);
    expect(controller.state.otpError, isNull);
  });

  test('a rejected OTP keeps the OTP step with the backend message', () async {
    final controller = controllerWith(_FakeApi(
      onRequest: (_) async => 'r-1',
      onVerify: (_, __) async => throw OtpRejectedException('Wrong OTP - 2 left.'),
    ));
    await controller.requestOtp('ramesh.bhai');

    await controller.verify('000000');

    expect(controller.state.view, PasswordResetView.otp);
    expect(controller.state.otpError, contains('Wrong OTP'));
  });

  test('completing with a new password finishes the flow', () async {
    String? capturedToken;
    String? capturedPassword;
    final controller = controllerWith(_FakeApi(
      onRequest: (_) async => 'r-1',
      onVerify: (_, __) async => 'tok-1',
      onComplete: (token, password) async {
        capturedToken = token;
        capturedPassword = password;
      },
    ));
    await controller.requestOtp('ramesh.bhai');
    await controller.verify('123456');

    await controller.complete('NewPass123');

    expect(controller.state.view, PasswordResetView.done);
    expect(capturedToken, 'tok-1');
    expect(capturedPassword, 'NewPass123');
  });

  test('an expired reset on complete keeps the password step with an error', () async {
    final controller = controllerWith(_FakeApi(
      onRequest: (_) async => 'r-1',
      onVerify: (_, __) async => 'tok-1',
      onComplete: (_, __) async => throw ResetExpiredException('expired'),
    ));
    await controller.requestOtp('ramesh.bhai');
    await controller.verify('123456');

    await controller.complete('NewPass123');

    expect(controller.state.view, PasswordResetView.password);
    expect(controller.state.error, isNotNull);
  });
}
