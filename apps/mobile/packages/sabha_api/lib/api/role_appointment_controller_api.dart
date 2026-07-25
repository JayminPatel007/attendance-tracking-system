//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class RoleAppointmentControllerApi {
  RoleAppointmentControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'POST /bff/appointments' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [AppointmentRequest] appointmentRequest (required):
  Future<Response> appointWithHttpInfo(AppointmentRequest appointmentRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/appointments';

    // ignore: prefer_final_locals
    Object? postBody = appointmentRequest;

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
  /// * [AppointmentRequest] appointmentRequest (required):
  Future<AppointmentResponse?> appoint(AppointmentRequest appointmentRequest, { Future<void>? abortTrigger, }) async {
    final response = await appointWithHttpInfo(appointmentRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'AppointmentResponse',) as AppointmentResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /bff/appointments/{id}/revoke' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] id (required):
  Future<Response> revokeWithHttpInfo(String id, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/appointments/{id}/revoke'
      .replaceAll('{id}', id);

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
  /// * [String] id (required):
  Future<void> revoke(String id, { Future<void>? abortTrigger, }) async {
    final response = await revokeWithHttpInfo(id, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }

  /// Performs an HTTP 'GET /bff/appointments/sah-nirdeshak-cap' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] kshetraId (required):
  ///
  /// * [String] demographic (required):
  Future<Response> sahNirdeshakCapWithHttpInfo(String kshetraId, String demographic, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/appointments/sah-nirdeshak-cap';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'kshetraId', kshetraId));
      queryParams.addAll(_queryParams('', 'demographic', demographic));

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
  /// * [String] kshetraId (required):
  ///
  /// * [String] demographic (required):
  Future<SahNirdeshakCapResponse?> sahNirdeshakCap(String kshetraId, String demographic, { Future<void>? abortTrigger, }) async {
    final response = await sahNirdeshakCapWithHttpInfo(kshetraId, demographic, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'SahNirdeshakCapResponse',) as SahNirdeshakCapResponse;
    
    }
    return null;
  }
}
