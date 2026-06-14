import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_api.dart';
import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_controller.dart';

/// A stand-in backend whose Occurrence state the actions mutate, so the
/// controller's reload-after-action behavior is exercised through the real API.
class _FakeBackend {
  String state = 'SCHEDULED';
  String? venueOverride;
  String? forbid; // path segment to reject with 403
  String? reject; // path segment to reject with 422 + message

  OccurrenceControlApi api() => OccurrenceControlApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async {
          if (req.method == 'GET') {
            return http.Response(
              jsonEncode({
                'id': 'occ-1',
                'sabhaId': 'sabha-1',
                'date': '2026-06-06',
                'state': state,
                'venueOverride': venueOverride,
                'rescheduledDate': null,
                'rescheduledStartTime': null,
                'rescheduledEndTime': null,
              }),
              200,
            );
          }
          final action = req.url.pathSegments.last;
          if (forbid == action) {
            return http.Response(jsonEncode({'detail': 'not allowed'}), 403);
          }
          if (reject == action) {
            return http.Response(jsonEncode({'detail': 'revert window expired'}), 422);
          }
          switch (action) {
            case 'cancel':
              state = 'CANCELLED';
            case 'revert':
              state = 'SCHEDULED';
            case 'reschedule':
              state = 'RESCHEDULED';
            case 'venue-override':
              venueOverride = jsonDecode(req.body)['venue'] as String;
          }
          return http.Response('', 200);
        }),
      );
}

void main() {
  test('initialize loads the current shapeable Occurrence', () async {
    final controller = OccurrenceControlController(api: _FakeBackend().api());
    await controller.initialize();

    expect(controller.state.loading, isFalse);
    expect(controller.state.occurrence!.state, 'SCHEDULED');
  });

  test('cancel reloads so the screen reflects the new CANCELLED state', () async {
    final controller = OccurrenceControlController(api: _FakeBackend().api());
    await controller.initialize();

    await controller.cancel('Festival clash');

    expect(controller.state.occurrence!.state, 'CANCELLED');
    expect(controller.state.error, isNull);
  });

  test('revert moves a Cancelled Occurrence back to Scheduled', () async {
    final backend = _FakeBackend()..state = 'CANCELLED';
    final controller = OccurrenceControlController(api: backend.api());
    await controller.initialize();

    await controller.revert();

    expect(controller.state.occurrence!.state, 'SCHEDULED');
  });

  test('venue override is reflected after the action', () async {
    final controller = OccurrenceControlController(api: _FakeBackend().api());
    await controller.initialize();

    await controller.overrideVenue('Community Hall Annexe');

    expect(controller.state.occurrence!.venueOverride, 'Community Hall Annexe');
  });

  test('a forbidden action surfaces an error without throwing or losing the Occurrence', () async {
    final backend = _FakeBackend()..forbid = 'cancel';
    final controller = OccurrenceControlController(api: backend.api());
    await controller.initialize();

    await controller.cancel('trying anyway');

    expect(controller.state.error, isNotNull);
    expect(controller.state.occurrence!.state, 'SCHEDULED');
  });

  test('a rejected revert surfaces the domain rule message', () async {
    final backend = _FakeBackend()
      ..state = 'CANCELLED'
      ..reject = 'revert';
    final controller = OccurrenceControlController(api: backend.api());
    await controller.initialize();

    await controller.revert();

    expect(controller.state.error, 'revert window expired');
    expect(controller.state.occurrence!.state, 'CANCELLED');
  });
}
