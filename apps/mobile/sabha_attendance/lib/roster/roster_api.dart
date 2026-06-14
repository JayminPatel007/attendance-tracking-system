import 'dart:convert';
import 'package:http/http.dart' as http;

import '../sync/pending_marking.dart';

/// Thin wrapper over the two backend endpoints the mobile needs in Slice 2:
/// GET /api/sanchalak/current-roster and POST /api/occurrences/{id}/markings.
class RosterApi {
  RosterApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  Future<CurrentRoster> fetch() async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/sanchalak/current-roster'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode != 200) {
      throw RosterApiException('GET current-roster -> ${resp.statusCode}: ${resp.body}');
    }
    return CurrentRoster.fromJson(jsonDecode(resp.body) as Map<String, dynamic>);
  }

  Future<void> mark({
    required String occurrenceId,
    required String personId,
    required bool present,
  }) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/occurrences/$occurrenceId/markings'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'personId': personId, 'present': present}),
    );
    if (resp.statusCode != 200) {
      throw RosterApiException('POST mark -> ${resp.statusCode}: ${resp.body}');
    }
  }

  /// Pushes a batch of locally-queued markings (ADR-0007). Returns the
  /// server-reported count of applied items. Throws [StaleRosterException]
  /// when the backend rejects the cached roster with code `ROSTER_STALE`.
  Future<int> sync({
    required DateTime rosterVersion,
    required List<PendingMarking> markings,
  }) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/sync'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'rosterVersion': rosterVersion.toUtc().toIso8601String(),
        'markings': markings
            .map((m) => {
                  'occurrenceId': m.occurrenceId,
                  'personId': m.personId,
                  'present': m.present,
                  'clientMarkedAt': m.clientMarkedAt.toUtc().toIso8601String(),
                })
            .toList(),
      }),
    );
    if (resp.statusCode == 200) {
      final body = jsonDecode(resp.body) as Map<String, dynamic>;
      return body['appliedCount'] as int;
    }
    if (resp.statusCode == 409) {
      try {
        final body = jsonDecode(resp.body) as Map<String, dynamic>;
        if (body['code'] == 'ROSTER_STALE') {
          throw StaleRosterException(body['detail'] as String? ?? 'roster stale');
        }
      } on FormatException {
        // fall through to generic
      }
    }
    throw RosterApiException('POST sync -> ${resp.statusCode}: ${resp.body}');
  }
}

class StaleRosterException implements Exception {
  StaleRosterException(this.message);
  final String message;
  @override
  String toString() => 'StaleRosterException: $message';
}

class RosterApiException implements Exception {
  RosterApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

class CurrentRoster {
  CurrentRoster({required this.occurrence, required this.roster, required this.rosterVersion});

  final OccurrenceView occurrence;
  final List<RosterEntry> roster;

  /// Server-stamped freshness token for this snapshot. Echoed back during
  /// sync so the backend can enforce the 7-day staleness gate (ADR-0007).
  final DateTime rosterVersion;

  factory CurrentRoster.fromJson(Map<String, dynamic> json) {
    return CurrentRoster(
      occurrence: OccurrenceView.fromJson(json['occurrence'] as Map<String, dynamic>),
      roster: (json['roster'] as List<dynamic>)
          .map((e) => RosterEntry.fromJson(e as Map<String, dynamic>))
          .toList(),
      rosterVersion: DateTime.parse(json['rosterVersion'] as String),
    );
  }
}

class OccurrenceView {
  OccurrenceView({
    required this.id,
    required this.date,
    required this.state,
    required this.sabhaId,
  });

  final String id;
  final String date;
  final String state;
  final String sabhaId;

  factory OccurrenceView.fromJson(Map<String, dynamic> json) {
    return OccurrenceView(
      id: json['id'] as String,
      date: json['date'] as String,
      state: json['state'] as String,
      sabhaId: json['sabhaId'] as String,
    );
  }
}

class RosterEntry {
  RosterEntry({required this.personId, required this.fullName, this.present});

  final String personId;
  final String fullName;
  final bool? present;

  factory RosterEntry.fromJson(Map<String, dynamic> json) {
    return RosterEntry(
      personId: json['personId'] as String,
      fullName: json['fullName'] as String,
      present: json['present'] as bool?,
    );
  }
}
