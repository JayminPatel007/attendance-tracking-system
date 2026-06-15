//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class PersonDirectoryRestControllerApi {
  PersonDirectoryRestControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'POST /api/directory/persons' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [AddPersonRequest] addPersonRequest (required):
  Future<Response> addWithHttpInfo(AddPersonRequest addPersonRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/directory/persons';

    // ignore: prefer_final_locals
    Object? postBody = addPersonRequest;

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
  /// * [AddPersonRequest] addPersonRequest (required):
  Future<AddPersonResponse?> add(AddPersonRequest addPersonRequest, { Future<void>? abortTrigger, }) async {
    final response = await addWithHttpInfo(addPersonRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'AddPersonResponse',) as AddPersonResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/directory/persons/{id}' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] id (required):
  Future<Response> detailWithHttpInfo(String id, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/directory/persons/{id}'
      .replaceAll('{id}', id);

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
  /// * [String] id (required):
  Future<PersonResponse?> detail(String id, { Future<void>? abortTrigger, }) async {
    final response = await detailWithHttpInfo(id, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'PersonResponse',) as PersonResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/directory/persons' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] mobile:
  ///
  /// * [String] name:
  ///
  /// * [String] kshetraId:
  Future<Response> searchWithHttpInfo({ String? mobile, String? name, String? kshetraId, Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/directory/persons';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (mobile != null) {
      queryParams.addAll(_queryParams('', 'mobile', mobile));
    }
    if (name != null) {
      queryParams.addAll(_queryParams('', 'name', name));
    }
    if (kshetraId != null) {
      queryParams.addAll(_queryParams('', 'kshetraId', kshetraId));
    }

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
  /// * [String] mobile:
  ///
  /// * [String] name:
  ///
  /// * [String] kshetraId:
  Future<Object?> search({ String? mobile, String? name, String? kshetraId, Future<void>? abortTrigger, }) async {
    final response = await searchWithHttpInfo(mobile: mobile, name: name, kshetraId: kshetraId, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/directory/walk-in-search' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] sabhaId (required):
  ///
  /// * [String] q (required):
  Future<Response> walkInSearchWithHttpInfo(String sabhaId, String q, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/directory/walk-in-search';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'sabhaId', sabhaId));
      queryParams.addAll(_queryParams('', 'q', q));

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
  ///
  /// * [String] q (required):
  Future<List<WalkInCandidate>?> walkInSearch(String sabhaId, String q, { Future<void>? abortTrigger, }) async {
    final response = await walkInSearchWithHttpInfo(sabhaId, q, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<WalkInCandidate>') as List)
        .cast<WalkInCandidate>()
        .toList(growable: false);

    }
    return null;
  }
}
