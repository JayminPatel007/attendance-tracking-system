import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/selection/selection_api.dart';

void main() {
  SelectionApi apiReturning(http.Response Function(http.Request) handler) {
    return SelectionApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  group('nominate', () {
    test('POSTs personId + regularSabhaId and returns the nominationId', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(jsonEncode({'nominationId': 'nom-9'}), 200);
      });

      final id = await api.nominate(personId: 'p-1', regularSabhaId: 'sabha-2');

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/sanchalak/nominations');
      expect(captured.headers['Authorization'], 'Bearer tok');
      expect(jsonDecode(captured.body), {'personId': 'p-1', 'regularSabhaId': 'sabha-2'});
      expect(id, 'nom-9');
    });

    test('throws not-authorized on 403 with the server message', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'not a Sanchalak'}), 403));
      await expectLater(
        () => api.nominate(personId: 'p-1', regularSabhaId: 'sabha-2'),
        throwsA(isA<NominationNotAuthorizedException>()
            .having((e) => e.message, 'message', 'not a Sanchalak')),
      );
    });

    test('throws already-nominated on 409 with the server message', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'already selected'}), 409));
      await expectLater(
        () => api.nominate(personId: 'p-1', regularSabhaId: 'sabha-2'),
        throwsA(isA<AlreadyNominatedException>()
            .having((e) => e.message, 'message', 'already selected')),
      );
    });

    test('throws rejected on 422 carrying the server message', () async {
      final api = apiReturning((req) => http.Response(jsonEncode({'message': 'no selective track'}), 422));
      await expectLater(
        () => api.nominate(personId: 'p-1', regularSabhaId: 'sabha-2'),
        throwsA(isA<NominationRejectedException>()
            .having((e) => e.message, 'message', 'no selective track')),
      );
    });

    test('throws a generic api error on other failures', () async {
      final api = apiReturning((req) => http.Response('boom', 500));
      await expectLater(
        () => api.nominate(personId: 'p-1', regularSabhaId: 'sabha-2'),
        throwsA(isA<SelectionApiException>()),
      );
    });
  });
}
