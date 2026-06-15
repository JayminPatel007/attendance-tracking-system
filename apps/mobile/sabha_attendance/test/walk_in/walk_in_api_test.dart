import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/walk_in/walk_in_api.dart';

void main() {
  WalkInApi apiReturning(http.Response Function(http.Request) handler) {
    return WalkInApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  group('search', () {
    test('GETs walk-in-search with the sabha + query and parses candidates', () async {
      final api = apiReturning((req) {
        expect(req.method, 'GET');
        expect(req.url.path, '/api/directory/walk-in-search');
        expect(req.url.queryParameters['sabhaId'], 'sabha-1');
        expect(req.url.queryParameters['q'], 'Ramesh');
        expect(req.headers['Authorization'], 'Bearer tok');
        return http.Response(
          jsonEncode([
            {
              'personId': 'p-110',
              'fullName': 'Ramesh Shah',
              'homeSabhas': ['REGULAR_BAAL', 'REGULAR_SANYUKTA'],
            },
          ]),
          200,
        );
      });

      final results = await api.search(sabhaId: 'sabha-1', query: 'Ramesh');

      expect(results, hasLength(1));
      expect(results.single.personId, 'p-110');
      expect(results.single.fullName, 'Ramesh Shah');
      expect(results.single.homeSabhas, ['REGULAR_BAAL', 'REGULAR_SANYUKTA']);
    });

    test('parses a candidate with no Home Sabha as an empty list', () async {
      final api = apiReturning((req) => http.Response(
            jsonEncode([
              {'personId': 'p-111', 'fullName': 'No Home', 'homeSabhas': <String>[]},
            ]),
            200,
          ));

      final results = await api.search(sabhaId: 'sabha-1', query: 'No Home');

      expect(results.single.homeSabhas, isEmpty);
    });

    test('returns an empty list when the Directory has no match', () async {
      final api = apiReturning((req) => http.Response('[]', 200));
      expect(await api.search(sabhaId: 'sabha-1', query: 'Nobody'), isEmpty);
    });

    test('throws on a non-200', () async {
      final api = apiReturning((req) => http.Response('boom', 500));
      await expectLater(
        () => api.search(sabhaId: 'sabha-1', query: 'Ramesh'),
        throwsA(isA<WalkInApiException>()),
      );
    });
  });

  group('recordWalkIn', () {
    test('POSTs the personId to the occurrence walk-ins endpoint', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response('', 200);
      });

      await api.recordWalkIn(occurrenceId: 'occ-1', personId: 'p-110');

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/occurrences/occ-1/walk-ins');
      expect((jsonDecode(captured.body) as Map)['personId'], 'p-110');
    });

    test('throws on a non-200', () async {
      final api = apiReturning((req) => http.Response('nope', 409));
      await expectLater(
        () => api.recordWalkIn(occurrenceId: 'occ-1', personId: 'p-110'),
        throwsA(isA<WalkInApiException>()),
      );
    });
  });
}
