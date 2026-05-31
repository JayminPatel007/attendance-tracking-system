import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_api.dart';

void main() {
  HomeSabhaTransferApi apiReturning(http.Response Function(http.Request) handler) {
    return HomeSabhaTransferApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  group('findByMobile', () {
    test('GETs the Directory by mobile and parses the Person', () async {
      final api = apiReturning((req) {
        expect(req.method, 'GET');
        expect(req.url.path, '/api/directory/persons');
        expect(req.url.queryParameters['mobile'], '+919820100200');
        expect(req.headers['Authorization'], 'Bearer tok');
        return http.Response(
          jsonEncode({
            'id': 'p-1',
            'fullName': 'Ravi Patel',
            'gender': 'MALE',
            'mobile': '+919820100200',
          }),
          200,
        );
      });

      final person = await api.findByMobile('+919820100200');

      expect(person, isNotNull);
      expect(person!.id, 'p-1');
      expect(person.fullName, 'Ravi Patel');
      expect(person.mobile, '+919820100200');
    });

    test('returns null when no Person matches the mobile (404)', () async {
      final api = apiReturning((req) => http.Response('', 404));
      expect(await api.findByMobile('+910000000000'), isNull);
    });
  });

  group('initiate', () {
    test('POSTs personId + destinationSabhaId and returns the transferId', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(jsonEncode({'transferId': 't-9'}), 200);
      });

      final id = await api.initiate(personId: 'p-1', destinationSabhaId: 'sabha-2');

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/home-sabha-transfers');
      expect(captured.headers['Authorization'], 'Bearer tok');
      expect(jsonDecode(captured.body), {'personId': 'p-1', 'destinationSabhaId': 'sabha-2'});
      expect(id, 't-9');
    });

    test('throws not-authorized on 403', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'not yours'}), 403));
      await expectLater(
        () => api.initiate(personId: 'p-1', destinationSabhaId: 'sabha-2'),
        throwsA(isA<TransferNotAuthorizedException>()),
      );
    });

    test('throws rate-limited on 429 with the server message', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'Too many OTPs'}), 429));
      await expectLater(
        () => api.initiate(personId: 'p-1', destinationSabhaId: 'sabha-2'),
        throwsA(isA<TransferRateLimitedException>()
            .having((e) => e.message, 'message', 'Too many OTPs')),
      );
    });
  });

  group('confirm', () {
    test('POSTs the otpCode to the transfer confirm endpoint', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response('', 200);
      });

      await api.confirm(transferId: 't-9', otpCode: '123456');

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/home-sabha-transfers/t-9/confirm');
      expect(jsonDecode(captured.body), {'otpCode': '123456'});
    });

    test('throws rejected on 422 carrying the server message', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'Incorrect OTP'}), 422));
      await expectLater(
        () => api.confirm(transferId: 't-9', otpCode: '000000'),
        throwsA(isA<TransferRejectedException>()
            .having((e) => e.message, 'message', 'Incorrect OTP')),
      );
    });
  });
}
