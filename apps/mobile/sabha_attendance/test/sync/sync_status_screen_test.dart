import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/roster/roster_controller.dart';
import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/sync/pending_marking.dart';
import 'package:sabha_attendance_mobile/sync/sync_engine.dart';
import 'package:sabha_attendance_mobile/sync/sync_status_screen.dart';

void main() {
  sqfliteFfiInit();
  late AttendanceStore store;

  setUp(() async {
    store = await AttendanceStore.openInMemory(factory: databaseFactoryFfi);
    await store.cacheRoster(CurrentRoster(
      occurrence: OccurrenceView(
        id: 'occ-1', date: '2026-05-24', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
      roster: [RosterEntry(personId: 'p1', fullName: 'Alice', present: null)],
      rosterVersion: DateTime.utc(2026, 5, 27, 10),
    ));
  });

  tearDown(() async {
    await store.close();
  });

  DateTime fixedNow() => DateTime.utc(2026, 5, 27, 12);

  RosterController makeController(RosterApi api) {
    final engine = SyncEngine(store: store, api: api, clock: fixedNow);
    return RosterController(api: api, store: store, syncEngine: engine, now: fixedNow);
  }

  testWidgets('shows pending count, last sync time, and a manual sync button', (tester) async {
    await tester.runAsync(() async {
      await store.enqueueMarking(PendingMarking(
        occurrenceId: 'occ-1', personId: 'p1', present: true,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 11, 55)));
      await store.setLastSyncedAt(DateTime.utc(2026, 5, 27, 11, 0));
    });

    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((_) async => http.Response('', 200)),
    );
    final controller = makeController(api);

    await tester.pumpWidget(MaterialApp(
      home: SyncStatusScreen(controller: controller, now: fixedNow),
    ));
    await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
    await tester.pump();

    expect(find.text('1 queued for sync'), findsOneWidget);
    expect(find.text('1h ago'), findsOneWidget);
    expect(find.text('Sync now'), findsOneWidget);
  });

  testWidgets('Sync now pushes the queue and reflects the cleared state', (tester) async {
    await tester.runAsync(() async {
      await store.enqueueMarking(PendingMarking(
        occurrenceId: 'occ-1', personId: 'p1', present: true,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 11, 55)));
    });

    var syncCalls = 0;
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async {
        if (req.url.path.endsWith('/api/sync')) {
          syncCalls++;
          return http.Response(jsonEncode({'appliedCount': 1}), 200);
        }
        return http.Response('', 404);
      }),
    );
    final controller = makeController(api);

    await tester.pumpWidget(MaterialApp(
      home: SyncStatusScreen(controller: controller, now: fixedNow),
    ));
    await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
    await tester.pump();

    await tester.runAsync(() async {
      await tester.tap(find.text('Sync now'));
      await Future<void>.delayed(const Duration(milliseconds: 100));
    });
    await tester.pump();

    expect(syncCalls, 1);
    expect(find.text('0 queued for sync'), findsOneWidget);
  });
}
