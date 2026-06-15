import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/walk_in/walk_in_api.dart';
import 'package:sabha_attendance_mobile/walk_in/walk_in_controller.dart';
import 'package:sabha_attendance_mobile/walk_in/walk_in_screen.dart';

class _FakeApi extends WalkInApi {
  _FakeApi({this.onSearch, this.onRecord}) : super(baseUrl: 'http://test', accessToken: 'tok');

  Future<List<WalkInCandidate>> Function(String sabhaId, String query)? onSearch;
  Future<void> Function(String occurrenceId, String personId)? onRecord;

  @override
  Future<List<WalkInCandidate>> search({required String sabhaId, required String query}) =>
      onSearch!(sabhaId, query);

  @override
  Future<void> recordWalkIn({required String occurrenceId, required String personId}) =>
      onRecord!(occurrenceId, personId);
}

void main() {
  sqfliteFfiInit();
  late AttendanceStore store;

  setUp(() async {
    store = await AttendanceStore.openInMemory(factory: databaseFactoryFfi);
  });

  tearDown(() async {
    await store.close();
  });

  WalkInController controllerWith(_FakeApi api) =>
      WalkInController(api: api, store: store, occurrenceId: 'occ-1', sabhaId: 'sabha-1');

  Future<void> pump(WidgetTester tester, WalkInController controller) async {
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: WalkInScreen(controller: controller))));
  }

  testWidgets('searching lists candidates and tapping one shows the confirm sheet with Home Sabha',
      (tester) async {
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => [
        WalkInCandidate(
          personId: 'p-110',
          fullName: 'Ramesh Shah',
          homeSabhas: const ['REGULAR_BAAL', 'REGULAR_SANYUKTA'],
        ),
      ],
    ));
    await pump(tester, controller);

    await tester.enterText(find.byKey(const Key('walk-in-search-field')), 'Ramesh');
    await tester.pump();
    await tester.tap(find.byKey(const Key('walk-in-search-button')));
    await tester.pumpAndSettle();

    expect(find.text('Ramesh Shah'), findsOneWidget);

    await tester.tap(find.text('Ramesh Shah'));
    await tester.pumpAndSettle();

    // Both Home Sabha kinds render on the confirm sheet (a Person has one per
    // kind: their demographic Sabha + Sanyukta).
    expect(find.text('REGULAR_BAAL, REGULAR_SANYUKTA'), findsOneWidget);
    expect(find.byKey(const Key('record-walk-in-button')), findsOneWidget);
  });

  testWidgets('recording a Walk-in shows the success state', (tester) async {
    final recorded = <String>[];
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => [
        WalkInCandidate(personId: 'p-110', fullName: 'Ramesh Shah', homeSabhas: const ['REGULAR_BAAL']),
      ],
      onRecord: (occ, person) async => recorded.add(person),
    ));
    await pump(tester, controller);
    await tester.enterText(find.byKey(const Key('walk-in-search-field')), 'Ramesh');
    await tester.pump();
    await tester.tap(find.byKey(const Key('walk-in-search-button')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ramesh Shah'));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('record-walk-in-button')));
    await tester.pumpAndSettle();

    expect(recorded, ['p-110']);
    expect(find.byKey(const Key('walk-in-recorded')), findsOneWidget);
  });

  testWidgets('an offline search with no match shows the offline banner and the wider-Directory hint',
      (tester) async {
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => throw http.ClientException('offline'),
    ));
    await pump(tester, controller);

    // The offline fallback reads the cached Roster from the real (ffi) SQLite
    // store, so drive the controller's async to completion under runAsync before
    // pumping the resulting frame.
    await tester.runAsync(() => controller.search('Visitor'));
    await tester.pump();

    expect(find.byKey(const Key('walk-in-offline-banner')), findsOneWidget);
    expect(find.byKey(const Key('walk-in-wider-directory-hint')), findsOneWidget);
  });
}
