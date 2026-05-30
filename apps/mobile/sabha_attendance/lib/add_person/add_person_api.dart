import 'dart:convert';
import 'package:http/http.dart' as http;

/// Thin wrapper over the Person Directory endpoints (Slice 6, ADR-0013). The
/// add flow is **online-only** (ADR-0007) — the de-dup check must hit the live
/// Directory, so nothing here is ever queued offline.
class AddPersonApi {
  AddPersonApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client();

  final String baseUrl;
  final String accessToken;
  final http.Client _client;

  /// Step 1 of the flow: look the entered mobile up against the Directory.
  /// Returns the existing Person on an exact hit (the forced-redirect case), or
  /// `null` when the number is new.
  Future<DirectoryPerson?> findByMobile(String mobile) async {
    final resp = await _client.get(
      Uri.parse('$baseUrl/api/directory/persons').replace(queryParameters: {'mobile': mobile}),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    if (resp.statusCode == 404) return null;
    if (resp.statusCode != 200) {
      throw AddPersonApiException('GET persons?mobile -> ${resp.statusCode}: ${resp.body}');
    }
    return DirectoryPerson.fromJson(jsonDecode(resp.body) as Map<String, dynamic>);
  }

  /// Step 2: create the Person. A `201` is a clean create; a `200` is the name
  /// soft-warn (candidates returned, nothing created yet); a `409` is the mobile
  /// hard block; a `422` is a domain-rule rejection (e.g. neither mobile nor
  /// guardian).
  Future<AddPersonOutcome> add(AddPersonRequest req) async {
    final resp = await _client.post(
      Uri.parse('$baseUrl/api/directory/persons'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode(req.toJson()),
    );
    if (resp.statusCode == 201 || resp.statusCode == 200) {
      return AddPersonOutcome.fromJson(jsonDecode(resp.body) as Map<String, dynamic>);
    }
    if (resp.statusCode == 409) {
      final body = _decode(resp.body);
      if (body?['code'] == 'MOBILE_ALREADY_REGISTERED') {
        throw MobileAlreadyRegisteredException(
          existingPersonId: body?['existingPersonId'] as String?,
          message: body?['message'] as String? ?? 'This mobile is already in the Directory.',
        );
      }
    }
    if (resp.statusCode == 422) {
      throw DirectoryRuleException(_messageOf(resp.body) ?? 'That add was rejected.');
    }
    throw AddPersonApiException('POST persons -> ${resp.statusCode}: ${resp.body}');
  }

  Map<String, dynamic>? _decode(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) return decoded;
    } on FormatException {
      // non-JSON body
    }
    return null;
  }

  String? _messageOf(String body) => _decode(body)?['message'] as String?;
}

class AddPersonApiException implements Exception {
  AddPersonApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// The mobile hard block (ADR-0013): an exact match on an existing Person's
/// mobile. The flow redirects to that Person's profile using
/// [existingPersonId].
class MobileAlreadyRegisteredException implements Exception {
  MobileAlreadyRegisteredException({required this.existingPersonId, required this.message});
  final String? existingPersonId;
  final String message;
  @override
  String toString() => message;
}

/// A domain rule rejected the add (422) — e.g. neither mobile nor guardian.
class DirectoryRuleException implements Exception {
  DirectoryRuleException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// Request body for `POST /api/directory/persons`. Either [mobile] (own) or
/// [guardianPersonId] (a child sharing a parent's mobile) is set, per ADR-0013.
class AddPersonRequest {
  const AddPersonRequest({
    required this.fullName,
    required this.gender,
    required this.homeSabhaId,
    this.dateOfBirth,
    this.mobile,
    this.guardianPersonId,
    this.overrideDuplicateWarning = false,
  });

  final String fullName;
  final String gender;
  final String homeSabhaId;
  final String? dateOfBirth;
  final String? mobile;
  final String? guardianPersonId;
  final bool overrideDuplicateWarning;

  AddPersonRequest copyWith({bool? overrideDuplicateWarning}) {
    return AddPersonRequest(
      fullName: fullName,
      gender: gender,
      homeSabhaId: homeSabhaId,
      dateOfBirth: dateOfBirth,
      mobile: mobile,
      guardianPersonId: guardianPersonId,
      overrideDuplicateWarning: overrideDuplicateWarning ?? this.overrideDuplicateWarning,
    );
  }

  Map<String, dynamic> toJson() => {
        'fullName': fullName,
        'gender': gender,
        'dateOfBirth': dateOfBirth,
        'mobile': mobile,
        'guardianPersonId': guardianPersonId,
        'homeSabhaId': homeSabhaId,
        'overrideDuplicateWarning': overrideDuplicateWarning,
      };
}

/// Outcome of an add: either a clean create ([createdPersonId] set) or a soft
/// warn ([requiresOverride] true, [candidates] populated, nothing created).
class AddPersonOutcome {
  AddPersonOutcome({this.createdPersonId, required this.candidates, required this.requiresOverride});

  final String? createdPersonId;
  final List<NameCandidate> candidates;
  final bool requiresOverride;

  bool get created => createdPersonId != null;

  factory AddPersonOutcome.fromJson(Map<String, dynamic> json) {
    return AddPersonOutcome(
      createdPersonId: json['personId'] as String?,
      requiresOverride: json['requiresOverride'] as bool? ?? false,
      candidates: ((json['candidates'] as List<dynamic>?) ?? const [])
          .map((e) => NameCandidate.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// A possible duplicate surfaced by the name soft-warn.
class NameCandidate {
  NameCandidate({required this.personId, required this.fullName, required this.homeSabhaName});

  final String personId;
  final String fullName;
  final String homeSabhaName;

  factory NameCandidate.fromJson(Map<String, dynamic> json) {
    return NameCandidate(
      personId: json['personId'] as String,
      fullName: json['fullName'] as String,
      homeSabhaName: json['homeSabhaName'] as String? ?? '',
    );
  }
}

/// A Person's Directory profile, as returned by the mobile lookup / detail
/// endpoints.
class DirectoryPerson {
  DirectoryPerson({
    required this.id,
    required this.fullName,
    required this.gender,
    this.dateOfBirth,
    this.mobile,
    this.guardianPersonId,
  });

  final String id;
  final String fullName;
  final String gender;
  final String? dateOfBirth;
  final String? mobile;
  final String? guardianPersonId;

  factory DirectoryPerson.fromJson(Map<String, dynamic> json) {
    return DirectoryPerson(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      gender: json['gender'] as String,
      dateOfBirth: json['dateOfBirth'] as String?,
      mobile: json['mobile'] as String?,
      guardianPersonId: json['guardianPersonId'] as String?,
    );
  }
}
