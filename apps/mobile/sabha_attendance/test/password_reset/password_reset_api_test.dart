import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/password_reset/password_reset_api.dart';

void main() {
  PasswordResetApi apiReturning(http.Response Function(http.Request) handler) {
    return PasswordResetApi(
      baseUrl: 'http://test',
      client: MockClient((req) async => handler(req)),
    );
  }

  group('requestReset', () {
    test('POSTs the username with no auth header and returns the resetId', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(jsonEncode({'resetId': 'r-1'}), 200);
      });

      final resetId = await api.requestReset('ramesh.bhai');

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/password-reset/request');
      expect(captured.headers.containsKey('Authorization'), isFalse);
      expect(jsonDecode(captured.body), {'username': 'ramesh.bhai'});
      expect(resetId, 'r-1');
    });

    test('throws unknown-username on 404', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'detail': 'no user'}), 404));
      await expectLater(
        () => api.requestReset('ghost'),
        throwsA(isA<UnknownUsernameException>()),
      );
    });

    test('throws no-registered-mobile on 422', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'detail': 'no mobile'}), 422));
      await expectLater(
        () => api.requestReset('ramesh.bhai'),
        throwsA(isA<NoRegisteredMobileException>()),
      );
    });

    test('throws rate-limited with the backend message on 429', () async {
      final api = apiReturning(
          (req) => http.Response(jsonEncode({'detail': 'Wait 30s before retrying.'}), 429));
      await expectLater(
        () => api.requestReset('ramesh.bhai'),
        throwsA(isA<ResetRateLimitedException>()
            .having((e) => e.message, 'message', contains('Wait 30s'))),
      );
    });
  });

  group('verify', () {
    test('POSTs the resetId + otp and returns the resetToken', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(jsonEncode({'resetToken': 'tok-1'}), 200);
      });

      final token = await api.verify(resetId: 'r-1', otpCode: '123456');

      expect(captured.url.path, '/api/password-reset/verify');
      expect(jsonDecode(captured.body), {'resetId': 'r-1', 'otpCode': '123456'});
      expect(token, 'tok-1');
    });

    test('throws OTP-rejected with the backend message on 422', () async {
      final api = apiReturning(
          (req) => http.Response(jsonEncode({'detail': 'Wrong OTP - 2 left.'}), 422));
      await expectLater(
        () => api.verify(resetId: 'r-1', otpCode: '000000'),
        throwsA(isA<OtpRejectedException>()
            .having((e) => e.message, 'message', contains('Wrong OTP'))),
      );
    });
  });

  group('complete', () {
    test('POSTs the resetToken + new password', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response('', 200);
      });

      await api.complete(resetToken: 'tok-1', newPassword: 'NewPass123');

      expect(captured.url.path, '/api/password-reset/complete');
      expect(jsonDecode(captured.body), {'resetToken': 'tok-1', 'newPassword': 'NewPass123'});
    });

    test('throws reset-expired on 422', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'detail': 'expired'}), 422));
      await expectLater(
        () => api.complete(resetToken: 'tok-1', newPassword: 'NewPass123'),
        throwsA(isA<ResetExpiredException>()),
      );
    });
  });

  group('whoAppointedMe', () {
    test('GETs the contacts keyed on username with no auth header', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(
          jsonEncode({
            'contacts': [
              {'name': 'Suresh', 'mobile': '+919820000001'}
            ]
          }),
          200,
        );
      });

      final contacts = await api.whoAppointedMe('ramesh.bhai');

      expect(captured.method, 'GET');
      expect(captured.url.path, '/api/who-appointed-me');
      expect(captured.url.queryParameters['username'], 'ramesh.bhai');
      expect(captured.headers.containsKey('Authorization'), isFalse);
      expect(contacts, hasLength(1));
      expect(contacts.first.name, 'Suresh');
      expect(contacts.first.mobile, '+919820000001');
    });

    test('throws unknown-username on 404', () async {
      final api = apiReturning((req) => http.Response('', 404));
      await expectLater(
        () => api.whoAppointedMe('ghost'),
        throwsA(isA<UnknownUsernameException>()),
      );
    });
  });
}
