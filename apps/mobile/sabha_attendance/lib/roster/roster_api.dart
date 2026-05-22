import 'dart:convert';
import 'package:http/http.dart' as http;

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
}

class RosterApiException implements Exception {
  RosterApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

class CurrentRoster {
  CurrentRoster({required this.occurrence, required this.roster});

  final OccurrenceView occurrence;
  final List<RosterEntry> roster;

  factory CurrentRoster.fromJson(Map<String, dynamic> json) {
    return CurrentRoster(
      occurrence: OccurrenceView.fromJson(json['occurrence'] as Map<String, dynamic>),
      roster: (json['roster'] as List<dynamic>)
          .map((e) => RosterEntry.fromJson(e as Map<String, dynamic>))
          .toList(),
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
