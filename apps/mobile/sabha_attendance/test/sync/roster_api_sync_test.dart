import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/sync/pending_marking.dart';

void main() {
  group('RosterApi.sync', () {
    test('posts the queue under {rosterVersion, markings:[...]} and returns appliedCount on 200', () async {
      Map<String, dynamic>? captured;
      final client = MockClient((req) async {
        if (req.method == 'POST' && req.url.path.endsWith('/api/sync')) {
          captured = jsonDecode(req.body) as Map<String, dynamic>;
          return http.Response(jsonEncode({'appliedCount': 2}), 200);
        }
        return http.Response('not found', 404);
      });
      final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: client);

      final applied = await api.sync(
        rosterVersion: DateTime.utc(2026, 5, 27, 10),
        markings: [
          PendingMarking(
            occurrenceId: 'occ-1', personId: 'p1', present: true,
            clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 0)),
          PendingMarking(
            occurrenceId: 'occ-1', personId: 'p2', present: false,
            clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 1)),
        ],
      );

      expect(applied, 2);
      expect(captured!['rosterVersion'], '2026-05-27T10:00:00.000Z');
      expect((captured!['markings'] as List), hasLength(2));
      expect((captured!['markings'] as List).first, {
        'occurrenceId': 'occ-1',
        'personId': 'p1',
        'present': true,
        'clientMarkedAt': '2026-05-27T19:00:00.000Z',
      });
    });

    test('throws StaleRosterException when the backend responds 409 with code ROSTER_STALE', () async {
      final client = MockClient((req) async => http.Response(
            jsonEncode({'code': 'ROSTER_STALE', 'status': 409, 'detail': 'stale'}),
            409,
            headers: {'content-type': 'application/json'},
          ));
      final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: client);

      expect(
        () => api.sync(rosterVersion: DateTime.utc(2026, 5, 1), markings: []),
        throwsA(isA<StaleRosterException>()),
      );
    });

    test('non-stale 4xx surfaces as a generic RosterApiException so callers can retry', () async {
      final client = MockClient((req) async => http.Response('boom', 422));
      final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: client);

      expect(
        () => api.sync(rosterVersion: DateTime.utc(2026, 5, 27), markings: []),
        throwsA(isA<RosterApiException>()),
      );
    });
  });
}
