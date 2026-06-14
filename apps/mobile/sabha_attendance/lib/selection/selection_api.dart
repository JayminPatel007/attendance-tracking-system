import 'dart:convert';
import 'package:http/http.dart' as http;

import '../api/api_error.dart';

/// Thin wrapper over the mobile BSS/YSS nomination endpoint (Slice 16, ADR-0006).
/// The Regular Sanchalak nominates a Person from their Roster for the selective
/// track; the selective Sabha is derived server-side. Online-only — a nomination
/// is never queued offline.
class SelectionApi {
  SelectionApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// Nominate the Roster Person for the selective track. Returns the new
  /// nomination id. A `403` means the caller is not the Sabha's Sanchalak; a
  /// `409` that the Person already has an open nomination or is already selected;
  /// a `422` a domain rejection (not on roster, no selective track/Sabha).
  Future<String> nominate({required String personId, required String regularSabhaId}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/sanchalak/nominations'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'personId': personId, 'regularSabhaId': regularSabhaId}),
    );
    if (resp.statusCode == 200) {
      return (jsonDecode(resp.body) as Map<String, dynamic>)['nominationId'] as String;
    }
    apiError(resp, 'POST nominations', {
      403: (e) => NominationNotAuthorizedException(
          e.message('Only this Sabha\'s Sanchalak can nominate.')),
      409: (e) => AlreadyNominatedException(
          e.message('This Person is already nominated or selected.')),
      422: (e) => NominationRejectedException(
          e.message('This Person can\'t be nominated.')),
    }, fallback: SelectionApiException.new);
  }
}

/// A transport/unexpected-status failure from the nomination endpoint.
class SelectionApiException implements Exception {
  SelectionApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The caller is not the Regular Sabha's Sanchalak/Sah-Sanchalak (403).
class NominationNotAuthorizedException implements Exception {
  NominationNotAuthorizedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The Person already has an open nomination for this track or is already
/// selected (409).
class AlreadyNominatedException implements Exception {
  AlreadyNominatedException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A domain rule rejected the nomination (422): not on the Roster, the
/// demographic has no selective track, or no selective Sabha exists.
class NominationRejectedException implements Exception {
  NominationRejectedException(this.message);
  final String message;
  @override
  String toString() => message;
}
