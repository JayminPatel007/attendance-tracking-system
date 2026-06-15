//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class OccurrenceReopenBffControllerApi {
  OccurrenceReopenBffControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'GET /bff/occurrences' operation and returns the [Response].
  Future<Response> listWithHttpInfo({ Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/occurrences';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
      abortTrigger: abortTrigger,
    );
  }

  Future<List<ReopenListItem>?> list({ Future<void>? abortTrigger, }) async {
    final response = await listWithHttpInfo(abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<ReopenListItem>') as List)
        .cast<ReopenListItem>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'POST /bff/occurrences/{occurrenceId}/reopen' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [ReopenRequest] reopenRequest (required):
  Future<Response> reopenWithHttpInfo(String occurrenceId, ReopenRequest reopenRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/occurrences/{occurrenceId}/reopen'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = reopenRequest;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['application/json'];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
      abortTrigger: abortTrigger,
    );
  }

  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [ReopenRequest] reopenRequest (required):
  Future<void> reopen(String occurrenceId, ReopenRequest reopenRequest, { Future<void>? abortTrigger, }) async {
    final response = await reopenWithHttpInfo(occurrenceId, reopenRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }
}
