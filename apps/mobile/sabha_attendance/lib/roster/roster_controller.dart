import 'package:flutter/foundation.dart';

import '../sync/attendance_store.dart';
import '../sync/pending_marking.dart';
import '../sync/sync_engine.dart';
import 'roster_api.dart';

class RosterViewState {
  const RosterViewState({
    required this.roster,
    required this.pendingPresent,
    required this.pendingAbsent,
    required this.blocked,
    required this.staleDays,
    required this.loading,
    this.error,
  });

  final CurrentRoster? roster;
  final Set<String> pendingPresent;
  final Set<String> pendingAbsent;
  final bool blocked;
  final int staleDays;
  final bool loading;
  final String? error;

  bool? displayedPresence(RosterEntry entry) {
    if (pendingPresent.contains(entry.personId)) return true;
    if (pendingAbsent.contains(entry.personId)) return false;
    return entry.present;
  }

  static const initial = RosterViewState(
    roster: null,
    pendingPresent: <String>{},
    pendingAbsent: <String>{},
    blocked: false,
    staleDays: 0,
    loading: true,
  );
}

/// Drives the [RosterScreen] / [SyncStatusScreen] state. Keeps all the async
/// I/O (HTTP, sqflite) out of the widget tree so widget tests can `await
/// initialize()` explicitly instead of relying on `pumpAndSettle` to drain
/// background isolate work (ADR-0007).
class RosterController extends ChangeNotifier {
  RosterController({
    required this.api,
    required this.store,
    required this.syncEngine,
    DateTime Function()? now,
  }) : _now = now ?? (() => DateTime.now().toUtc());

  final RosterApi api;
  final AttendanceStore store;
  final SyncEngine syncEngine;
  final DateTime Function() _now;

  RosterViewState _state = RosterViewState.initial;
  RosterViewState get state => _state;

  Future<void> initialize() async {
    try {
      final fresh = await api.fetch();
      await store.cacheRoster(fresh);
    } catch (_) {
      // offline or transient — fall back to cached snapshot below
    }
    await refreshFromStore();
  }

  Future<void> refreshFromStore() async {
    final cached = await store.cachedRoster();
    final blocked = await syncEngine.isBlockedByStaleRoster();
    final pending = await store.pendingMarkings();
    final present = <String>{};
    final absent = <String>{};
    for (final m in pending) {
      (m.present ? present : absent).add(m.personId);
    }
    final staleDays = cached == null ? 0 : _now().difference(cached.rosterVersion).inDays;
    _state = RosterViewState(
      roster: cached,
      pendingPresent: present,
      pendingAbsent: absent,
      blocked: blocked,
      staleDays: staleDays,
      loading: false,
      error: cached == null ? 'No cached roster yet — connect and pull to start.' : null,
    );
    notifyListeners();
  }

  Future<void> toggle(RosterEntry entry) async {
    if (_state.blocked || _state.roster == null) return;
    final current = _state.displayedPresence(entry);
    final next = !(current ?? false);
    await store.enqueueMarking(PendingMarking(
      occurrenceId: _state.roster!.occurrence.id,
      personId: entry.personId,
      present: next,
      clientMarkedAt: _now(),
    ));
    await refreshFromStore();
  }

  Future<SyncOutcome> syncNow() async {
    final outcome = await syncEngine.syncNow();
    if (outcome.staleRoster) {
      try {
        final fresh = await api.fetch();
        await store.cacheRoster(fresh);
      } catch (_) {/* keep modal */}
    }
    await refreshFromStore();
    return outcome;
  }

  Future<int> pendingCount() async => (await store.pendingMarkings()).length;
  Future<DateTime?> lastSyncedAt() => store.lastSyncedAt();
}
