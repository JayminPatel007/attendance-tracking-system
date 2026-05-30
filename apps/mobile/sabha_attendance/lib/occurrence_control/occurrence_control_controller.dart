import 'package:flutter/foundation.dart';

import 'occurrence_control_api.dart';

/// Immutable view state for the occurrence-control screen. Held by
/// [OccurrenceControlController] so the widget tree stays free of async I/O and
/// widget tests can `await` each action explicitly.
class OccurrenceControlViewState {
  const OccurrenceControlViewState({
    required this.occurrence,
    required this.loading,
    required this.busy,
    this.error,
  });

  final ShapeableOccurrence? occurrence;
  final bool loading;

  /// An action (cancel/revert/reschedule/venue) is in flight — the screen
  /// disables the action controls so a Sanchalak can't double-submit.
  final bool busy;

  /// The last action or load failure, surfaced inline. Cleared when the next
  /// action starts.
  final String? error;

  OccurrenceControlViewState copyWith({
    ShapeableOccurrence? occurrence,
    bool? loading,
    bool? busy,
    String? error,
    bool clearError = false,
  }) {
    return OccurrenceControlViewState(
      occurrence: occurrence ?? this.occurrence,
      loading: loading ?? this.loading,
      busy: busy ?? this.busy,
      error: clearError ? null : (error ?? this.error),
    );
  }

  static const initial = OccurrenceControlViewState(
    occurrence: null,
    loading: true,
    busy: false,
  );
}

/// Drives the occurrence-control screen (Slice 5, ADR-0001). Each Sabha-shaping
/// action calls the backend and then reloads, so the screen always reflects the
/// authoritative server state (including a state flipped by a concurrent
/// scanner). These actions are online-only (ADR-0007) — failures surface as an
/// inline error rather than a queued mutation.
class OccurrenceControlController extends ChangeNotifier {
  OccurrenceControlController({required this.api});

  final OccurrenceControlApi api;

  OccurrenceControlViewState _state = OccurrenceControlViewState.initial;
  OccurrenceControlViewState get state => _state;

  Future<void> initialize() => _load();

  Future<void> _load() async {
    try {
      final occ = await api.fetch();
      _state = _state.copyWith(occurrence: occ, loading: false, clearError: true);
    } catch (e) {
      _state = _state.copyWith(loading: false, error: 'Couldn\'t load the Occurrence — check your connection.');
    }
    notifyListeners();
  }

  Future<void> cancel(String reason) => _run((id) => api.cancel(id, reason));

  Future<void> revert() => _run((id) => api.revert(id));

  Future<void> reschedule({
    required String date,
    required String startTime,
    required String endTime,
  }) =>
      _run((id) => api.reschedule(id, date: date, startTime: startTime, endTime: endTime));

  Future<void> overrideVenue(String venue) => _run((id) => api.overrideVenue(id, venue));

  /// Runs one Sabha-shaping action against the loaded Occurrence, then reloads.
  /// Authorization (403) and domain-rule (422) failures become inline errors;
  /// the loaded Occurrence is preserved so the screen stays usable.
  Future<void> _run(Future<void> Function(String occurrenceId) action) async {
    final occ = _state.occurrence;
    if (occ == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      await action(occ.id);
      await _load();
      _state = _state.copyWith(busy: false);
    } on OccurrenceForbiddenException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } on OccurrenceRuleException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: 'Action failed — check your connection and try again.');
    }
    notifyListeners();
  }
}
