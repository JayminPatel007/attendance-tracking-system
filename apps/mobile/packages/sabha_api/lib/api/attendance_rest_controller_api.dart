//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class AttendanceRestControllerApi {
  AttendanceRestControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'POST /api/sync' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [SyncRequest] syncRequest (required):
  Future<Response> callSyncWithHttpInfo(SyncRequest syncRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sync';

    // ignore: prefer_final_locals
    Object? postBody = syncRequest;

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
  /// * [SyncRequest] syncRequest (required):
  Future<SyncResponse?> callSync(SyncRequest syncRequest, { Future<void>? abortTrigger, }) async {
    final response = await callSyncWithHttpInfo(syncRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'SyncResponse',) as SyncResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/cancel' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [CancelRequest] cancelRequest (required):
  Future<Response> cancel1WithHttpInfo(String occurrenceId, CancelRequest cancelRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/cancel'
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
  Future<void> cancel1(String occurrenceId, CancelRequest cancelRequest, { Future<void>? abortTrigger, }) async {
    final response = await cancel1WithHttpInfo(occurrenceId, cancelRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'POST /api/sabhas/{sabhaId}/occurrences' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] sabhaId (required):
  ///
  /// * [CreateOccurrenceRequest] createOccurrenceRequest (required):
  Future<Response> createMonthlyOccurrenceWithHttpInfo(String sabhaId, CreateOccurrenceRequest createOccurrenceRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sabhas/{sabhaId}/occurrences'
      .replaceAll('{sabhaId}', sabhaId);

    // ignore: prefer_final_locals
    Object? postBody = createOccurrenceRequest;

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
  /// * [String] sabhaId (required):
  ///
  /// * [CreateOccurrenceRequest] createOccurrenceRequest (required):
  Future<CreatedOccurrenceResponse?> createMonthlyOccurrence(String sabhaId, CreateOccurrenceRequest createOccurrenceRequest, { Future<void>? abortTrigger, }) async {
    final response = await createMonthlyOccurrenceWithHttpInfo(sabhaId, createOccurrenceRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'CreatedOccurrenceResponse',) as CreatedOccurrenceResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/sanchalak/current-occurrence' operation and returns the [Response].
  Future<Response> currentOccurrenceWithHttpInfo({ Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sanchalak/current-occurrence';

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

  Future<CurrentOccurrence?> currentOccurrence({ Future<void>? abortTrigger, }) async {
    final response = await currentOccurrenceWithHttpInfo(abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'CurrentOccurrence',) as CurrentOccurrence;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/sanchalak/current-roster' operation and returns the [Response].
  Future<Response> currentRosterWithHttpInfo({ Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sanchalak/current-roster';

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

  Future<CurrentRoster?> currentRoster({ Future<void>? abortTrigger, }) async {
    final response = await currentRosterWithHttpInfo(abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'CurrentRoster',) as CurrentRoster;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/markings' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [MarkRequest] markRequest (required):
  Future<Response> markWithHttpInfo(String occurrenceId, MarkRequest markRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/markings'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = markRequest;

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
  /// * [MarkRequest] markRequest (required):
  Future<void> mark(String occurrenceId, MarkRequest markRequest, { Future<void>? abortTrigger, }) async {
    final response = await markWithHttpInfo(occurrenceId, markRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'GET /api/sabhas/{sabhaId}/monthly-compliance' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] sabhaId (required):
  Future<Response> monthlyComplianceWithHttpInfo(String sabhaId, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sabhas/{sabhaId}/monthly-compliance'
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
  Future<MonthlyComplianceResponse?> monthlyCompliance(String sabhaId, { Future<void>? abortTrigger, }) async {
    final response = await monthlyComplianceWithHttpInfo(sabhaId, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MonthlyComplianceResponse',) as MonthlyComplianceResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/sanchalak/monthly-sabhas' operation and returns the [Response].
  Future<Response> monthlySabhasWithHttpInfo({ Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/sanchalak/monthly-sabhas';

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

  Future<List<MonthlySabha>?> monthlySabhas({ Future<void>? abortTrigger, }) async {
    final response = await monthlySabhasWithHttpInfo(abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<MonthlySabha>') as List)
        .cast<MonthlySabha>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/reschedule' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [RescheduleRequest] rescheduleRequest (required):
  Future<Response> reschedule1WithHttpInfo(String occurrenceId, RescheduleRequest rescheduleRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/reschedule'
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
  Future<void> reschedule1(String occurrenceId, RescheduleRequest rescheduleRequest, { Future<void>? abortTrigger, }) async {
    final response = await reschedule1WithHttpInfo(occurrenceId, rescheduleRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/revert' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  Future<Response> revertWithHttpInfo(String occurrenceId, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/revert'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


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
  Future<void> revert(String occurrenceId, { Future<void>? abortTrigger, }) async {
    final response = await revertWithHttpInfo(occurrenceId, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/venue-override' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [VenueOverrideRequest] venueOverrideRequest (required):
  Future<Response> venueOverride1WithHttpInfo(String occurrenceId, VenueOverrideRequest venueOverrideRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/venue-override'
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
  Future<void> venueOverride1(String occurrenceId, VenueOverrideRequest venueOverrideRequest, { Future<void>? abortTrigger, }) async {
    final response = await venueOverride1WithHttpInfo(occurrenceId, venueOverrideRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'POST /api/occurrences/{occurrenceId}/walk-ins' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] occurrenceId (required):
  ///
  /// * [WalkInRequest] walkInRequest (required):
  Future<Response> walkInWithHttpInfo(String occurrenceId, WalkInRequest walkInRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/occurrences/{occurrenceId}/walk-ins'
      .replaceAll('{occurrenceId}', occurrenceId);

    // ignore: prefer_final_locals
    Object? postBody = walkInRequest;

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
  /// * [WalkInRequest] walkInRequest (required):
  Future<void> walkIn(String occurrenceId, WalkInRequest walkInRequest, { Future<void>? abortTrigger, }) async {
    final response = await walkInWithHttpInfo(occurrenceId, walkInRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }
}
