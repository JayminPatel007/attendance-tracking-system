import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/sync/pending_marking.dart';
import 'package:sabha_attendance_mobile/sync/sync_engine.dart';

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

  test('syncNow with an empty queue is a no-op and does not call the backend', () async {
    var calls = 0;
    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async {
        calls++;
        return http.Response('{"appliedCount":0}', 200);
      }),
    );
    final engine = SyncEngine(store: store, api: api, clock: () => DateTime.utc(2026, 5, 27, 12));

    final outcome = await engine.syncNow();

    expect(outcome.appliedCount, 0);
    expect(outcome.staleRoster, isFalse);
    expect(calls, 0);
  });

  test('syncNow pushes queued markings and clears them on 200, recording lastSyncedAt', () async {
    await store.enqueueMarking(PendingMarking(
      occurrenceId: 'occ-1', personId: 'p1', present: true,
      clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 0)));
    await store.enqueueMarking(PendingMarking(
      occurrenceId: 'occ-1', personId: 'p2', present: false,
      clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 5)));

    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => http.Response(jsonEncode({'appliedCount': 2}), 200)),
    );
    final now = DateTime.utc(2026, 5, 27, 19, 10);
    final engine = SyncEngine(store: store, api: api, clock: () => now);

    final outcome = await engine.syncNow();

    expect(outcome.appliedCount, 2);
    expect(outcome.staleRoster, isFalse);
    expect(await store.pendingMarkings(), isEmpty);
    expect(await store.lastSyncedAt(), now);
  });

  test('syncNow surfaces stale-roster from the backend without clearing the queue', () async {
    final m = PendingMarking(
      occurrenceId: 'occ-1', personId: 'p1', present: true,
      clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 0));
    await store.enqueueMarking(m);

    final api = RosterApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => http.Response(
            jsonEncode({'code': 'ROSTER_STALE', 'status': 409, 'message': 'stale'}),
            409,
            headers: {'content-type': 'application/json'},
          )),
    );
    final engine = SyncEngine(store: store, api: api, clock: () => DateTime.utc(2026, 5, 27, 19, 10));

    final outcome = await engine.syncNow();

    expect(outcome.staleRoster, isTrue);
    expect(outcome.appliedCount, 0);
    expect(await store.pendingMarkings(), [m]);
    expect(await store.lastSyncedAt(), isNull);
  });

  test('isBlockedByStaleRoster is true when cached rosterVersion is older than 7 days', () async {
    await store.cacheRoster(CurrentRoster(
      occurrence: OccurrenceView(
        id: 'occ-1', date: '2026-05-24', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
      roster: [],
      rosterVersion: DateTime.utc(2026, 5, 20, 10),
    ));
    final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: MockClient((_) async => http.Response('', 200)));
    final engine = SyncEngine(store: store, api: api, clock: () => DateTime.utc(2026, 5, 27, 10, 0, 1));

    expect(await engine.isBlockedByStaleRoster(), isTrue);
  });

  test('isBlockedByStaleRoster is false when the cached snapshot is exactly 7 days old', () async {
    await store.cacheRoster(CurrentRoster(
      occurrence: OccurrenceView(
        id: 'occ-1', date: '2026-05-24', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
      roster: [],
      rosterVersion: DateTime.utc(2026, 5, 20, 10),
    ));
    final api = RosterApi(baseUrl: 'http://test', accessToken: 'tok', client: MockClient((_) async => http.Response('', 200)));
    final engine = SyncEngine(store: store, api: api, clock: () => DateTime.utc(2026, 5, 27, 10));

    expect(await engine.isBlockedByStaleRoster(), isFalse);
  });
}
