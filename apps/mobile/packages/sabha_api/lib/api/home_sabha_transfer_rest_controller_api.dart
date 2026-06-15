//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class HomeSabhaTransferRestControllerApi {
  HomeSabhaTransferRestControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'POST /api/home-sabha-transfers/{id}/confirm' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] id (required):
  ///
  /// * [ConfirmTransferRequest] confirmTransferRequest (required):
  Future<Response> confirmWithHttpInfo(String id, ConfirmTransferRequest confirmTransferRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/home-sabha-transfers/{id}/confirm'
      .replaceAll('{id}', id);

    // ignore: prefer_final_locals
    Object? postBody = confirmTransferRequest;

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
  /// * [String] id (required):
  ///
  /// * [ConfirmTransferRequest] confirmTransferRequest (required):
  Future<void> confirm(String id, ConfirmTransferRequest confirmTransferRequest, { Future<void>? abortTrigger, }) async {
    final response = await confirmWithHttpInfo(id, confirmTransferRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'POST /api/home-sabha-transfers' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [InitiateTransferRequest] initiateTransferRequest (required):
  Future<Response> initiateWithHttpInfo(InitiateTransferRequest initiateTransferRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/home-sabha-transfers';

    // ignore: prefer_final_locals
    Object? postBody = initiateTransferRequest;

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
  /// * [InitiateTransferRequest] initiateTransferRequest (required):
  Future<InitiateTransferResponse?> initiate(InitiateTransferRequest initiateTransferRequest, { Future<void>? abortTrigger, }) async {
    final response = await initiateWithHttpInfo(initiateTransferRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'InitiateTransferResponse',) as InitiateTransferResponse;
    
    }
    return null;
  }
}
