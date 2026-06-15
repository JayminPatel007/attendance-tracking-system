//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ApiClient {
  ApiClient({this.basePath = 'http://localhost', this.authentication,});

  final String basePath;
  final Authentication? authentication;

  var _client = Client();
  final _defaultHeaderMap = <String, String>{};

  /// Returns the current HTTP [Client] instance to use in this class.
  ///
  /// The return value is guaranteed to never be null.
  Client get client => _client;

  /// Requests to use a new HTTP [Client] in this class.
  set client(Client newClient) {
    _client = newClient;
  }

  Map<String, String> get defaultHeaderMap => _defaultHeaderMap;

  void addDefaultHeader(String key, String value) {
     _defaultHeaderMap[key] = value;
  }

  // We don't use a Map<String, String> for queryParams.
  // If collectionFormat is 'multi', a key might appear multiple times.
  Future<Response> invokeAPI(
    String path,
    String method,
    List<QueryParam> queryParams,
    Object? body,
    Map<String, String> headerParams,
    Map<String, String> formParams,
    String? contentType, {
    Future<void>? abortTrigger,
  }) async {
    await authentication?.applyToParams(queryParams, headerParams);

    headerParams.addAll(_defaultHeaderMap);
    if (contentType != null) {
      headerParams['Content-Type'] = contentType;
    }

    final urlEncodedQueryParams = queryParams.map((param) => '$param');
    final queryString = urlEncodedQueryParams.isNotEmpty ? '?${urlEncodedQueryParams.join('&')}' : '';
    final uri = Uri.parse('$basePath$path$queryString');

    try {
      // Special case for uploading a single file which isn't a 'multipart/form-data'.
      if (
        body is MultipartFile && (contentType == null ||
        !contentType.toLowerCase().startsWith('multipart/form-data'))
      ) {
        final request = AbortableStreamedRequest(method, uri, abortTrigger: abortTrigger);
        request.headers.addAll(headerParams);
        request.contentLength = body.length;
        body.finalize().listen(
          request.sink.add,
          onDone: request.sink.close,
          // ignore: avoid_types_on_closure_parameters
          onError: (Object error, StackTrace trace) => request.sink.close(),
          cancelOnError: true,
        );
        final response = await _client.send(request);
        return Response.fromStream(response);
      }

      if (body is MultipartRequest) {
        final request = AbortableMultipartRequest(method, uri, abortTrigger: abortTrigger);
        request.fields.addAll(body.fields);
        request.files.addAll(body.files);
        request.headers.addAll(body.headers);
        request.headers.addAll(headerParams);
        final response = await _client.send(request);
        return Response.fromStream(response);
      }

      final msgBody = contentType == 'application/x-www-form-urlencoded'
        ? formParams
        : await serializeAsync(body);
      final nullableHeaderParams = headerParams.isEmpty ? null : headerParams;

      final request = AbortableRequest(method, uri, abortTrigger: abortTrigger);
      if (nullableHeaderParams != null) {
        request.headers.addAll(nullableHeaderParams);
      }
      if (msgBody is String && msgBody.isNotEmpty) {
        request.body = msgBody;
      } else if (msgBody is List<int> && msgBody.isNotEmpty) {
        request.bodyBytes = msgBody;
      } else if (msgBody is Map<String, String>) {
        request.bodyFields = msgBody;
      }
      final response = await _client.send(request);
      return Response.fromStream(response);
    } on SocketException catch (error, trace) {
      throw ApiException.withInner(
        HttpStatus.badRequest,
        'Socket operation failed: $method $path',
        error,
        trace,
      );
    } on TlsException catch (error, trace) {
      throw ApiException.withInner(
        HttpStatus.badRequest,
        'TLS/SSL communication failed: $method $path',
        error,
        trace,
      );
    } on IOException catch (error, trace) {
      throw ApiException.withInner(
        HttpStatus.badRequest,
        'I/O operation failed: $method $path',
        error,
        trace,
      );
    } on ClientException catch (error, trace) {
      throw ApiException.withInner(
        HttpStatus.badRequest,
        'HTTP connection failed: $method $path',
        error,
        trace,
      );
    } on Exception catch (error, trace) {
      throw ApiException.withInner(
        HttpStatus.badRequest,
        'Exception occurred: $method $path',
        error,
        trace,
      );
    }
  }

  Future<dynamic> deserializeAsync(String value, String targetType, {bool growable = false,}) async =>
    // ignore: deprecated_member_use_from_same_package
    deserialize(value, targetType, growable: growable);

  @Deprecated('Scheduled for removal in OpenAPI Generator 6.x. Use deserializeAsync() instead.')
  dynamic deserialize(String value, String targetType, {bool growable = false,}) {
    // Remove all spaces. Necessary for regular expressions as well.
    targetType = targetType.replaceAll(' ', ''); // ignore: parameter_assignments

    // If the expected target type is String, nothing to do...
    return targetType == 'String'
      ? value
      : fromJson(json.decode(value), targetType, growable: growable);
  }

  // ignore: deprecated_member_use_from_same_package
  Future<String> serializeAsync(Object? value) async => serialize(value);

  @Deprecated('Scheduled for removal in OpenAPI Generator 6.x. Use serializeAsync() instead.')
  String serialize(Object? value) => value == null ? '' : json.encode(value);

  /// Returns a native instance of an OpenAPI class matching the [specified type][targetType].
  static dynamic fromJson(dynamic value, String targetType, {bool growable = false,}) {
    try {
      switch (targetType) {
        case 'String':
          return value is String ? value : value.toString();
        case 'int':
          return value is int ? value : int.parse('$value');
        case 'double':
          return value is double ? value : double.parse('$value');
        case 'bool':
          if (value is bool) {
            return value;
          }
          final valueString = '$value'.toLowerCase();
          return valueString == 'true' || valueString == '1';
        case 'DateTime':
          return value is DateTime ? value : DateTime.tryParse(value);
        case 'AddPersonRequest':
          return AddPersonRequest.fromJson(value);
        case 'AddPersonResponse':
          return AddPersonResponse.fromJson(value);
        case 'AppointeePayload':
          return AppointeePayload.fromJson(value);
        case 'AppointerContact':
          return AppointerContact.fromJson(value);
        case 'AppointmentRequest':
          return AppointmentRequest.fromJson(value);
        case 'AppointmentResponse':
          return AppointmentResponse.fromJson(value);
        case 'AuditEntry':
          return AuditEntry.fromJson(value);
        case 'CancelRequest':
          return CancelRequest.fromJson(value);
        case 'CandidateRow':
          return CandidateRow.fromJson(value);
        case 'ChooseCityRequest':
          return ChooseCityRequest.fromJson(value);
        case 'CityChip':
          return CityChip.fromJson(value);
        case 'CityOption':
          return CityOption.fromJson(value);
        case 'CityView':
          return CityView.fromJson(value);
        case 'CompletePayload':
          return CompletePayload.fromJson(value);
        case 'ConfirmTransferRequest':
          return ConfirmTransferRequest.fromJson(value);
        case 'CreateCityRequest':
          return CreateCityRequest.fromJson(value);
        case 'CreateKshetraRequest':
          return CreateKshetraRequest.fromJson(value);
        case 'CreateOccurrenceRequest':
          return CreateOccurrenceRequest.fromJson(value);
        case 'CreateSabhaKindRequest':
          return CreateSabhaKindRequest.fromJson(value);
        case 'CreateZoneRequest':
          return CreateZoneRequest.fromJson(value);
        case 'CreatedOccurrenceResponse':
          return CreatedOccurrenceResponse.fromJson(value);
        case 'CreatedResponse':
          return CreatedResponse.fromJson(value);
        case 'CurrentOccurrence':
          return CurrentOccurrence.fromJson(value);
        case 'CurrentRoster':
          return CurrentRoster.fromJson(value);
        case 'DashboardOverview':
          return DashboardOverview.fromJson(value);
        case 'DefineSabhaRequest':
          return DefineSabhaRequest.fromJson(value);
        case 'DeselectRequest':
          return DeselectRequest.fromJson(value);
        case 'InitiateTransferRequest':
          return InitiateTransferRequest.fromJson(value);
        case 'InitiateTransferResponse':
          return InitiateTransferResponse.fromJson(value);
        case 'Kpis':
          return Kpis.fromJson(value);
        case 'Kshetra':
          return Kshetra.fromJson(value);
        case 'KshetraView':
          return KshetraView.fromJson(value);
        case 'MarkRequest':
          return MarkRequest.fromJson(value);
        case 'MarkingItem':
          return MarkingItem.fromJson(value);
        case 'MonthlyComplianceResponse':
          return MonthlyComplianceResponse.fromJson(value);
        case 'MonthlySabha':
          return MonthlySabha.fromJson(value);
        case 'NameCandidate':
          return NameCandidate.fromJson(value);
        case 'NewPersonPayload':
          return NewPersonPayload.fromJson(value);
        case 'NominateRequest':
          return NominateRequest.fromJson(value);
        case 'NominateResponse':
          return NominateResponse.fromJson(value);
        case 'OccurrenceView':
          return OccurrenceView.fromJson(value);
        case 'PendingNominationItem':
          return PendingNominationItem.fromJson(value);
        case 'PersonResponse':
          return PersonResponse.fromJson(value);
        case 'ProxyOccurrenceItem':
          return ProxyOccurrenceItem.fromJson(value);
        case 'ProxySabhaListItem':
          return ProxySabhaListItem.fromJson(value);
        case 'ReissueRequest':
          return ReissueRequest.fromJson(value);
        case 'RejectRequest':
          return RejectRequest.fromJson(value);
        case 'ReopenListItem':
          return ReopenListItem.fromJson(value);
        case 'ReopenRequest':
          return ReopenRequest.fromJson(value);
        case 'RequestPayload':
          return RequestPayload.fromJson(value);
        case 'RequestResponse':
          return RequestResponse.fromJson(value);
        case 'RescheduleRequest':
          return RescheduleRequest.fromJson(value);
        case 'RosterEntry':
          return RosterEntry.fromJson(value);
        case 'Sabha':
          return Sabha.fromJson(value);
        case 'SabhaDefinitionResponse':
          return SabhaDefinitionResponse.fromJson(value);
        case 'SabhaKindView':
          return SabhaKindView.fromJson(value);
        case 'SabhaTree':
          return SabhaTree.fromJson(value);
        case 'SelectedPersonItem':
          return SelectedPersonItem.fromJson(value);
        case 'SyncRequest':
          return SyncRequest.fromJson(value);
        case 'SyncResponse':
          return SyncResponse.fromJson(value);
        case 'Thresholds':
          return Thresholds.fromJson(value);
        case 'ThresholdsRequest':
          return ThresholdsRequest.fromJson(value);
        case 'VenueOverrideRequest':
          return VenueOverrideRequest.fromJson(value);
        case 'VerifyPayload':
          return VerifyPayload.fromJson(value);
        case 'VerifyResponse':
          return VerifyResponse.fromJson(value);
        case 'WalkInCandidate':
          return WalkInCandidate.fromJson(value);
        case 'WalkInRequest':
          return WalkInRequest.fromJson(value);
        case 'WebSessionResponse':
          return WebSessionResponse.fromJson(value);
        case 'WhoAmIResponse':
          return WhoAmIResponse.fromJson(value);
        case 'WhoAppointedMeResponse':
          return WhoAppointedMeResponse.fromJson(value);
        case 'Zone':
          return Zone.fromJson(value);
        case 'ZoneView':
          return ZoneView.fromJson(value);
        default:
          dynamic match;
          if (value is List && (match = _regList.firstMatch(targetType)?.group(1)) != null) {
            return value
              .map<dynamic>((dynamic v) => fromJson(v, match, growable: growable,))
              .toList(growable: growable);
          }
          if (value is Set && (match = _regSet.firstMatch(targetType)?.group(1)) != null) {
            return value
              .map<dynamic>((dynamic v) => fromJson(v, match, growable: growable,))
              .toSet();
          }
          if (value is Map && (match = _regMap.firstMatch(targetType)?.group(1)) != null) {
            return Map<String, dynamic>.fromIterables(
              value.keys.cast<String>(),
              value.values.map<dynamic>((dynamic v) => fromJson(v, match, growable: growable,)),
            );
          }
      }
    } on Exception catch (error, trace) {
      throw ApiException.withInner(HttpStatus.internalServerError, 'Exception during deserialization.', error, trace,);
    }
    throw ApiException(HttpStatus.internalServerError, 'Could not find a suitable class for deserialization',);
  }
}

