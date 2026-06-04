import 'package:flutter/foundation.dart';

import 'monthly_occurrence_api.dart';

/// Immutable view state for the monthly-occurrence screen. Held by
/// [MonthlyOccurrenceController] so the widget tree stays free of async I/O and
/// widget tests can `await` each action explicitly.
class MonthlyOccurrenceViewState {
  const MonthlyOccurrenceViewState({
    required this.sabhas,
    required this.loading,
    required this.busy,
    this.error,
    this.createdOccurrenceId,
  });

  final List<MonthlySabha> sabhas;
  final bool loading;

  /// A create is in flight — the screen disables the submit control so the
  /// Sanchalak can't double-submit.
  final bool busy;

  /// The last create/load failure, surfaced inline. Cleared when the next
  /// create starts.
  final String? error;

  /// The id of the Occurrence created in this session, if any — lets the screen
  /// show a one-off confirmation.
  final String? createdOccurrenceId;

  MonthlyOccurrenceViewState copyWith({
    List<MonthlySabha>? sabhas,
    bool? loading,
    bool? busy,
    String? error,
    String? createdOccurrenceId,
    bool clearError = false,
    bool clearCreated = false,
  }) {
    return MonthlyOccurrenceViewState(
      sabhas: sabhas ?? this.sabhas,
      loading: loading ?? this.loading,
      busy: busy ?? this.busy,
      error: clearError ? null : (error ?? this.error),
      createdOccurrenceId: clearCreated ? null : (createdOccurrenceId ?? this.createdOccurrenceId),
    );
  }

  static const initial = MonthlyOccurrenceViewState(sabhas: [], loading: true, busy: false);
}

/// Drives the monthly-occurrence screen (Slice 12, ADR-0012). Loads the monthly
/// Sabhas the Sanchalak presides over (each with its compliance nudge), and
/// creates this month's Occurrence on a picked date. After a successful create
/// it reloads so the nudge for that Sabha clears. Online-only (ADR-0007):
/// failures surface as an inline error, never a queued mutation.
class MonthlyOccurrenceController extends ChangeNotifier {
  MonthlyOccurrenceController({required this.api});

  final MonthlyOccurrenceApi api;

  MonthlyOccurrenceViewState _state = MonthlyOccurrenceViewState.initial;
  MonthlyOccurrenceViewState get state => _state;

  Future<void> initialize() => _load();

  Future<void> _load() async {
    try {
      final sabhas = await api.fetchSabhas();
      _state = _state.copyWith(sabhas: sabhas, loading: false, clearError: true);
    } catch (_) {
      _state = _state.copyWith(loading: false, error: 'Couldn\'t load your Sabhas — check your connection.');
    }
    notifyListeners();
  }

  /// Creates this month's Occurrence for [sabhaId], then reloads so the Sabha's
  /// compliance nudge reflects the new Occurrence. Authorization (403) and
  /// domain-rule (422) failures become inline errors; the Sabha list is kept so
  /// the screen stays usable.
  Future<void> create(
    String sabhaId, {
    required String date,
    required String startTime,
    required String endTime,
    required String venue,
  }) async {
    if (_state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true, clearCreated: true);
    notifyListeners();
    try {
      final occurrenceId =
          await api.create(sabhaId, date: date, startTime: startTime, endTime: endTime, venue: venue);
      await _load();
      _state = _state.copyWith(busy: false, createdOccurrenceId: occurrenceId);
    } on MonthlyOccurrenceForbiddenException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } on MonthlyOccurrenceRuleException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: 'Create failed — check your connection and try again.');
    }
    notifyListeners();
  }
}
