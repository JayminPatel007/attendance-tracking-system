import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/roster/roster_screen.dart';

void main() {
  testWidgets('roster screen renders the people returned by the backend', (tester) async {
    final client = MockClient((req) async {
      if (req.url.path.endsWith('/api/sanchalak/current-roster')) {
        return http.Response(
          jsonEncode({
            'occurrence': {
              'id': '00000000-0000-0000-0000-000000000020',
              'date': '2026-05-22',
              'state': 'OPEN_FOR_MARKING',
              'sabhaId': '00000000-0000-0000-0000-000000000002',
            },
            'roster': [
              {'personId': 'p1', 'fullName': 'Alice', 'present': null},
              {'personId': 'p2', 'fullName': 'Bob', 'present': true},
            ],
          }),
          200,
        );
      }
      return http.Response('not found', 404);
    });
    final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: client);

    await tester.pumpWidget(MaterialApp(home: Scaffold(body: RosterScreen(api: api))));
    await tester.pumpAndSettle();

    expect(find.text('Alice'), findsOneWidget);
    expect(find.text('Bob'), findsOneWidget);
  });

  testWidgets('tapping a person sends a mark request with the toggled value', (tester) async {
    final List<Map<String, dynamic>> postedBodies = [];
    int fetchCount = 0;

    final client = MockClient((req) async {
      if (req.method == 'GET' && req.url.path.endsWith('/api/sanchalak/current-roster')) {
        fetchCount += 1;
        // First fetch: Alice unmarked. After a POST flips Alice to present,
        // the screen re-fetches; return Alice as present this time.
        final alicePresent = fetchCount > 1 ? true : null;
        return http.Response(
          jsonEncode({
            'occurrence': {
              'id': 'occ-1',
              'date': '2026-05-22',
              'state': 'OPEN_FOR_MARKING',
              'sabhaId': 'sabha-1',
            },
            'roster': [
              {'personId': 'p1', 'fullName': 'Alice', 'present': alicePresent},
            ],
          }),
          200,
        );
      }
      if (req.method == 'POST' && req.url.path.contains('/markings')) {
        postedBodies.add(jsonDecode(req.body) as Map<String, dynamic>);
        return http.Response('', 200);
      }
      return http.Response('not found', 404);
    });
    final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: client);

    await tester.pumpWidget(MaterialApp(home: Scaffold(body: RosterScreen(api: api))));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Alice'));
    await tester.pumpAndSettle();

    expect(postedBodies, hasLength(1));
    expect(postedBodies.single, {'personId': 'p1', 'present': true});
  });
}
