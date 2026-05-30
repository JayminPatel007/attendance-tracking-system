import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_api.dart';

void main() {
  OccurrenceControlApi apiReturning(http.Response Function(http.Request) handler) {
    return OccurrenceControlApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  test('fetch returns the current shapeable Occurrence with its shaped state', () async {
    final api = apiReturning((req) {
      expect(req.method, 'GET');
      expect(req.url.path, '/api/sanchalak/current-occurrence');
      expect(req.headers['Authorization'], 'Bearer tok');
      return http.Response(
        jsonEncode({
          'id': 'occ-1',
          'sabhaId': 'sabha-1',
          'date': '2026-06-06',
          'state': 'SCHEDULED',
          'venueOverride': null,
          'rescheduledDate': null,
          'rescheduledStartTime': null,
          'rescheduledEndTime': null,
        }),
        200,
      );
    });

    final occ = await api.fetch();

    expect(occ, isNotNull);
    expect(occ!.id, 'occ-1');
    expect(occ.state, 'SCHEDULED');
    expect(occ.venueOverride, isNull);
  });

  test('fetch returns null when there is no shapeable Occurrence (404)', () async {
    final api = apiReturning((req) => http.Response('', 404));
    expect(await api.fetch(), isNull);
  });

  test('cancel POSTs the reason to the cancel endpoint', () async {
    late http.Request captured;
    final api = apiReturning((req) {
      captured = req;
      return http.Response('', 200);
    });

    await api.cancel('occ-1', 'Festival clash');

    expect(captured.method, 'POST');
    expect(captured.url.path, '/api/occurrences/occ-1/cancel');
    expect(jsonDecode(captured.body), {'reason': 'Festival clash'});
  });

  test('revert POSTs to the revert endpoint with no body', () async {
    late http.Request captured;
    final api = apiReturning((req) {
      captured = req;
      return http.Response('', 200);
    });

    await api.revert('occ-1');

    expect(captured.method, 'POST');
    expect(captured.url.path, '/api/occurrences/occ-1/revert');
  });

  test('reschedule POSTs the new date/time window', () async {
    late http.Request captured;
    final api = apiReturning((req) {
      captured = req;
      return http.Response('', 200);
    });

    await api.reschedule('occ-1', date: '2026-06-20', startTime: '18:00', endTime: '19:30');

    expect(captured.url.path, '/api/occurrences/occ-1/reschedule');
    expect(jsonDecode(captured.body), {
      'date': '2026-06-20',
      'startTime': '18:00',
      'endTime': '19:30',
    });
  });

  test('overrideVenue POSTs the venue to the venue-override endpoint', () async {
    late http.Request captured;
    final api = apiReturning((req) {
      captured = req;
      return http.Response('', 200);
    });

    await api.overrideVenue('occ-1', 'Community Hall Annexe');

    expect(captured.url.path, '/api/occurrences/occ-1/venue-override');
    expect(jsonDecode(captured.body), {'venue': 'Community Hall Annexe'});
  });

  test('a 403 surfaces as OccurrenceForbiddenException (Sah-Sanchalak excluded)', () async {
    final api = apiReturning((req) => http.Response(
          jsonEncode({'status': 403, 'error': 'Forbidden', 'message': 'not allowed to shape this Sabha'}),
          403,
        ));

    expect(
      () => api.cancel('occ-1', 'trying anyway'),
      throwsA(isA<OccurrenceForbiddenException>()),
    );
  });

  test('a 422 surfaces the domain rule message via OccurrenceRuleException', () async {
    final api = apiReturning((req) => http.Response(
          jsonEncode({'status': 422, 'error': 'Unprocessable Entity', 'message': 'revert window expired'}),
          422,
        ));

    await expectLater(
      () => api.revert('occ-1'),
      throwsA(
        isA<OccurrenceRuleException>().having((e) => e.message, 'message', 'revert window expired'),
      ),
    );
  });
}
