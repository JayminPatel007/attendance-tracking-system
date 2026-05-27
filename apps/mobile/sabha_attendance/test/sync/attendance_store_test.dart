import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/sync/pending_marking.dart';

void main() {
  sqfliteFfiInit();
  late AttendanceStore store;

  setUp(() async {
    store = await AttendanceStore.openInMemory(factory: databaseFactoryFfi);
  });

  tearDown(() async {
    await store.close();
  });

  group('cached roster', () {
    test('cacheRoster persists the snapshot so it can be read back offline', () async {
      final snap = CurrentRoster(
        occurrence: OccurrenceView(
          id: 'occ-1',
          date: '2026-05-24',
          state: 'OPEN_FOR_MARKING',
          sabhaId: 'sabha-1',
        ),
        roster: [
          RosterEntry(personId: 'p1', fullName: 'Alice', present: null),
          RosterEntry(personId: 'p2', fullName: 'Bob', present: true),
        ],
        rosterVersion: DateTime.utc(2026, 5, 27, 12),
      );

      await store.cacheRoster(snap);
      final loaded = await store.cachedRoster();

      expect(loaded, isNotNull);
      expect(loaded!.occurrence.id, 'occ-1');
      expect(loaded.roster, hasLength(2));
      expect(loaded.roster[1].present, true);
      expect(loaded.rosterVersion, DateTime.utc(2026, 5, 27, 12));
    });

    test('cacheRoster replaces the previous snapshot rather than accumulating rows', () async {
      await store.cacheRoster(CurrentRoster(
        occurrence: OccurrenceView(id: 'occ-1', date: '2026-05-24', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
        roster: [RosterEntry(personId: 'p1', fullName: 'Alice', present: null)],
        rosterVersion: DateTime.utc(2026, 5, 20),
      ));
      await store.cacheRoster(CurrentRoster(
        occurrence: OccurrenceView(id: 'occ-2', date: '2026-05-31', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
        roster: [
          RosterEntry(personId: 'p1', fullName: 'Alice', present: null),
          RosterEntry(personId: 'p3', fullName: 'Charlie', present: null),
        ],
        rosterVersion: DateTime.utc(2026, 5, 27),
      ));

      final loaded = await store.cachedRoster();
      expect(loaded!.occurrence.id, 'occ-2');
      expect(loaded.roster, hasLength(2));
      expect(loaded.rosterVersion, DateTime.utc(2026, 5, 27));
    });

    test('cachedRoster returns null on a fresh store', () async {
      expect(await store.cachedRoster(), isNull);
    });
  });

  group('pending queue', () {
    test('enqueueMarking holds the marking with its clientMarkedAt', () async {
      final m = PendingMarking(
        occurrenceId: 'occ-1',
        personId: 'p1',
        present: true,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 5),
      );

      await store.enqueueMarking(m);

      expect(await store.pendingMarkings(), [m]);
    });

    test('re-enqueueing the same (occurrence, person) replaces the prior entry with the newer one', () async {
      final earlier = PendingMarking(
        occurrenceId: 'occ-1', personId: 'p1', present: true,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 0));
      final later = PendingMarking(
        occurrenceId: 'occ-1', personId: 'p1', present: false,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 5));

      await store.enqueueMarking(earlier);
      await store.enqueueMarking(later);

      expect(await store.pendingMarkings(), [later]);
    });

    test('clearPending removes only the listed (occurrence, person) keys', () async {
      final a = PendingMarking(
        occurrenceId: 'occ-1', personId: 'p1', present: true,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 0));
      final b = PendingMarking(
        occurrenceId: 'occ-1', personId: 'p2', present: false,
        clientMarkedAt: DateTime.utc(2026, 5, 27, 19, 1));

      await store.enqueueMarking(a);
      await store.enqueueMarking(b);
      await store.clearPending([a]);

      expect(await store.pendingMarkings(), [b]);
    });
  });

  group('last sync timestamp', () {
    test('lastSyncedAt round-trips', () async {
      final t = DateTime.utc(2026, 5, 27, 12, 34, 56);
      await store.setLastSyncedAt(t);
      expect(await store.lastSyncedAt(), t);
    });

    test('lastSyncedAt returns null when never set', () async {
      expect(await store.lastSyncedAt(), isNull);
    });
  });
}
