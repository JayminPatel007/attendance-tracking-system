import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the Occurrence-shaping surface (Slice 5, ADR-0001). Reads
/// the Sanchalak's currently-shapeable Occurrence and dispatches the four
/// Sanchalak-only actions. These are **online-only** (ADR-0007) — never queued
/// in the offline sync engine; a lost connection surfaces as an error the
/// screen shows, not a pending mutation.
class OccurrenceControlApi {
  OccurrenceControlApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// GET the Occurrence the Sanchalak can currently shape, or `null` when there
  /// is none (e.g. nothing scheduled).
  Future<ShapeableOccurrence?> fetch() async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/sanchalak/current-occurrence'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode == 404) return null;
    if (resp.statusCode != 200) {
      throw OccurrenceControlApiException('GET current-occurrence -> ${resp.statusCode}: ${resp.body}');
    }
    return ShapeableOccurrence.fromJson(jsonDecode(resp.body) as Map<String, dynamic>);
  }

  /// Cancel a Scheduled Occurrence. Reason is required (ADR-0001).
  Future<void> cancel(String occurrenceId, String reason) =>
      _action(occurrenceId, 'cancel', {'reason': reason});

  /// Revert a Cancelled Occurrence back to Scheduled. The backend rejects with
  /// 422 once the 24h grace past scheduled end has passed.
  Future<void> revert(String occurrenceId) => _action(occurrenceId, 'revert', null);

  /// Move this Occurrence to a new date/time without touching the standing
  /// schedule. Times are `HH:mm`.
  Future<void> reschedule(
    String occurrenceId, {
    required String date,
    required String startTime,
    required String endTime,
  }) =>
      _action(occurrenceId, 'reschedule', {'date': date, 'startTime': startTime, 'endTime': endTime});

  /// Set a one-off venue for this Occurrence only.
  Future<void> overrideVenue(String occurrenceId, String venue) =>
      _action(occurrenceId, 'venue-override', {'venue': venue});

  Future<void> _action(String occurrenceId, String path, Map<String, dynamic>? body) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/occurrences/$occurrenceId/$path'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        if (body != null) 'Content-Type': 'application/json',
      },
      body: body == null ? null : jsonEncode(body),
    );
    if (resp.statusCode == 200) return;
    if (resp.statusCode == 403) {
      throw OccurrenceForbiddenException(_messageOf(resp.body) ?? 'You are not allowed to perform this action.');
    }
    if (resp.statusCode == 422) {
      throw OccurrenceRuleException(_messageOf(resp.body) ?? 'That action is not allowed right now.');
    }
    throw OccurrenceControlApiException('POST $path -> ${resp.statusCode}: ${resp.body}');
  }

  /// Pulls the `message` field out of the uniform error body (ADR-0019), if any.
  String? _messageOf(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) return decoded['detail'] as String?;
    } on FormatException {
      // non-JSON body — fall back to the generic message
    }
    return null;
  }
}

class OccurrenceControlApiException implements Exception {
  OccurrenceControlApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The caller (e.g. a Sah-Sanchalak) is not authorized for this Sabha-shaping
/// action — the Authorization Engine returned 403 (ADR-0001).
class OccurrenceForbiddenException implements Exception {
  OccurrenceForbiddenException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A domain rule rejected the action (422) — e.g. the revert window has expired
/// or the Occurrence is no longer in a shapeable state.
class OccurrenceRuleException implements Exception {
  OccurrenceRuleException(this.message);
  final String message;
  @override
  String toString() => message;
}

class ShapeableOccurrence {
  ShapeableOccurrence({
    required this.id,
    required this.sabhaId,
    required this.date,
    required this.state,
    this.venueOverride,
    this.rescheduledDate,
    this.rescheduledStartTime,
    this.rescheduledEndTime,
  });

  final String id;
  final String sabhaId;
  final String date;
  final String state;
  final String? venueOverride;
  final String? rescheduledDate;
  final String? rescheduledStartTime;
  final String? rescheduledEndTime;

  factory ShapeableOccurrence.fromJson(Map<String, dynamic> json) {
    return ShapeableOccurrence(
      id: json['id'] as String,
      sabhaId: json['sabhaId'] as String,
      date: json['date'] as String,
      state: json['state'] as String,
      venueOverride: json['venueOverride'] as String?,
      rescheduledDate: json['rescheduledDate'] as String?,
      rescheduledStartTime: json['rescheduledStartTime'] as String?,
      rescheduledEndTime: json['rescheduledEndTime'] as String?,
    );
  }
}
