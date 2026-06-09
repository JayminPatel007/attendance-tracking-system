import 'package:flutter/foundation.dart';

import 'password_reset_api.dart';

/// The step the self-service reset flow is currently showing.
enum PasswordResetView {
  /// Enter the username to receive an OTP.
  username,

  /// Enter the OTP that was sent to the registered mobile.
  otp,

  /// Set the new password.
  password,

  /// Terminal — the password has been reset.
  done,
}

/// Immutable view state for the password-reset screen. Mirrors the Home Sabha
/// Transfer controller so the widget tree stays free of async I/O and widget
/// tests can `await` each step.
class PasswordResetViewState {
  const PasswordResetViewState({
    required this.view,
    required this.busy,
    this.error,
    this.otpError,
  });

  final PasswordResetView view;
  final bool busy;

  /// A step-level error (request failed, reset expired) shown inline.
  final String? error;

  /// A wrong/expired/exhausted-OTP message shown on the OTP step only.
  final String? otpError;

  PasswordResetViewState copyWith({
    PasswordResetView? view,
    bool? busy,
    String? error,
    String? otpError,
    bool clearError = false,
    bool clearOtpError = false,
  }) {
    return PasswordResetViewState(
      view: view ?? this.view,
      busy: busy ?? this.busy,
      error: clearError ? null : (error ?? this.error),
      otpError: clearOtpError ? null : (otpError ?? this.otpError),
    );
  }

  static const initial = PasswordResetViewState(
    view: PasswordResetView.username,
    busy: false,
  );
}

/// Drives the self-service password-reset flow (ADR-0004, Slice 18B): enter
/// username → receive an OTP on the registered mobile → enter it → set a new
/// password → done. Unauthenticated throughout; failures surface as inline
/// errors, and the no-registered-mobile case points the user at the
/// who-appointed-me lookup.
class PasswordResetController extends ChangeNotifier {
  PasswordResetController({required this.api});

  final PasswordResetApi api;

  PasswordResetViewState _state = PasswordResetViewState.initial;
  PasswordResetViewState get state => _state;

  String? _resetId;
  String? _resetToken;

  /// Step 1: ask the backend to send an OTP, then advance to the OTP step.
  Future<void> requestOtp(String username) async {
    if (_state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      _resetId = await api.requestReset(username);
      _state = _state.copyWith(view: PasswordResetView.otp, busy: false);
    } on UnknownUsernameException {
      _state = _state.copyWith(
          busy: false, error: "We couldn't find that username. Check the spelling and try again.");
    } on NoRegisteredMobileException {
      _state = _state.copyWith(
          busy: false,
          error: 'No mobile is registered for that user. Use "Who appointed me?" '
              'to find who can reset your password.');
    } on ResetRateLimitedException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: "Couldn't send the OTP - try again.");
    }
    notifyListeners();
  }

  /// Step 2: exchange the entered OTP for a reset token, then advance to the
  /// new-password step. A rejected code keeps the OTP step with the reason.
  Future<void> verify(String otpCode) async {
    final resetId = _resetId;
    if (resetId == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearOtpError: true);
    notifyListeners();
    try {
      _resetToken = await api.verify(resetId: resetId, otpCode: otpCode);
      _state = _state.copyWith(view: PasswordResetView.password, busy: false);
    } on OtpRejectedException catch (e) {
      _state = _state.copyWith(busy: false, otpError: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, otpError: "Couldn't verify the OTP - try again.");
    }
    notifyListeners();
  }

  /// Step 3: set the new password against the reset token. An expired/replayed
  /// token keeps the password step with an error so the user can start again.
  Future<void> complete(String newPassword) async {
    final resetToken = _resetToken;
    if (resetToken == null || _state.busy) return;
    _state = _state.copyWith(busy: true, clearError: true);
    notifyListeners();
    try {
      await api.complete(resetToken: resetToken, newPassword: newPassword);
      _state = _state.copyWith(view: PasswordResetView.done, busy: false);
    } on ResetExpiredException catch (e) {
      _state = _state.copyWith(busy: false, error: e.message);
    } catch (_) {
      _state = _state.copyWith(busy: false, error: "Couldn't set the password - try again.");
    }
    notifyListeners();
  }

  /// Back to the start, dropping any in-flight reset.
  void restart() {
    _resetId = null;
    _resetToken = null;
    _state = PasswordResetViewState.initial;
    notifyListeners();
  }
}
