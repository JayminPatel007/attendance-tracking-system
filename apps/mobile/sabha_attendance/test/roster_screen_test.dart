import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/roster/roster_controller.dart';
import 'package:sabha_attendance_mobile/roster/roster_screen.dart';
import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/sync/sync_engine.dart';

CurrentRoster _seedRoster({DateTime? rosterVersion}) => CurrentRoster(
      occurrence: OccurrenceView(
        id: 'occ-1', date: '2026-05-24', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
      roster: [
        RosterEntry(personId: 'p1', fullName: 'Alice', present: null),
        RosterEntry(personId: 'p2', fullName: 'Bob', present: true),
      ],
      rosterVersion: rosterVersion ?? DateTime.utc(2026, 5, 27, 10),
    );

void main() {
  sqfliteFfiInit();
  late AttendanceStore store;

  setUp(() async {
    store = await AttendanceStore.openInMemory(factory: databaseFactoryFfi);
  });

  tearDown(() async {
    await store.close();
  });

  DateTime fixedNow() => DateTime.utc(2026, 5, 27, 12);

  RosterController makeController(RosterApi api) {
    final engine = SyncEngine(store: store, api: api, clock: fixedNow);
    return RosterController(api: api, store: store, syncEngine: engine, now: fixedNow);
  }

  /// Pumps the widget tree after the controller is fully initialized. All
  /// sqflite + HTTP I/O happens inside `tester.runAsync` because `testWidgets`
  /// runs in a FakeAsync zone where real-time Futures never resolve.
  Future<RosterController> bootScreen(
    WidgetTester tester,
    RosterApi api, {
    Future<void> Function()? setupStore,
    VoidCallback? onOpenWalkIn,
  }) async {
    if (setupStore != null) {
      await tester.runAsync(setupStore);
    }
    final controller = makeController(api);
    await tester.runAsync(() => controller.initialize());
    await tester.pumpWidget(MaterialApp(
        home: Scaffold(body: RosterScreen(controller: controller, onOpenWalkIn: onOpenWalkIn))));
    await tester.pump();
    return controller;
  }

  testWidgets('roster renders people fetched from the backend and caches them locally', (tester) async {
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => http.Response(jsonEncode({
            'occurrence': {'id': 'occ-1', 'date': '2026-05-24', 'state': 'OPEN_FOR_MARKING', 'sabhaId': 'sabha-1'},
            'roster': [
              {'personId': 'p1', 'fullName': 'Alice', 'present': null},
              {'personId': 'p2', 'fullName': 'Bob', 'present': true},
            ],
            'rosterVersion': '2026-05-27T10:00:00.000Z',
          }), 200)),
    );
    await bootScreen(tester, api);

    expect(find.text('Alice'), findsOneWidget);
    expect(find.text('Bob'), findsOneWidget);
    final cached = await tester.runAsync(() => store.cachedRoster());
    expect(cached!.roster, hasLength(2));
  });

  testWidgets('offline launch falls back to the cached roster when the API call fails', (tester) async {
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => throw http.ClientException('offline')),
    );
    await bootScreen(tester, api,
        setupStore: () => store.cacheRoster(_seedRoster()));

    expect(find.text('Alice'), findsOneWidget);
    expect(find.text('Bob'), findsOneWidget);
  });

  testWidgets('tapping a person enqueues a pending marking without calling the mark endpoint', (tester) async {
    var markCalls = 0;
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async {
        if (req.url.path.endsWith('/markings')) {
          markCalls++;
          return http.Response('', 200);
        }
        return http.Response(jsonEncode({
          'occurrence': {'id': 'occ-1', 'date': '2026-05-24', 'state': 'OPEN_FOR_MARKING', 'sabhaId': 'sabha-1'},
          'roster': [{'personId': 'p1', 'fullName': 'Alice', 'present': null}],
          'rosterVersion': '2026-05-27T10:00:00.000Z',
        }), 200);
      }),
    );
    await bootScreen(tester, api);

    await tester.runAsync(() async {
      await tester.tap(find.text('Alice'));
      await Future<void>.delayed(const Duration(milliseconds: 30));
    });
    await tester.pump();

    expect(markCalls, 0);
    final pending = await tester.runAsync(() => store.pendingMarkings());
    expect(pending, hasLength(1));
    expect(pending!.single.personId, 'p1');
    expect(pending.single.present, true);
  });

  testWidgets('the Walk-in FAB opens the walk-in flow', (tester) async {
    var opened = 0;
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => http.Response(jsonEncode({
            'occurrence': {'id': 'occ-1', 'date': '2026-05-24', 'state': 'OPEN_FOR_MARKING', 'sabhaId': 'sabha-1'},
            'roster': [{'personId': 'p1', 'fullName': 'Alice', 'present': null}],
            'rosterVersion': '2026-05-27T10:00:00.000Z',
          }), 200)),
    );
    await bootScreen(tester, api, onOpenWalkIn: () => opened++);

    expect(find.byKey(const Key('walk-in-fab')), findsOneWidget);
    await tester.tap(find.byKey(const Key('walk-in-fab')));
    await tester.pump();

    expect(opened, 1);
  });

  testWidgets('when the cached rosterVersion is older than 7 days a blocking modal disables taps', (tester) async {
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => throw http.ClientException('offline')),
    );
    await bootScreen(tester, api,
        setupStore: () => store.cacheRoster(_seedRoster(rosterVersion: DateTime.utc(2026, 5, 19, 10))));

    expect(find.textContaining('Roster is'), findsOneWidget);
    expect(find.text('Sync now'), findsOneWidget);

    await tester.runAsync(() async {
      await tester.tap(find.text('Alice'), warnIfMissed: false);
      await Future<void>.delayed(const Duration(milliseconds: 30));
    });
    await tester.pump();
    final pending = await tester.runAsync(() => store.pendingMarkings());
    expect(pending, isEmpty);
  });
}
