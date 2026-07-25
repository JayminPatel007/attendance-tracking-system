import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:sabha_api/api.dart' as api;

import '../api/api_error.dart';

/// Thin wrapper over the Person Directory endpoints (Slice 6, ADR-0013). The
/// add flow is **online-only** (ADR-0007) — the de-dup check must hit the live
/// Directory, so nothing here is ever queued offline.
///
/// Every payload on this seam — request and response, both directions — is a
/// generated typed model (issues #73, #104), so no shape here can silently drift
/// from the backend the way the old hand-rolled parsers did (issue #75). What
/// remains hand-written is the *transport* of the add call: the generated
/// operation collapses a failure into a status plus a body string, and the add
/// flow needs the ProblemDetail `code` and `existingPersonId` extensions off a
/// `409` (issues #67, #70) to drive the hard-block redirect. So the add posts the
/// generated request through the plain client and keeps the shared [apiError]
/// dispatcher; the mobile lookup, which has no such error contract, goes through
/// the generated operation whole.
class AddPersonApi {
  AddPersonApi({required this.baseUrl, required this.accessToken, http.Client? client})
      : _client = client ?? http.Client() {
    final apiClient = api.ApiClient(
      basePath: baseUrl,
      authentication: api.HttpBearerAuth()..accessToken = accessToken,
    );
    if (client != null) {
      apiClient.client = client;
    }
    _directory = api.PersonDirectoryRestControllerApi(apiClient);
  }

  final String baseUrl;
  final String accessToken;
  final http.Client _client;
  late final api.PersonDirectoryRestControllerApi _directory;

  /// Step 1 of the flow: look the entered mobile up against the Directory.
  /// Returns the existing Person on an exact hit (the forced-redirect case), or
  /// `null` when the number is new — the `404` the endpoint documents as an
  /// outcome, not a failure. Any other status is a transport failure and must
  /// not be read as "new number".
  Future<DirectoryPerson?> findByMobile(String mobile) async {
    try {
      final person = await _directory.byMobile(mobile);
      return person == null ? null : DirectoryPerson._fromApi(person);
    } on api.ApiException catch (e) {
      if (e.code == 404) return null;
      throw AddPersonApiException('GET persons?mobile -> ${e.code}: ${e.message}');
    }
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
      body: jsonEncode(req.toApi()),
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

  /// The generated wire model for this request.
  ///
  /// [dateOfBirth] is the one field that needs care. A birthdate is a calendar
  /// date — the backend field is a `LocalDate`, no time and no zone — but the
  /// generated model types it as a `DateTime` and serializes
  /// `.toUtc()`. Handing it a *local* midnight would therefore post the previous
  /// day from any device east of Greenwich (IST `2010-05-01` → `2010-04-30`).
  /// Building the value at UTC midnight makes that conversion a no-op, so the
  /// entered date goes out as entered from any timezone (issue #104).
  api.AddPersonRequest toApi() => api.AddPersonRequest(
        fullName: fullName,
        gender: _genderValue(gender),
        dateOfBirth: _utcMidnight(dateOfBirth),
        mobile: mobile,
        guardianPersonId: guardianPersonId,
        homeSabhaId: homeSabhaId,
        overrideDuplicateWarning: overrideDuplicateWarning,
      );

  /// [gender] is a closed set on the wire. The generated transformer answers
  /// `null` for a value outside it, which would quietly post a Person with no
  /// gender; say so instead, since gender decides which demographic Sabhas the
  /// Person is eligible for (CONTEXT.md).
  static api.AddPersonRequestGenderEnum _genderValue(String gender) {
    final value = api.AddPersonRequestGenderEnum.fromJson(gender);
    if (value == null) {
      throw DirectoryRuleException('Choose a gender before adding the Person.');
    }
    return value;
  }

  /// The DOB field is free text, so an unparseable entry is an adder mistake to
  /// report — not a crash to fall through into the generic connection error.
  static DateTime? _utcMidnight(String? isoDate) {
    if (isoDate == null || isoDate.isEmpty) return null;
    final date = DateTime.tryParse(isoDate);
    if (date == null) {
      throw DirectoryRuleException('Enter the date of birth as YYYY-MM-DD.');
    }
    return DateTime.utc(date.year, date.month, date.day);
  }
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
      requiresOverride: r.requiresOverride,
      candidates: r.candidates.map(NameCandidate._fromApi).toList(),
    );
  }
}

/// A possible duplicate surfaced by the name soft-warn. [homeSabhas] carries all
/// of the Person's current Home Sabha kinds — a Person has one per Sabha kind
/// they qualify for (typically their demographic Sabha + Sanyukta, CONTEXT.md) —
/// so the adder sees the demographic Sabha and not just whichever sorts first.
/// Elements are `sabha_kind` strings.
class NameCandidate {
  NameCandidate({required this.personId, required this.fullName, this.homeSabhas = const []});

  final String personId;
  final String fullName;
  final List<String> homeSabhas;

  /// The Home Sabha kinds as a single line for the soft-warn, e.g.
  /// `REGULAR_YUVAK, REGULAR_SANYUKTA`. Empty when the Person has none.
  String get homeSabhasLabel => homeSabhas.join(', ');

  factory NameCandidate._fromApi(api.NameCandidate c) => NameCandidate(
        personId: c.personId,
        fullName: c.fullName,
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

  /// [dateOfBirth] comes back as the generated `DateTime`; the profile view shows
  /// it as the calendar date it is, read off the value's own fields so no
  /// timezone conversion can move it (issue #104).
  factory DirectoryPerson._fromApi(api.PersonResponse p) => DirectoryPerson(
        id: p.id,
        fullName: p.fullName,
        gender: p.gender.value,
        dateOfBirth: p.dateOfBirth == null ? null : _isoDate(p.dateOfBirth!),
        mobile: p.mobile,
        guardianPersonId: p.guardianPersonId,
      );

  static String _isoDate(DateTime date) => '${date.year.toString().padLeft(4, '0')}'
      '-${date.month.toString().padLeft(2, '0')}'
      '-${date.day.toString().padLeft(2, '0')}';
}
