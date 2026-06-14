import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the Verified Home Sabha Transfer endpoints (Slice 8,
/// ADR-0002). The whole flow is **online-only** (ADR-0007): the Person is found
/// against the live Directory, and the OTP initiate/confirm must reach the
/// backend — nothing here is ever queued offline.
class HomeSabhaTransferApi {
  HomeSabhaTransferApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// Step 1: find the Person standing in front of the Sanchalak by their mobile
  /// — the system-wide-unique key (ADR-0013), so this works even when they are
  /// transferring in from another Kshetra. Returns `null` when no Person matches.
  Future<TransferPerson?> findByMobile(String mobile) async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/directory/persons').replace(queryParameters: {'mobile': mobile}),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode == 404) return null;
    if (resp.statusCode != 200) {
      throw HomeSabhaTransferApiException('GET persons?mobile -> ${resp.statusCode}: ${resp.body}');
    }
    return TransferPerson.fromJson(jsonDecode(resp.body) as Map<String, dynamic>);
  }

  /// Step 2: initiate the transfer — the backend generates an OTP and sends it
  /// to the Person's mobile. Returns the new transfer id. A `403` means the
  /// caller is not the destination Sabha's Sanchalak/Sah-Sanchalak; a `429` is
  /// the per-mobile rate limit (3/hour) or resend cooldown.
  Future<String> initiate({required String personId, required String destinationSabhaId}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/home-sabha-transfers'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'personId': personId, 'destinationSabhaId': destinationSabhaId}),
    );
    if (resp.statusCode == 200) {
      return (jsonDecode(resp.body) as Map<String, dynamic>)['transferId'] as String;
    }
    if (resp.statusCode == 403) {
      throw TransferNotAuthorizedException(
          _messageOf(resp.body) ?? 'Only the destination Sabha\'s Sanchalak can do this.');
    }
    if (resp.statusCode == 429) {
      throw TransferRateLimitedException(
          _messageOf(resp.body) ?? 'Too many OTP requests — wait a moment and try again.');
    }
    throw HomeSabhaTransferApiException('POST home-sabha-transfers -> ${resp.statusCode}: ${resp.body}');
  }

  /// Step 3: submit the Person's OTP. On success (`200`) the Roster swap has
  /// committed. A `422` carries the domain rejection — wrong OTP, expired TTL, or
  /// attempts exhausted — surfaced via [TransferRejectedException] with the
  /// backend's message so the Sanchalak can read out what went wrong.
  Future<void> confirm({required String transferId, required String otpCode}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/home-sabha-transfers/$transferId/confirm'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'otpCode': otpCode}),
    );
    if (resp.statusCode == 200) return;
    if (resp.statusCode == 422) {
      throw TransferRejectedException(_messageOf(resp.body) ?? 'That OTP was rejected.');
    }
    throw HomeSabhaTransferApiException('POST confirm -> ${resp.statusCode}: ${resp.body}');
  }

  String? _messageOf(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) return decoded['detail'] as String?;
    } on FormatException {
      // non-JSON body
    }
    return null;
  }
}

class HomeSabhaTransferApiException implements Exception {
  HomeSabhaTransferApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The caller is not authorized to initiate a transfer into the destination
/// Sabha (403) — they hold neither Sanchalak nor Sah-Sanchalak there.
class TransferNotAuthorizedException implements Exception {
  TransferNotAuthorizedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The per-mobile OTP rate limit (3/hour) or 30s resend cooldown was hit (429).
class TransferRateLimitedException implements Exception {
  TransferRateLimitedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The OTP confirm was rejected by a domain rule (422): wrong code, expired TTL,
/// or attempts exhausted. [message] is the backend's explanation.
class TransferRejectedException implements Exception {
  TransferRejectedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The Person being transferred, as returned by the Directory mobile lookup.
class TransferPerson {
  TransferPerson({required this.id, required this.fullName, this.mobile});

  final String id;
  final String fullName;
  final String? mobile;

  factory TransferPerson.fromJson(Map<String, dynamic> json) {
    return TransferPerson(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      mobile: json['mobile'] as String?,
    );
  }
}