/// Primarily intended for use in an isolate.
class DeserializationMessage {
  const DeserializationMessage({
    required this.json,
    required this.targetType,
    this.growable = false,
  });

  /// The JSON value to deserialize.
  final String json;

  /// Target type to deserialize to.
  final String targetType;

  /// Whether to make deserialized lists or maps growable.
  final bool growable;
}

/// Primarily intended for use in an isolate.
Future<dynamic> decodeAsync(DeserializationMessage message) async {
  // Remove all spaces. Necessary for regular expressions as well.
  final targetType = message.targetType.replaceAll(' ', '');

  // If the expected target type is String, nothing to do...
  return targetType == 'String'
    ? message.json
    : json.decode(message.json);
}

/// Primarily intended for use in an isolate.
Future<dynamic> deserializeAsync(DeserializationMessage message) async {
  // Remove all spaces. Necessary for regular expressions as well.
  final targetType = message.targetType.replaceAll(' ', '');

  // If the expected target type is String, nothing to do...
  return targetType == 'String'
    ? message.json
    : ApiClient.fromJson(
        json.decode(message.json),
        targetType,
        growable: message.growable,
      );
}

/// Primarily intended for use in an isolate.
Future<String> serializeAsync(Object? value) async => value == null ? '' : json.encode(value);
