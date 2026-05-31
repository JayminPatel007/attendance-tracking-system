import 'package:flutter/foundation.dart';

import '../sync/attendance_store.dart';
import 'walk_in_api.dart';

/// The step the walk-in flow is currently showing.
enum WalkInView {
  /// Entering a query / browsing results.
  search,

  /// A candidate is selected — the confirm sheet showing their Home Sabha.
  confirm,

  /// Terminal — the Walk-in was recorded.
  recorded,
}

/// Immutable view state for the walk-in screen. Mirrors [AddPersonController]'s
/// shape so the widget tree stays free of async I/O and widget tests can
/// `await` each step.
class WalkInViewState {
  const WalkInViewState({
    required this.view,
    required this.query,
    required this.results,
    required this.selected,
    required this.busy,
    required this.offline,
    required this.showWiderDirectoryHint,
    this.error,
  });

  final WalkInView view;
  final String query;
  final List<WalkInCandidate> results;
  final WalkInCandidate? selected;
  final bool busy;

  /// True when the last search ran against the cached Roster (online call
  /// failed). Drives the offline banner.
  final bool offline;

  /// True when an offline search found no cached match — the cue to "search the
  /// wider Directory when online" (ADR-0007).
  final bool showWiderDirectoryHint;

  final String? error;

  WalkInViewState copyWith({
    WalkInView? view,
    String? query,
    List<WalkInCandidate>? results,
    WalkInCandidate? selected,
    bool? busy,
    bool? offline,
    bool? showWiderDirectoryHint,
    String? error,
    bool clearSelected = false,
    bool clearError = false,
  }) {
    return WalkInViewState(
      view: view ?? this.view,
      query: query ?? this.query,
      results: results ?? this.results,
      selected: clearSelected ? null : (selected ?? this.selected),
      busy: busy ?? this.busy,
      offline: offline ?? this.offline,
      showWiderDirectoryHint: showWiderDirectoryHint ?? this.showWiderDirectoryHint,
      error: clearError ? null : (error ?? this.error),
    );
  }

  static const initial = WalkInViewState(
    view: WalkInView.search,
    query: '',
    results: <WalkInCandidate>[],
    selected: null,
    busy: false,
    offline: false,
    showWiderDirectoryHint: false,
  );
}

/// Drives the mobile-walk-in flow (Slice 7, issue #8): search the Directory for
/// a visitor → confirm their current Home Sabha → record the Walk-in. Online
/// search hits the full Directory; when the device is offline the search falls
/// back to the cached Roster only, flagging "search the wider Directory when
/// online" on no match (ADR-0007). Recording is online-only.
class WalkInController extends ChangeNotifier {
  WalkInController({
    required this.api,
    required this.store,
    required this.occurrenceId,
    required this.sabhaId,
  });

  final WalkInApi api;
  final AttendanceStore store;
  final String occurrenceId;
  final String sabhaId;

  WalkInViewState _state = WalkInViewState.initial;
  WalkInViewState get state => _state;

  Future<void> search(String query) async {
    _state = _state.copyWith(busy: true, query: query, clearError: true);
    notifyListeners();
    try {
      final results = await api.search(sabhaId: sabhaId, query: query);
      _state = _state.copyWith(
          results: results, busy: false, offline: false, showWiderDirectoryHint: false);
    } on WalkInApiException {
      // The server answered with a non-200 — reachable, so not an offline case.
      _state = _state.copyWith(busy: false, error: 'Search failed — please try again.');
    } catch (_) {
      // A connection failure — fall back to the cached Roster only (ADR-0007).
      final matches = await _searchCachedRoster(query);
      _state = _state.copyWith(
        results: matches,
        busy: false,
        offline: true,
        showWiderDirectoryHint: matches.isEmpty,
      );
    }
    notifyListeners();
  }

  Future<List<WalkInCandidate>> _searchCachedRoster(String query) async {
    final cached = await store.cachedRoster();
    if (cached == null) return const [];
    final needle = query.trim().toLowerCase();
    if (needle.isEmpty) return const [];
    return cached.roster
        .where((e) => e.fullName.toLowerCase().contains(needle))
        .map((e) => WalkInCandidate(personId: e.personId, fullName: e.fullName, homeSabha: ''))
        .toList();
  }

  void select(WalkInCandidate candidate) {
    _state = _state.copyWith(view: WalkInView.confirm, selected: candidate, clearError: true);
    notifyListeners();
  }

  void backToSearch() {
    _state = _state.copyWith(view: WalkInView.search, clearSelected: true, clearError: true);
    notifyListeners();
  }

  Future<void> record() async {
    final selected = _state.selected;
    if (selected == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      await api.recordWalkIn(occurrenceId: occurrenceId, personId: selected.personId);
      _state = _state.copyWith(view: WalkInView.recorded, busy: false);
    } catch (_) {
      _state = _state.copyWith(
          busy: false, error: 'Couldn\'t record the Walk-in — Walk-ins need a connection.');
    }
    notifyListeners();
  }
}
