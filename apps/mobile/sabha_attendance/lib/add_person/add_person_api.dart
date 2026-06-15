import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:sabha_api/api.dart' as api;

import '../api/api_error.dart';

/// Thin wrapper over the Person Directory endpoints (Slice 6, ADR-0013). The
/// add flow is **online-only** (ADR-0007) — the de-dup check must hit the live
/// Directory, so nothing here is ever queued offline.
///
/// Responses are deserialized through the generated typed models (issue #73) —
/// `AddPersonResponse`/`NameCandidate` — so the soft-warn candidate shape can
/// never silently drift from the backend the way the old hand-rolled parser did
/// (issue #75). The *request* is still built by hand: `dateOfBirth` is a
/// free-text passthrough and `gender` a plain string, neither of which survives
/// the generated `AddPersonRequest` cleanly (its `DateTime` serializer applies
/// `.toUtc()`, shifting birthdates, and `gender` is a closed enum). `findByMobile`
/// likewise stays hand-rolled: the spec types that response as an untyped object.
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
      return AddPersonOutcome.fromResponse(api.AddPersonResponse.fromJson(jsonDecode(resp.body))!);
    }
    apiError(resp, 'POST persons', {
      409: (e) => e.code == 'MOBILE_ALREADY_REGISTERED'
          ? MobileAlreadyRegisteredException(
              existingPersonId: e.extension('existingPersonId'),
              message: e.message('This mobile is already in the Directory.'),
            )
          : null,
      422: (e) => DirectoryRuleException(e.message('That add was rejected.')),
    }, fallback: AddPersonApiException.new);
  }
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

  factory AddPersonOutcome.fromResponse(api.AddPersonResponse r) {
    return AddPersonOutcome(
      createdPersonId: r.personId,
      requiresOverride: r.requiresOverride ?? false,
      candidates: r.candidates.map(NameCandidate._fromApi).toList(),
    );
  }
}

/// A possible duplicate surfaced by the name soft-warn. [homeSabhas] carries all
/// of the Person's current Home Sabha kinds — a Person has one per Sabha kind
/// they qualify for (typically their demographic Sabha + Sanyukta, CONTEXT.md) —
/// so the adder sees the demographic Sabha and not just whichever sorts first.
/// Elements are `sabha_kind` strings.
///
/// A non-null view model: the API seam asserts the backend's contract (id + name
/// always present) once here, so the soft-warn card never deals with nulls.
class NameCandidate {
  NameCandidate({required this.personId, required this.fullName, this.homeSabhas = const []});

  final String personId;
  final String fullName;
  final List<String> homeSabhas;

  /// The Home Sabha kinds as a single line for the soft-warn, e.g.
  /// `REGULAR_YUVAK, REGULAR_SANYUKTA`. Empty when the Person has none.
  String get homeSabhasLabel => homeSabhas.join(', ');

  factory NameCandidate._fromApi(api.NameCandidate c) => NameCandidate(
        personId: c.personId!,
        fullName: c.fullName!,
        homeSabhas: c.homeSabhas,
      );
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
