import 'package:flutter/foundation.dart';

import 'selection_api.dart';

/// A Roster Person the Sanchalak may nominate for the selective track.
class Nominee {
  const Nominee({required this.personId, required this.fullName});

  final String personId;
  final String fullName;
}

/// Per-Person nomination result on the nominate screen.
enum NominationOutcome {
  /// Not yet nominated.
  none,

  /// Nomination submitted and accepted.
  nominated,

  /// Nomination failed — see [SelectionController.errorFor].
  failed,
}

/// Drives the dedicated nominate screen (Slice 16, ADR-0006). Holds the Roster
/// People the Regular Sanchalak may nominate for the selective track and tracks a
/// per-Person outcome so each row reflects its own result. Online-only — keeps
/// async I/O out of the widget tree so widget tests can `await nominate(...)`.
class SelectionController extends ChangeNotifier {
  SelectionController({
    required this.api,
    required this.regularSabhaId,
    required this.people,
  });

  final SelectionApi api;
  final String regularSabhaId;
  final List<Nominee> people;

  final Map<String, NominationOutcome> _outcomes = {};
  final Map<String, String> _errors = {};
  String? _busyPersonId;

  /// The Person currently being submitted, or null when idle.
  String? get busyPersonId => _busyPersonId;

  NominationOutcome outcomeFor(String personId) =>
      _outcomes[personId] ?? NominationOutcome.none;

  /// The failure message for a Person whose nomination failed, else null.
  String? errorFor(String personId) => _errors[personId];

  /// Nominate the Person for the selective track. Records [NominationOutcome.nominated]
  /// on success or [NominationOutcome.failed] with the backend's message otherwise.
  Future<void> nominate(String personId) async {
    if (_busyPersonId != null || outcomeFor(personId) == NominationOutcome.nominated) {
      return;
    }
    _busyPersonId = personId;
    _errors.remove(personId);
    notifyListeners();
    try {
      await api.nominate(personId: personId, regularSabhaId: regularSabhaId);
      _outcomes[personId] = NominationOutcome.nominated;
    } on NominationNotAuthorizedException catch (e) {
      _fail(personId, e.message);
    } on AlreadyNominatedException catch (e) {
      _fail(personId, e.message);
    } on NominationRejectedException catch (e) {
      _fail(personId, e.message);
    } catch (_) {
      _fail(personId, 'Couldn\'t nominate — check your connection and try again.');
    } finally {
      _busyPersonId = null;
      notifyListeners();
    }
  }

  void _fail(String personId, String message) {
    _outcomes[personId] = NominationOutcome.failed;
    _errors[personId] = message;
  }
}
