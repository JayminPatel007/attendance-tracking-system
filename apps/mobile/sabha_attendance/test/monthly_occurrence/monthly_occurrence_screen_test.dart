import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_api.dart';
import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_controller.dart';
import 'package:sabha_attendance_mobile/monthly_occurrence/monthly_occurrence_screen.dart';

class _FakeBackend {
  bool firstNeeds = true;
  String? forbid;

  MonthlyOccurrenceApi api() => MonthlyOccurrenceApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async {
          if (req.method == 'GET') {
            return http.Response(
              jsonEncode([
                {'sabhaId': 'sabha-1', 'sabhaKind': 'YSS_YUVAK', 'standingVenue': 'Hall A', 'needsOccurrence': firstNeeds},
                {'sabhaId': 'sabha-2', 'sabhaKind': 'BSS_BAAL', 'standingVenue': 'Hall B', 'needsOccurrence': false},
              ]),
              200,
            );
          }
          if (forbid != null) {
            return http.Response(jsonEncode({'detail': forbid}), 403);
          }
          firstNeeds = false;
          return http.Response(jsonEncode({'occurrenceId': 'occ-new'}), 201);
        }),
      );
}

Future<MonthlyOccurrenceController> _boot(WidgetTester tester, _FakeBackend backend) async {
  final controller = MonthlyOccurrenceController(api: backend.api());
  await tester.runAsync(controller.initialize);
  await tester.pumpWidget(
    MaterialApp(home: Scaffold(body: MonthlyOccurrenceScreen(controller: controller))),
  );
  await tester.pump();
  return controller;
}

void main() {
  testWidgets('nudges only the monthly Sabha missing this month\'s Occurrence', (tester) async {
    await _boot(tester, _FakeBackend());

    expect(find.text('Hall A'), findsOneWidget);
    expect(find.text('Hall B'), findsOneWidget);
    // Exactly one Sabha (Hall A) is past midpoint with no Occurrence.
    expect(find.byKey(const Key('compliance-nudge-sabha-1')), findsOneWidget);
    expect(find.byKey(const Key('compliance-nudge-sabha-2')), findsNothing);
  });

  testWidgets('creating this month\'s Occurrence submits the form and clears the nudge', (tester) async {
    await _boot(tester, _FakeBackend());

    await tester.tap(find.byKey(const Key('add-occurrence-sabha-1')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('occ-date-field')), '2026-06-21');
    await tester.enterText(find.byKey(const Key('occ-start-field')), '18:00');
    await tester.enterText(find.byKey(const Key('occ-end-field')), '19:30');
    await tester.pump();

    await tester.tap(find.byKey(const Key('submit-occurrence-button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('compliance-nudge-sabha-1')), findsNothing);
  });

  testWidgets('a forbidden create surfaces inline and keeps the Sabha listed', (tester) async {
    await _boot(tester, _FakeBackend()..forbid = 'Only the Sanchalak can create this Occurrence.');

    await tester.tap(find.byKey(const Key('add-occurrence-sabha-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('occ-date-field')), '2026-06-21');
    await tester.enterText(find.byKey(const Key('occ-start-field')), '18:00');
    await tester.enterText(find.byKey(const Key('occ-end-field')), '19:30');
    await tester.pump();

    await tester.tap(find.byKey(const Key('submit-occurrence-button')));
    await tester.pumpAndSettle();

    expect(find.text('Only the Sanchalak can create this Occurrence.'), findsOneWidget);
    expect(find.text('YSS_YUVAK'), findsOneWidget);
  });
}
