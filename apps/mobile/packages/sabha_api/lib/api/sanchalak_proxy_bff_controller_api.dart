//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class SanchalakProxyBffControllerApi {
  SanchalakProxyBffControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'POST /bff/proxy/occurrences/{occurrenceId}/cancel' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [CancelRequest] cancelRequest (required):
  Future<Response> cancelWithHttpInfo(String occurrenceId, CancelRequest cancelRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/proxy/occurrences/{occurrenceId}/cancel'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = cancelRequest;

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
  /// * [CancelRequest] cancelRequest (required):
  Future<void> cancel(String occurrenceId, CancelRequest cancelRequest, { Future<void>? abortTrigger, }) async {
    final response = await cancelWithHttpInfo(occurrenceId, cancelRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'GET /bff/proxy/sabhas/{sabhaId}/occurrences' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] sabhaId (required):
  Future<Response> occurrencesWithHttpInfo(String sabhaId, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/proxy/sabhas/{sabhaId}/occurrences'
      .replaceAll('{sabhaId}', sabhaId);

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

  /// Parameters:
  ///
  /// * [String] sabhaId (required):
  Future<List<ProxyOccurrenceItem>?> occurrences(String sabhaId, { Future<void>? abortTrigger, }) async {
    final response = await occurrencesWithHttpInfo(sabhaId, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<ProxyOccurrenceItem>') as List)
        .cast<ProxyOccurrenceItem>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'POST /bff/proxy/occurrences/{occurrenceId}/reschedule' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [RescheduleRequest] rescheduleRequest (required):
  Future<Response> rescheduleWithHttpInfo(String occurrenceId, RescheduleRequest rescheduleRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/proxy/occurrences/{occurrenceId}/reschedule'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = rescheduleRequest;

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
  /// * [RescheduleRequest] rescheduleRequest (required):
  Future<void> reschedule(String occurrenceId, RescheduleRequest rescheduleRequest, { Future<void>? abortTrigger, }) async {
    final response = await rescheduleWithHttpInfo(occurrenceId, rescheduleRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'GET /bff/proxy/sabhas' operation and returns the [Response].
  Future<Response> sabhasWithHttpInfo({ Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/proxy/sabhas';

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

  Future<List<ProxySabhaListItem>?> sabhas({ Future<void>? abortTrigger, }) async {
    final response = await sabhasWithHttpInfo(abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<ProxySabhaListItem>') as List)
        .cast<ProxySabhaListItem>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'POST /bff/proxy/occurrences/{occurrenceId}/venue-override' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [VenueOverrideRequest] venueOverrideRequest (required):
  Future<Response> venueOverrideWithHttpInfo(String occurrenceId, VenueOverrideRequest venueOverrideRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/proxy/occurrences/{occurrenceId}/venue-override'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = venueOverrideRequest;

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
  /// * [VenueOverrideRequest] venueOverrideRequest (required):
  Future<void> venueOverride(String occurrenceId, VenueOverrideRequest venueOverrideRequest, { Future<void>? abortTrigger, }) async {
    final response = await venueOverrideWithHttpInfo(occurrenceId, venueOverrideRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }
}
