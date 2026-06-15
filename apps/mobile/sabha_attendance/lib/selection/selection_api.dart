import 'package:http/http.dart' as http;
import 'package:sabha_api/api.dart';

import '../api/api_error.dart';

/// Thin wrapper over the mobile BSS/YSS nomination endpoint (Slice 16, ADR-0006).
/// The Regular Sanchalak nominates a Person from their Roster for the selective
/// track; the selective Sabha is derived server-side. Online-only — a nomination
/// is never queued offline.
///
/// Delegates to the generated typed client ([SelectionRestControllerApi], issue
/// #73): the request/response shapes are the generated `NominateRequest` /
/// `NominateResponse` models. Status-to-exception mapping stays on the shared
/// [apiError] seam (#67) — the generated client's [ApiException] is bridged back
/// to an [http.Response] so feature error handling is unchanged.
class SelectionApi {
  SelectionApi({required String baseUrl, required String accessToken, http.Client? client})
      : _api = SelectionRestControllerApi(_apiClient(baseUrl, accessToken, client));

  final SelectionRestControllerApi _api;

  static ApiClient _apiClient(String baseUrl, String accessToken, http.Client? client) {
    final apiClient = ApiClient(
      basePath: baseUrl,
      authentication: HttpBearerAuth()..accessToken = accessToken,
    );
    if (client != null) {
      apiClient.client = client;
    }
    return apiClient;
  }

  /// Nominate the Roster Person for the selective track. Returns the new
  /// nomination id. A `403` means the caller is not the Sabha's Sanchalak; a
  /// `409` that the Person already has an open nomination or is already selected;
  /// a `422` a domain rejection (not on roster, no selective track/Sabha).
  Future<String> nominate({required String personId, required String regularSabhaId}) async {
    try {
      final response = await _api.nominate(
        NominateRequest(personId: personId, regularSabhaId: regularSabhaId),
      );
      return response!.nominationId!;
    } on ApiException catch (e) {
      apiError(http.Response(e.message ?? '', e.code), 'POST nominations', {
        403: (err) => NominationNotAuthorizedException(
            err.message('Only this Sabha\'s Sanchalak can nominate.')),
        409: (err) => AlreadyNominatedException(
            err.message('This Person is already nominated or selected.')),
        422: (err) => NominationRejectedException(
            err.message('This Person can\'t be nominated.')),
      }, fallback: SelectionApiException.new);
    }
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
