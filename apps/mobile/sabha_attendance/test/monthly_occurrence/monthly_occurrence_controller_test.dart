import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_api.dart';
import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_controller.dart';

/// A stand-in backend: creating an Occurrence flips the Sabha's nudge flag off,
/// so the controller's reload-after-create behavior is exercised end to end.
class _FakeBackend {
  bool needsOccurrence = true;
  String? forbid; // set to reject the create with 403
  String? reject; // set to a message to reject the create with 422

  MonthlyOccurrenceApi api() => MonthlyOccurrenceApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async {
          if (req.method == 'GET') {
            return http.Response(
              jsonEncode([
                {
                  'sabhaId': 'sabha-1',
                  'sabhaKind': 'YSS_YUVAK',
                  'standingVenue': 'Hall A',
                  'needsOccurrence': needsOccurrence,
                },
              ]),
              200,
            );
          }
          if (forbid != null) {
            return http.Response(jsonEncode({'message': forbid}), 403);
          }
          if (reject != null) {
            return http.Response(jsonEncode({'message': reject}), 422);
          }
          needsOccurrence = false;
          return http.Response(jsonEncode({'occurrenceId': 'occ-new'}), 201);
        }),
      );
}

Future<MonthlyOccurrenceController> _created(_FakeBackend backend) async {
  final controller = MonthlyOccurrenceController(api: backend.api());
  await controller.initialize();
  return controller;
}

void main() {
  test('initialize loads the caller\'s monthly Sabhas', () async {
    final controller = await _created(_FakeBackend());

    expect(controller.state.loading, isFalse);
    expect(controller.state.sabhas, hasLength(1));
    expect(controller.state.sabhas.first.needsOccurrence, isTrue);
  });

  test('create reloads so the nudged Sabha no longer needs an Occurrence', () async {
    final controller = await _created(_FakeBackend());

    await controller.create('sabha-1', date: '2026-06-21', startTime: '18:00', endTime: '19:30', venue: 'Hall A');

    expect(controller.state.createdOccurrenceId, 'occ-new');
    expect(controller.state.sabhas.first.needsOccurrence, isFalse);
    expect(controller.state.error, isNull);
  });

  test('a forbidden create surfaces an error without losing the Sabha list', () async {
    final controller = await _created(_FakeBackend()..forbid = 'only the Sanchalak can create this');

    await controller.create('sabha-1', date: '2026-06-21', startTime: '18:00', endTime: '19:30', venue: 'Hall A');

    expect(controller.state.error, 'only the Sanchalak can create this');
    expect(controller.state.sabhas, hasLength(1));
    expect(controller.state.createdOccurrenceId, isNull);
  });

  test('a rejected create surfaces the domain rule message', () async {
    final controller = await _created(_FakeBackend()..reject = 'weekly Sabhas materialize automatically');

    await controller.create('sabha-1', date: '2026-06-21', startTime: '18:00', endTime: '19:30', venue: 'Hall A');

    expect(controller.state.error, 'weekly Sabhas materialize automatically');
  });
}
