import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the Walk-in endpoints (Slice 7, issue #8). Search hits the
/// full Directory online (`GET /api/directory/walk-in-search`); recording a
/// Walk-in is online-only (`POST /api/occurrences/{id}/walk-ins`) — there is no
/// offline queue for Walk-ins (ADR-0007), so nothing here is ever deferred.
class WalkInApi {
  WalkInApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// Searches the Directory for a visitor by name or mobile, scoped server-side
  /// to the Occurrence's Kshetra. Each match carries the Person's current Home
  /// Sabha for the confirm sheet.
  Future<List<WalkInCandidate>> search({required String sabhaId, required String query}) async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/directory/walk-in-search')
          .replace(queryParameters: {'sabhaId': sabhaId, 'q': query}),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode != 200) {
      throw WalkInApiException('GET walk-in-search -> ${resp.statusCode}: ${resp.body}');
    }
    final decoded = jsonDecode(resp.body) as List<dynamic>;
    return decoded.map((e) => WalkInCandidate.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Records [personId] as a Walk-in at [occurrenceId]. The backend stores it
  /// with `markingType = WALK_IN`, always present, never touching Home Sabha.
  Future<void> recordWalkIn({required String occurrenceId, required String personId}) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/occurrences/$occurrenceId/walk-ins'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'personId': personId}),
    );
    if (resp.statusCode != 200) {
      throw WalkInApiException('POST walk-ins -> ${resp.statusCode}: ${resp.body}');
    }
  }
}

class WalkInApiException implements Exception {
  WalkInApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A Directory match offered when recording a Walk-in. [homeSabha] is the
/// Person's current Home Sabha (now away), shown on the confirm sheet.
class WalkInCandidate {
  WalkInCandidate({required this.personId, required this.fullName, required this.homeSabha});

  final String personId;
  final String fullName;
  final String homeSabha;

  factory WalkInCandidate.fromJson(Map<String, dynamic> json) {
    return WalkInCandidate(
      personId: json['personId'] as String,
      fullName: json['fullName'] as String,
      homeSabha: json['homeSabha'] as String? ?? '',
    );
  }
}
