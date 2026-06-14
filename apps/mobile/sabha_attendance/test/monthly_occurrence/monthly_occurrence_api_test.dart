import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_api.dart';

void main() {
  MonthlyOccurrenceApi apiReturning(http.Response Function(http.Request) handler) {
    return MonthlyOccurrenceApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  test('fetchSabhas returns the caller\'s monthly Sabhas with their nudge flag', () async {
    final api = apiReturning((req) {
      expect(req.method, 'GET');
      expect(req.url.path, '/api/sanchalak/monthly-sabhas');
      expect(req.headers['Authorization'], 'Bearer tok');
      return http.Response(
        jsonEncode([
          {'sabhaId': 'sabha-1', 'sabhaKind': 'YSS_YUVAK', 'standingVenue': 'Hall A', 'needsOccurrence': true},
          {'sabhaId': 'sabha-2', 'sabhaKind': 'BSS_BAAL', 'standingVenue': 'Hall B', 'needsOccurrence': false},
        ]),
        200,
      );
    });

    final sabhas = await api.fetchSabhas();

    expect(sabhas, hasLength(2));
    expect(sabhas.first.sabhaId, 'sabha-1');
    expect(sabhas.first.sabhaKind, 'YSS_YUVAK');
    expect(sabhas.first.standingVenue, 'Hall A');
    expect(sabhas.first.needsOccurrence, isTrue);
    expect(sabhas[1].needsOccurrence, isFalse);
  });

  test('create POSTs the picked date/time/venue and returns the new occurrence id', () async {
    late http.Request captured;
    final api = apiReturning((req) {
      captured = req;
      return http.Response(jsonEncode({'occurrenceId': 'occ-9'}), 201);
    });

    final id = await api.create(
      'sabha-1',
      date: '2026-06-21',
      startTime: '18:00',
      endTime: '19:30',
      venue: 'Community Hall',
    );

    expect(id, 'occ-9');
    expect(captured.method, 'POST');
    expect(captured.url.path, '/api/sabhas/sabha-1/occurrences');
    expect(jsonDecode(captured.body), {
      'date': '2026-06-21',
      'startTime': '18:00',
      'endTime': '19:30',
      'venue': 'Community Hall',
    });
  });

  test('a 403 surfaces as MonthlyOccurrenceForbiddenException', () async {
    final api = apiReturning((req) => http.Response(
          jsonEncode({'detail': 'only the Sanchalak can create this'}),
          403,
        ));

    await expectLater(
      () => api.create('sabha-1', date: '2026-06-21', startTime: '18:00', endTime: '19:30', venue: 'X'),
      throwsA(isA<MonthlyOccurrenceForbiddenException>()),
    );
  });

  test('a 422 surfaces the domain rule message (e.g. a weekly Sabha rejects manual create)', () async {
    final api = apiReturning((req) => http.Response(
          jsonEncode({'detail': 'weekly Sabhas materialize automatically'}),
          422,
        ));

    await expectLater(
      () => api.create('sabha-1', date: '2026-06-21', startTime: '18:00', endTime: '19:30', venue: 'X'),
      throwsA(isA<MonthlyOccurrenceRuleException>()
          .having((e) => e.message, 'message', 'weekly Sabhas materialize automatically')),
    );
  });
}
