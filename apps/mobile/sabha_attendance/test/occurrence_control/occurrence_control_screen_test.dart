import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_api.dart';
import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_controller.dart';
import 'package:sabha_attendance_mobile/occurrence_control/occurrence_control_screen.dart';

class _FakeBackend {
  String state = 'SCHEDULED';
  String? venueOverride;
  String? forbid;

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
            return http.Response(jsonEncode({'message': 'Only the Sanchalak can shape this Sabha.'}), 403);
          }
          switch (action) {
            case 'cancel':
              state = 'CANCELLED';
            case 'revert':
              state = 'SCHEDULED';
            case 'venue-override':
              venueOverride = jsonDecode(req.body)['venue'] as String;
          }
          return http.Response('', 200);
        }),
      );
}

Future<OccurrenceControlController> _boot(WidgetTester tester, _FakeBackend backend) async {
  final controller = OccurrenceControlController(api: backend.api());
  await tester.runAsync(controller.initialize);
  await tester.pumpWidget(
    MaterialApp(home: Scaffold(body: OccurrenceControlScreen(controller: controller))),
  );
  await tester.pump();
  return controller;
}

void main() {
  testWidgets('a Scheduled Occurrence shows the three Sabha-shaping cards', (tester) async {
    await _boot(tester, _FakeBackend());

    expect(find.text('Reschedule'), findsOneWidget);
    expect(find.text('Venue override'), findsOneWidget);
    expect(find.text('Cancel Sabha'), findsOneWidget);
    expect(find.text('Revert'), findsNothing);
  });

  testWidgets('cancelling with a reason flips to Cancelled and reveals Revert', (tester) async {
    await _boot(tester, _FakeBackend());

    await tester.tap(find.text('Cancel Sabha'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('cancel-reason-field')), 'Festival clash');
    await tester.pump();

    await tester.tap(find.byKey(const Key('confirm-cancel-button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('revert-button')), findsOneWidget);
    expect(find.text('Cancel Sabha'), findsNothing);
  });

  testWidgets('an already-Cancelled Occurrence offers Revert and hides the shaping cards', (tester) async {
    await _boot(tester, _FakeBackend()..state = 'CANCELLED');

    expect(find.byKey(const Key('revert-button')), findsOneWidget);
    expect(find.text('Cancel Sabha'), findsNothing);
    expect(find.text('Reschedule'), findsNothing);
  });

  testWidgets('a Rescheduled Occurrence only offers venue override (reschedule/cancel need Scheduled)', (tester) async {
    await _boot(tester, _FakeBackend()..state = 'RESCHEDULED');

    expect(find.text('Venue override'), findsOneWidget);
    expect(find.text('Reschedule'), findsNothing);
    expect(find.text('Cancel Sabha'), findsNothing);
    expect(find.byKey(const Key('revert-button')), findsNothing);
  });

  testWidgets('a Sah-Sanchalak 403 surfaces inline and leaves the cards intact', (tester) async {
    await _boot(tester, _FakeBackend()..forbid = 'cancel');

    await tester.tap(find.text('Cancel Sabha'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('cancel-reason-field')), 'trying anyway');
    await tester.pump();

    await tester.tap(find.byKey(const Key('confirm-cancel-button')));
    await tester.pumpAndSettle();

    expect(find.text('Only the Sanchalak can shape this Sabha.'), findsOneWidget);
    expect(find.text('Cancel Sabha'), findsOneWidget);
  });

  testWidgets('a venue override is reflected in the header after applying', (tester) async {
    await _boot(tester, _FakeBackend());

    await tester.tap(find.text('Venue override'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('venue-field')), 'Community Hall Annexe');
    await tester.pump();

    await tester.tap(find.byKey(const Key('apply-venue-button')));
    await tester.pumpAndSettle();

    expect(find.text('Venue: Community Hall Annexe'), findsOneWidget);
  });
}
