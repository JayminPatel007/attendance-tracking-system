import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the self-service password-reset endpoints (ADR-0004, Slice
/// 18). Unlike the rest of the app, these calls carry **no** auth — a locked-out
/// user reaches them from the login screen before any token exists, so they sit
/// on the backend's public API chain. The two-step flow keeps the OTP off the
/// final password-set call:
///
///   requestReset(username)                  -> resetId    (OTP sent to mobile)
///   verify(resetId, otpCode)                -> resetToken
///   complete(resetToken, newPassword)       -> (password changed)
///
/// plus an unauthenticated [whoAppointedMe] lookup for users who have lost their
/// mobile entirely and need their appointer's contact instead.
class PasswordResetApi {
  PasswordResetApi({required this.baseUrl, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final http.Client _client;

  static const _jsonHeaders = {'Content-Type': 'application/json'};

  /// Step 1: ask the backend to send an OTP to the username's registered mobile.
  /// A `404` is an unknown username, `422` no registered mobile, `429` the resend
  /// cooldown or per-mobile rate limit.
  Future<String> requestReset(String username) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/password-reset/request'),
      headers: _jsonHeaders,
      body: jsonEncode({'username': username}),
    );
    if (resp.statusCode == 200) {
      return (jsonDecode(resp.body) as Map<String, dynamic>)['resetId'] as String;
    }
    if (resp.statusCode == 404) {
      throw UnknownUsernameException(_messageOf(resp.body) ?? 'No such username.');
    }
    if (resp.statusCode == 422) {
      throw NoRegisteredMobileException(
          _messageOf(resp.body) ?? 'No mobile is registered for that user.');
    }
    if (resp.statusCode == 429) {
      throw ResetRateLimitedException(
          _messageOf(resp.body) ?? 'Too many OTP requests — wait a moment and try again.');
    }
    throw PasswordResetApiException('POST request -> ${resp.statusCode}: ${resp.body}');
  }

  /// Step 2: exchange a correct OTP for a short-lived reset token. A `422` is a
  /// wrong, expired, or used-up OTP — [OtpRejectedException] carries the message.
  Future<String> verify({required String resetId, required String otpCode}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/password-reset/verify'),
      headers: _jsonHeaders,
      body: jsonEncode({'resetId': resetId, 'otpCode': otpCode}),
    );
    if (resp.statusCode == 200) {
      return (jsonDecode(resp.body) as Map<String, dynamic>)['resetToken'] as String;
    }
    if (resp.statusCode == 422) {
      throw OtpRejectedException(_messageOf(resp.body) ?? 'That OTP was rejected.');
    }
    throw PasswordResetApiException('POST verify -> ${resp.statusCode}: ${resp.body}');
  }

  /// Step 3: set the new password against the reset token. A `404`/`422` means the
  /// reset has expired or been used — the user must start again.
  Future<void> complete({required String resetToken, required String newPassword}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/password-reset/complete'),
      headers: _jsonHeaders,
      body: jsonEncode({'resetToken': resetToken, 'newPassword': newPassword}),
    );
    if (resp.statusCode == 200) return;
    if (resp.statusCode == 404 || resp.statusCode == 422) {
      throw ResetExpiredException(
          _messageOf(resp.body) ?? 'That reset has expired — start again.');
    }
    throw PasswordResetApiException('POST complete -> ${resp.statusCode}: ${resp.body}');
  }

  /// Lost-mobile lookup: who can reissue this username's password (their
  /// appointer, or a Madhyastha Karyalaya member for Sants). A `404` is unknown.
  Future<List<AppointerContact>> whoAppointedMe(String username) async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/who-appointed-me').replace(queryParameters: {'username': username}),
    );
    if (resp.statusCode == 200) {
      final contacts = (jsonDecode(resp.body) as Map<String, dynamic>)['contacts'] as List<dynamic>;
      return contacts
          .map((c) => AppointerContact.fromJson(c as Map<String, dynamic>))
          .toList(growable: false);
    }
    if (resp.statusCode == 404) {
      throw UnknownUsernameException(_messageOf(resp.body) ?? 'No such username.');
    }
    throw PasswordResetApiException('GET who-appointed-me -> ${resp.statusCode}: ${resp.body}');
  }

  String? _messageOf(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) return decoded['message'] as String?;
    } on FormatException {
      // non-JSON body
    }
    return null;
  }
}

/// Contact of whoever can reissue a locked-out user's password.
class AppointerContact {
  AppointerContact({required this.name, required this.mobile});

  final String name;
  final String mobile;

  factory AppointerContact.fromJson(Map<String, dynamic> json) {
    return AppointerContact(
      name: json['name'] as String,
      mobile: json['mobile'] as String,
    );
  }
}

class PasswordResetApiException implements Exception {
  PasswordResetApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The username is not known to the system (404).
class UnknownUsernameException implements Exception {
  UnknownUsernameException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The user has no registered mobile, so a self-service OTP can't be sent (422
/// on request) — they must use "Who appointed me?" instead.
class NoRegisteredMobileException implements Exception {
  NoRegisteredMobileException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The per-mobile OTP rate limit (3/hour) or 30s resend cooldown was hit (429).
class ResetRateLimitedException implements Exception {
  ResetRateLimitedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The OTP was wrong, expired, or used up (422 on verify). [message] is the
/// backend's explanation, e.g. how many attempts remain.
class OtpRejectedException implements Exception {
  OtpRejectedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The reset token has expired or been replayed (404/422 on complete).
class ResetExpiredException implements Exception {
  ResetExpiredException(this.message);
  final String message;
  @override
  String toString() => message;
}
