import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/add_person/add_person_api.dart';
import 'package:sabha_attendance_mobile/add_person/add_person_controller.dart';
import 'package:sabha_attendance_mobile/add_person/add_person_screen.dart';

/// Backs the real API with a scripted Directory: one mobile already taken, and
/// a name that trips the soft-warn until the override flag is set.
class _FakeBackend {
  String takenMobile = '+919820111122';
  String softWarnName = 'Jay Mehta';

  AddPersonApi api() => AddPersonApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async {
          if (req.method == 'GET') {
            final mobile = req.url.queryParameters['mobile'];
            if (mobile == takenMobile) {
              return http.Response(
                jsonEncode({
                  'id': 'person-9',
                  'fullName': 'Ravi Patel',
                  'gender': 'MALE',
                  'dateOfBirth': null,
                  'mobile': takenMobile,
                  'guardianPersonId': null,
                }),
                200,
              );
            }
            return http.Response('', 404);
          }
          // POST add
          final body = jsonDecode(req.body) as Map<String, dynamic>;
          final override = body['overrideDuplicateWarning'] as bool? ?? false;
          if (body['fullName'] == softWarnName && !override) {
            return http.Response(
              jsonEncode({
                'personId': null,
                'requiresOverride': true,
                'candidates': [
                  {'personId': 'cand-1', 'fullName': 'Jai Mehta', 'homeSabhaName': 'REGULAR_YUVAK'},
                ],
              }),
              200,
            );
          }
          return http.Response(
            jsonEncode({'personId': 'new-1', 'candidates': <dynamic>[], 'requiresOverride': false}),
            201,
          );
        }),
      );
}

Future<({AddPersonController controller, List<String> selected, List<String> created})> _boot(
    WidgetTester tester, _FakeBackend backend) async {
  final controller = AddPersonController(api: backend.api(), homeSabhaId: 'sabha-1');
  final selected = <String>[];
  final created = <String>[];
  await tester.pumpWidget(MaterialApp(
    home: Scaffold(
      body: AddPersonScreen(
        controller: controller,
        homeSabhaLabel: 'Yuvak Sabha · Kshetra Tracer',
        onSelectExisting: selected.add,
        onCreated: created.add,
      ),
    ),
  ));
  await tester.pump();
  return (controller: controller, selected: selected, created: created);
}

void main() {
  testWidgets('an existing mobile forces a redirect to that Person and Use selects them', (tester) async {
    final ctx = await _boot(tester, _FakeBackend());

    await tester.enterText(find.byKey(const Key('add-person-mobile-field')), '+919820111122');
    await tester.pump();
    await tester.tap(find.byKey(const Key('check-mobile-button')));
    await tester.pumpAndSettle();

    expect(find.text('Ravi Patel'), findsOneWidget);
    expect(find.byKey(const Key('add-to-directory-button')), findsNothing);

    await tester.tap(find.byKey(const Key('use-existing-button')));
    await tester.pump();

    expect(ctx.selected, ['person-9']);
  });

  testWidgets('a new mobile advances to details and a clean add creates the Person', (tester) async {
    final ctx = await _boot(tester, _FakeBackend());

    await tester.enterText(find.byKey(const Key('add-person-mobile-field')), '+919999000111');
    await tester.pump();
    await tester.tap(find.byKey(const Key('check-mobile-button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('add-person-name-field')), findsOneWidget);
    await tester.enterText(find.byKey(const Key('add-person-name-field')), 'Brand New');
    await tester.tap(find.byKey(const Key('gender-male')));
    await tester.pump();

    await tester.tap(find.byKey(const Key('add-to-directory-button')));
    await tester.pumpAndSettle();

    expect(ctx.created, ['new-1']);
  });

  testWidgets('a close name surfaces candidates and create-new-anyway overrides them', (tester) async {
    final ctx = await _boot(tester, _FakeBackend());

    await tester.enterText(find.byKey(const Key('add-person-mobile-field')), '+919999000222');
    await tester.pump();
    await tester.tap(find.byKey(const Key('check-mobile-button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('add-person-name-field')), 'Jay Mehta');
    await tester.tap(find.byKey(const Key('gender-male')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('add-to-directory-button')));
    await tester.pumpAndSettle();

    // Candidate surfaced, nothing created yet.
    expect(find.text('Jai Mehta'), findsOneWidget);
    expect(ctx.created, isEmpty);

    await tester.tap(find.byKey(const Key('create-new-anyway-button')));
    await tester.pumpAndSettle();

    expect(ctx.created, ['new-1']);
  });
}
