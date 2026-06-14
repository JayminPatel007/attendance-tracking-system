import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the monthly-ad-hoc surface (Slice 12, ADR-0012). A BSS/YSS
/// Sanchalak lists the monthly Sabhas they preside over — each carrying the
/// compliance nudge flag — and manually creates this month's Occurrence on a
/// date they pick, since monthly Sabhas have no standing schedule to
/// materialize from. These are online-only (ADR-0007): a lost connection
/// surfaces as an error the screen shows, never a queued mutation.
class MonthlyOccurrenceApi {
  MonthlyOccurrenceApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// GET the monthly Sabhas the caller is Sanchalak of, with their nudge flags.
  Future<List<MonthlySabha>> fetchSabhas() async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/sanchalak/monthly-sabhas'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode != 200) {
      throw MonthlyOccurrenceApiException('GET monthly-sabhas -> ${resp.statusCode}: ${resp.body}');
    }
    final list = jsonDecode(resp.body) as List<dynamic>;
    return list.map((e) => MonthlySabha.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Create this month's Occurrence on the picked date/time/venue. Times are
  /// `HH:mm`. Returns the new Occurrence id; it starts in `Scheduled`.
  Future<String> create(
    String sabhaId, {
    required String date,
    required String startTime,
    required String endTime,
    required String venue,
  }) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/sabhas/$sabhaId/occurrences'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'date': date, 'startTime': startTime, 'endTime': endTime, 'venue': venue}),
    );
    if (resp.statusCode == 201) {
      return (jsonDecode(resp.body) as Map<String, dynamic>)['occurrenceId'] as String;
    }
    if (resp.statusCode == 403) {
      throw MonthlyOccurrenceForbiddenException(
          _messageOf(resp.body) ?? 'Only the Sabha\'s Sanchalak can create this Occurrence.');
    }
    if (resp.statusCode == 422) {
      throw MonthlyOccurrenceRuleException(
          _messageOf(resp.body) ?? 'That Occurrence can\'t be created right now.');
    }
    throw MonthlyOccurrenceApiException('POST occurrences -> ${resp.statusCode}: ${resp.body}');
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

class MonthlyOccurrenceApiException implements Exception {
  MonthlyOccurrenceApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The caller is not the Sabha's Sanchalak — the Authorization Engine returned
/// 403 (ADR-0012). A Sah-Sanchalak is excluded from monthly creation (ADR-0001).
class MonthlyOccurrenceForbiddenException implements Exception {
  MonthlyOccurrenceForbiddenException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A domain rule rejected the create (422) — e.g. the Sabha is weekly-recurring
/// and materializes automatically, or the Sabha is unknown.
class MonthlyOccurrenceRuleException implements Exception {
  MonthlyOccurrenceRuleException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A monthly-ad-hoc Sabha the caller presides over (Slice 12). [needsOccurrence]
/// drives the soft compliance nudge — true when this month has no Occurrence yet
/// and the month is past its midpoint.
class MonthlySabha {
  MonthlySabha({
    required this.sabhaId,
    required this.sabhaKind,
    required this.standingVenue,
    required this.needsOccurrence,
  });

  final String sabhaId;
  final String sabhaKind;
  final String standingVenue;
  final bool needsOccurrence;

  factory MonthlySabha.fromJson(Map<String, dynamic> json) {
    return MonthlySabha(
      sabhaId: json['sabhaId'] as String,
      sabhaKind: json['sabhaKind'] as String,
      standingVenue: json['standingVenue'] as String,
      needsOccurrence: json['needsOccurrence'] as bool,
    );
  }
}
