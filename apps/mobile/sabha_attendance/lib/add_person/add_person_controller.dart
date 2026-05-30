import 'package:flutter/foundation.dart';

import 'add_person_api.dart';

/// The step the add-person flow is currently showing.
enum AddPersonView {
  /// Step 1 — mobile entry, the de-dup key (ADR-0013).
  mobile,

  /// Exact mobile match — the existing Person's profile (forced redirect).
  profile,

  /// Step 2 — name / gender / DOB; may also be showing soft-warn candidates.
  details,

  /// Terminal — the Person was created.
  created,
}

/// Immutable view state for the add-person screen. Mirrors the
/// occurrence-control pattern so the widget tree stays free of async I/O and
/// widget tests can `await` each step.
class AddPersonViewState {
  const AddPersonViewState({
    required this.view,
    required this.mobile,
    required this.existingPerson,
    required this.candidates,
    required this.requiresOverride,
    required this.createdPersonId,
    required this.busy,
    this.error,
    this.blockedByExistingId,
  });

  final AddPersonView view;
  final String mobile;

  /// The Person an exact mobile match landed on (profile view).
  final DirectoryPerson? existingPerson;

  /// Name soft-warn candidates surfaced on the details step.
  final List<NameCandidate> candidates;

  /// True once a soft-warn fired — the only way past it is create-new-anyway.
  final bool requiresOverride;

  final String? createdPersonId;
  final bool busy;
  final String? error;

  /// Set when a hard block raced us at submit time — the id to redirect to.
  final String? blockedByExistingId;

  AddPersonViewState copyWith({
    AddPersonView? view,
    String? mobile,
    DirectoryPerson? existingPerson,
    List<NameCandidate>? candidates,
    bool? requiresOverride,
    String? createdPersonId,
    bool? busy,
    String? error,
    String? blockedByExistingId,
    bool clearExisting = false,
    bool clearError = false,
  }) {
    return AddPersonViewState(
      view: view ?? this.view,
      mobile: mobile ?? this.mobile,
      existingPerson: clearExisting ? null : (existingPerson ?? this.existingPerson),
      candidates: candidates ?? this.candidates,
      requiresOverride: requiresOverride ?? this.requiresOverride,
      createdPersonId: createdPersonId ?? this.createdPersonId,
      busy: busy ?? this.busy,
      error: clearError ? null : (error ?? this.error),
      blockedByExistingId: clearError ? null : (blockedByExistingId ?? this.blockedByExistingId),
    );
  }

  static const initial = AddPersonViewState(
    view: AddPersonView.mobile,
    mobile: '',
    existingPerson: null,
    candidates: <NameCandidate>[],
    requiresOverride: false,
    createdPersonId: null,
    busy: false,
  );
}

/// Drives the two-step add-person flow (Slice 6, ADR-0013): mobile lookup →
/// existing-Person redirect or details entry → create, with the name soft-warn
/// handled inline. Online-only (ADR-0007); failures surface as inline errors.
class AddPersonController extends ChangeNotifier {
  AddPersonController({required this.api, required this.homeSabhaId});

  final AddPersonApi api;

  /// The Home Sabha a created Person is registered to — the Sanchalak's current
  /// Sabha. Also the scope the backend derives for the name soft-warn.
  final String homeSabhaId;

  AddPersonViewState _state = AddPersonViewState.initial;
  AddPersonViewState get state => _state;

  /// Step 1: check the entered mobile against the Directory.
  Future<void> checkMobile(String mobile) async {
    _state = _state.copyWith(busy: true, mobile: mobile, clearError: true);
    notifyListeners();
    try {
      final existing = await api.findByMobile(mobile);
      if (existing != null) {
        _state = _state.copyWith(view: AddPersonView.profile, existingPerson: existing, busy: false);
      } else {
        _state = _state.copyWith(view: AddPersonView.details, busy: false, clearExisting: true);
      }
    } catch (_) {
      _state = _state.copyWith(busy: false, error: 'Couldn\'t check that number — check your connection.');
    }
    notifyListeners();
  }

  /// Step 2: create the Person. A soft-warn keeps us on the details step with
  /// the candidate list shown; only [createNewAnyway] gets past it.
  Future<void> submitDetails({
    required String fullName,
    required String gender,
    String? dateOfBirth,
    String? guardianPersonId,
    bool override = false,
  }) async {
    final request = AddPersonRequest(
      fullName: fullName,
      gender: gender,
      dateOfBirth: dateOfBirth,
      mobile: guardianPersonId == null ? _state.mobile : null,
      guardianPersonId: guardianPersonId,
      homeSabhaId: homeSabhaId,
      overrideDuplicateWarning: override,
    );
    await _submit(request);
  }

  /// "None of these — create new" past a soft-warn: re-issue the last add with
  /// the override flag set.
  Future<void> createNewAnyway() => submitDetails(
        fullName: _lastRequest!.fullName,
        gender: _lastRequest!.gender,
        dateOfBirth: _lastRequest!.dateOfBirth,
        guardianPersonId: _lastRequest!.guardianPersonId,
        override: true,
      );

  AddPersonRequest? _lastRequest;

  Future<void> _submit(AddPersonRequest request) async {
    if (_state.busy) return;
    _lastRequest = request;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      final outcome = await api.add(request);
      if (outcome.created) {
        _state = _state.copyWith(
            view: AddPersonView.created, createdPersonId: outcome.createdPersonId, busy: false);
      } else {
        _state = _state.copyWith(
            candidates: outcome.candidates, requiresOverride: true, busy: false);
      }
    } on MobileAlreadyRegisteredException catch (e) {
      _state = _state.copyWith(
          busy: false, error: e.message, blockedByExistingId: e.existingPersonId);
    } on DirectoryRuleException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: 'Add failed — check your connection and try again.');
    }
    notifyListeners();
  }

  /// Back to step 1 from the profile or details step.
  void backToMobile() {
    _state = AddPersonViewState.initial;
    notifyListeners();
  }
}
