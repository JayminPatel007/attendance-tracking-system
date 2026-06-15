//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class DirectoryBffControllerApi {
  DirectoryBffControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'GET /bff/directory/search' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] mobile:
  ///
  /// * [String] name:
  ///
  /// * [String] kshetraId:
  Future<Response> search1WithHttpInfo({ String? mobile, String? name, String? kshetraId, Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/directory/search';

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
  Future<Object?> search1({ String? mobile, String? name, String? kshetraId, Future<void>? abortTrigger, }) async {
    final response = await search1WithHttpInfo(mobile: mobile, name: name, kshetraId: kshetraId, abortTrigger: abortTrigger,);
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
}
