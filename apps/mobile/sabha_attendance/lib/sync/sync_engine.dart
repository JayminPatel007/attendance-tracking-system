import '../roster/roster_api.dart';
import 'attendance_store.dart';

typedef Clock = DateTime Function();

class SyncOutcome {
  const SyncOutcome({required this.appliedCount, required this.staleRoster});
  final int appliedCount;
  final bool staleRoster;
}

/// Glues the offline [AttendanceStore] queue to the backend's `POST /api/sync`.
/// On a successful push the queue is cleared and `lastSyncedAt` is recorded;
/// on `ROSTER_STALE` the queue is preserved so the user can refresh the
/// Roster and retry (ADR-0007).
class SyncEngine {
  SyncEngine({required this.store, required this.api, required this.clock});

  static const Duration maxRosterAge = Duration(days: 7);

  final AttendanceStore store;
  final RosterApi api;
  final Clock clock;

  Future<SyncOutcome> syncNow() async {
    final pending = await store.pendingMarkings();
    if (pending.isEmpty) {
      return const SyncOutcome(appliedCount: 0, staleRoster: false);
    }
    final cached = await store.cachedRoster();
    if (cached == null) {
      throw StateError('cannot sync without a cached roster snapshot');
    }
    try {
      final applied = await api.sync(rosterVersion: cached.rosterVersion, markings: pending);
      await store.clearPending(pending);
      await store.setLastSyncedAt(clock());
      return SyncOutcome(appliedCount: applied, staleRoster: false);
    } on StaleRosterException {
      return const SyncOutcome(appliedCount: 0, staleRoster: true);
    }
  }

  /// True when the cached Roster snapshot is older than [maxRosterAge] and
  /// the UI must therefore block new markings until a fresh sync (ADR-0007).
  Future<bool> isBlockedByStaleRoster() async {
    final cached = await store.cachedRoster();
    if (cached == null) return false;
    return clock().difference(cached.rosterVersion) > maxRosterAge;
  }
}
