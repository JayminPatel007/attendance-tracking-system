import 'package:flutter/foundation.dart';

import 'home_sabha_transfer_api.dart';

/// The step the Verified Home Sabha Transfer flow is currently showing.
enum HomeSabhaTransferView {
  /// Step 1 — find the Person by mobile.
  find,

  /// Step 2 — confirm the direction (this Person → your Sabha) before the OTP.
  confirm,

  /// Step 3 — enter the OTP the Person received.
  otp,

  /// Terminal — the transfer committed.
  done,
}

/// Immutable view state for the Home Sabha Transfer screen. Mirrors the
/// add-person / walk-in controllers so the widget tree stays free of async I/O
/// and widget tests can `await` each step.
class HomeSabhaTransferViewState {
  const HomeSabhaTransferViewState({
    required this.view,
    required this.person,
    required this.transferId,
    required this.busy,
    this.error,
    this.otpError,
  });

  final HomeSabhaTransferView view;
  final TransferPerson? person;
  final String? transferId;
  final bool busy;

  /// A step-level error (find failed, OTP send rejected) shown inline.
  final String? error;

  /// A wrong/expired/exhausted-OTP message shown on the OTP step only.
  final String? otpError;

  HomeSabhaTransferViewState copyWith({
    HomeSabhaTransferView? view,
    TransferPerson? person,
    String? transferId,
    bool? busy,
    String? error,
    String? otpError,
    bool clearPerson = false,
    bool clearError = false,
    bool clearOtpError = false,
  }) {
    return HomeSabhaTransferViewState(
      view: view ?? this.view,
      person: clearPerson ? null : (person ?? this.person),
      transferId: transferId ?? this.transferId,
      busy: busy ?? this.busy,
      error: clearError ? null : (error ?? this.error),
      otpError: clearOtpError ? null : (otpError ?? this.otpError),
    );
  }

  static const initial = HomeSabhaTransferViewState(
    view: HomeSabhaTransferView.find,
    person: null,
    transferId: null,
    busy: false,
  );
}

/// Drives the Verified Home Sabha Transfer flow (Slice 8, ADR-0002): find the
/// Person by mobile → confirm the direction → send the OTP → enter it → done.
/// Online-only (ADR-0007); failures surface as inline errors.
class HomeSabhaTransferController extends ChangeNotifier {
  HomeSabhaTransferController({required this.api, required this.destinationSabhaId});

  final HomeSabhaTransferApi api;

  /// The Sabha the Person is being pulled into — the Sanchalak's current Sabha.
  final String destinationSabhaId;

  HomeSabhaTransferViewState _state = HomeSabhaTransferViewState.initial;
  HomeSabhaTransferViewState get state => _state;

  /// Step 1: look the entered mobile up against the Directory.
  Future<void> findByMobile(String mobile) async {
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      final person = await api.findByMobile(mobile);
      if (person == null) {
        _state = _state.copyWith(
            busy: false, error: 'No Person in the Directory has that mobile.');
      } else {
        _state = _state.copyWith(
            view: HomeSabhaTransferView.confirm, person: person, busy: false);
      }
    } catch (_) {
      _state = _state.copyWith(
          busy: false, error: 'Couldn\'t look that up — check your connection.');
    }
    notifyListeners();
  }

  /// Step 2 → 3: initiate the transfer, which sends the OTP to the Person's
  /// mobile, then advance to the OTP-entry step.
  Future<void> sendOtp() async {
    final person = _state.person;
    if (person == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      final transferId =
          await api.initiate(personId: person.id, destinationSabhaId: destinationSabhaId);
      _state = _state.copyWith(
          view: HomeSabhaTransferView.otp, transferId: transferId, busy: false);
    } on TransferRateLimitedException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } on TransferNotAuthorizedException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: 'Couldn\'t send the OTP — try again.');
    }
    notifyListeners();
  }

  /// Step 3: submit the Person's OTP. On success the swap has committed; a
  /// rejected code (wrong / expired / exhausted) keeps the OTP step with the
  /// backend's message so the Sanchalak can act on it.
  Future<void> confirm(String otpCode) async {
    final transferId = _state.transferId;
    if (transferId == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearOtpError: true);
    notifyListeners();
    try {
      await api.confirm(transferId: transferId, otpCode: otpCode);
      _state = _state.copyWith(view: HomeSabhaTransferView.done, busy: false);
    } on TransferRejectedException catch (e) {
      _state = _state.copyWith(busy: false, otpError: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, otpError: 'Couldn\'t verify the OTP — try again.');
    }
    notifyListeners();
  }

  /// Back to step 1 to pick a different Person — drops the current selection.
  void backToFind() {
    _state = HomeSabhaTransferViewState.initial;
    notifyListeners();
  }

  /// Back to the confirm step from OTP entry — clears any OTP error.
  void backToConfirm() {
    _state = _state.copyWith(view: HomeSabhaTransferView.confirm, clearOtpError: true);
    notifyListeners();
  }
}
