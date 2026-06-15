//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class AuditLogBffControllerApi {
  AuditLogBffControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'GET /bff/audit-log' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] targetType:
  ///
  /// * [String] targetId:
  ///
  /// * [String] actorUserId:
  ///
  /// * [String] action:
  ///
  /// * [DateTime] from:
  ///
  /// * [DateTime] to:
  ///
  /// * [bool] proxyOnly:
  Future<Response> list1WithHttpInfo({ String? targetType, String? targetId, String? actorUserId, String? action, DateTime? from, DateTime? to, bool? proxyOnly, Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/bff/audit-log';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (targetType != null) {
      queryParams.addAll(_queryParams('', 'targetType', targetType));
    }
    if (targetId != null) {
      queryParams.addAll(_queryParams('', 'targetId', targetId));
    }
    if (actorUserId != null) {
      queryParams.addAll(_queryParams('', 'actorUserId', actorUserId));
    }
    if (action != null) {
      queryParams.addAll(_queryParams('', 'action', action));
    }
    if (from != null) {
      queryParams.addAll(_queryParams('', 'from', from));
    }
    if (to != null) {
      queryParams.addAll(_queryParams('', 'to', to));
    }
    if (proxyOnly != null) {
      queryParams.addAll(_queryParams('', 'proxyOnly', proxyOnly));
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
  /// * [String] targetType:
  ///
  /// * [String] targetId:
  ///
  /// * [String] actorUserId:
  ///
  /// * [String] action:
  ///
  /// * [DateTime] from:
  ///
  /// * [DateTime] to:
  ///
  /// * [bool] proxyOnly:
  Future<List<AuditEntry>?> list1({ String? targetType, String? targetId, String? actorUserId, String? action, DateTime? from, DateTime? to, bool? proxyOnly, Future<void>? abortTrigger, }) async {
    final response = await list1WithHttpInfo(targetType: targetType, targetId: targetId, actorUserId: actorUserId, action: action, from: from, to: to, proxyOnly: proxyOnly, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<AuditEntry>') as List)
        .cast<AuditEntry>()
        .toList(growable: false);

    }
    return null;
  }
}
