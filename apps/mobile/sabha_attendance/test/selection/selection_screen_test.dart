import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/testing.dart';
import 'package:http/http.dart' as http;

import 'package:sabha_attendance_mobile/selection/selection_api.dart';
import 'package:sabha_attendance_mobile/selection/selection_controller.dart';
import 'package:sabha_attendance_mobile/selection/selection_screen.dart';

void main() {
  SelectionController controllerReturning(http.Response Function(http.Request) handler) {
    return SelectionController(
      api: SelectionApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async => handler(req)),
      ),
      regularSabhaId: 'sabha-2',
      people: const [
        Nominee(personId: 'p-1', fullName: 'Asha Patel'),
        Nominee(personId: 'p-2', fullName: 'Ravi Shah'),
      ],
    );
  }

  Future<void> pump(WidgetTester tester, SelectionController controller) {
    return tester.pumpWidget(MaterialApp(
      home: Scaffold(body: SelectionScreen(controller: controller)),
    ));
  }

  testWidgets('lists the roster People with a nominate action each', (tester) async {
    final controller = controllerReturning((req) => http.Response('{}', 200));
    await pump(tester, controller);

    expect(find.text('Asha Patel'), findsOneWidget);
    expect(find.text('Ravi Shah'), findsOneWidget);
    expect(find.byKey(const Key('nominate-p-1')), findsOneWidget);
    expect(find.byKey(const Key('nominate-p-2')), findsOneWidget);
  });

  testWidgets('nominating a Person confirms then shows the nominated state', (tester) async {
    final controller =
        controllerReturning((req) => http.Response('{"nominationId":"nom-1"}', 200));
    await pump(tester, controller);

    await tester.tap(find.byKey(const Key('nominate-p-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('nominate-confirm')));
    await tester.pumpAndSettle();

    expect(find.text('Nominated'), findsOneWidget);
  });

  testWidgets('a 409 surfaces the backend message inline for that Person', (tester) async {
    final controller = controllerReturning(
        (req) => http.Response('{"detail":"already selected"}', 409));
    await pump(tester, controller);

    await tester.tap(find.byKey(const Key('nominate-p-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('nominate-confirm')));
    await tester.pumpAndSettle();

    expect(find.text('already selected'), findsOneWidget);
  });
}
