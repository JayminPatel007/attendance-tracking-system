import 'package:http/http.dart' as http;
import 'package:sabha_api/api.dart' as api;

/// Thin wrapper over the Walk-in endpoints (Slice 7, issue #8). Search hits the
/// full Directory online (`GET /api/directory/walk-in-search`); recording a
/// Walk-in is online-only (`POST /api/occurrences/{id}/walk-ins`) — there is no
/// offline queue for Walk-ins (ADR-0007), so nothing here is ever deferred.
///
/// Delegates to the generated typed client (issue #73): the response shapes are
/// the generated `WalkInCandidate` / `WalkInRequest` models, so this client can
/// never silently drift from the backend the way the old hand-rolled parser did
/// (issue #75). The generated [api.ApiException] is bridged to the feature's
/// [WalkInApiException] so callers are unchanged.
class WalkInApi {
  WalkInApi({required String baseUrl, required String accessToken, http.Client? client}) {
    final apiClient = _apiClient(baseUrl, accessToken, client);
    _directory = api.PersonDirectoryRestControllerApi(apiClient);
    _occurrences = api.AttendanceRestControllerApi(apiClient);
  }

  late final api.PersonDirectoryRestControllerApi _directory;
  late final api.AttendanceRestControllerApi _occurrences;

  static api.ApiClient _apiClient(String baseUrl, String accessToken, http.Client? client) {
    final apiClient = api.ApiClient(
      basePath: baseUrl,
      authentication: api.HttpBearerAuth()..accessToken = accessToken,
    );
    if (client != null) {
      apiClient.client = client;
    }
    return apiClient;
  }

  /// Searches the Directory for a visitor by name or mobile, scoped server-side
  /// to the Occurrence's Kshetra. Each match carries all of the Person's current
  /// Home Sabha kinds for the confirm sheet.
  Future<List<WalkInCandidate>> search({required String sabhaId, required String query}) async {
    try {
      final results = await _directory.walkInSearch(sabhaId, query);
      return (results ?? const []).map(WalkInCandidate._fromApi).toList();
    } on api.ApiException catch (e) {
      throw WalkInApiException('GET walk-in-search -> ${e.code}: ${e.message}');
    }
  }

  /// Records [personId] as a Walk-in at [occurrenceId]. The backend stores it
  /// with `markingType = WALK_IN`, always present, never touching Home Sabha.
  Future<void> recordWalkIn({required String occurrenceId, required String personId}) async {
    try {
      await _occurrences.walkIn(occurrenceId, api.WalkInRequest(personId: personId));
    } on api.ApiException catch (e) {
      throw WalkInApiException('POST walk-ins -> ${e.code}: ${e.message}');
    }
  }
}

class WalkInApiException implements Exception {
  WalkInApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// A Directory match offered when recording a Walk-in. [homeSabhas] carries all
/// of the Person's current Home Sabha kinds (now away) — a Person has one per
/// Sabha kind they qualify for (typically their demographic Sabha + Sanyukta,
/// CONTEXT.md) — shown on the confirm sheet. Elements are `sabha_kind` strings.
class WalkInCandidate {
  WalkInCandidate({required this.personId, required this.fullName, this.homeSabhas = const []});

  final String personId;
  final String fullName;
  final List<String> homeSabhas;

  /// The Home Sabha kinds as a single line for the confirm sheet, e.g.
  /// `REGULAR_BAAL, REGULAR_SANYUKTA`. Empty when the Person has none.
  String get homeSabhasLabel => homeSabhas.join(', ');

  factory WalkInCandidate._fromApi(api.WalkInCandidate c) => WalkInCandidate(
        personId: c.personId,
        fullName: c.fullName,
        homeSabhas: c.homeSabhas,
      );
}
