//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AppointmentResponse {
  /// Returns a new [AppointmentResponse] instance.
  AppointmentResponse({
    required this.assignmentId,
    this.candidates = const [],
    required this.personId,
    required this.requiresOverride,
    required this.userId,
  });

  String? assignmentId;

  List<NameCandidate> candidates;

  String? personId;

  bool requiresOverride;

  String? userId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AppointmentResponse &&
    other.assignmentId == assignmentId &&
    _deepEquality.equals(other.candidates, candidates) &&
    other.personId == personId &&
    other.requiresOverride == requiresOverride &&
    other.userId == userId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (assignmentId == null ? 0 : assignmentId!.hashCode) +
    (candidates.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (requiresOverride.hashCode) +
    (userId == null ? 0 : userId!.hashCode);

  @override
  String toString() => 'AppointmentResponse[assignmentId=$assignmentId, candidates=$candidates, personId=$personId, requiresOverride=$requiresOverride, userId=$userId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.assignmentId != null) {
      json[r'assignmentId'] = this.assignmentId;
    } else {
      json[r'assignmentId'] = null;
    }
      json[r'candidates'] = this.candidates;
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
      json[r'requiresOverride'] = this.requiresOverride;
    if (this.userId != null) {
      json[r'userId'] = this.userId;
    } else {
      json[r'userId'] = null;
    }
    return json;
  }

  /// Returns a new [AppointmentResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AppointmentResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'assignmentId'), 'Required key "AppointmentResponse[assignmentId]" is missing from JSON.');
        assert(json.containsKey(r'candidates'), 'Required key "AppointmentResponse[candidates]" is missing from JSON.');
        assert(json[r'candidates'] != null, 'Required key "AppointmentResponse[candidates]" has a null value in JSON.');
        assert(json.containsKey(r'personId'), 'Required key "AppointmentResponse[personId]" is missing from JSON.');
        assert(json.containsKey(r'requiresOverride'), 'Required key "AppointmentResponse[requiresOverride]" is missing from JSON.');
        assert(json[r'requiresOverride'] != null, 'Required key "AppointmentResponse[requiresOverride]" has a null value in JSON.');
        assert(json.containsKey(r'userId'), 'Required key "AppointmentResponse[userId]" is missing from JSON.');
        return true;
      }());

      return AppointmentResponse(
        assignmentId: mapValueOfType<String>(json, r'assignmentId'),
        candidates: NameCandidate.listFromJson(json[r'candidates']),
        personId: mapValueOfType<String>(json, r'personId'),
        requiresOverride: mapValueOfType<bool>(json, r'requiresOverride')!,
        userId: mapValueOfType<String>(json, r'userId'),
      );
    }
    return null;
  }

  static List<AppointmentResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AppointmentResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AppointmentResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AppointmentResponse> mapFromJson(dynamic json) {
    final map = <String, AppointmentResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AppointmentResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AppointmentResponse-objects as value to a dart map
  static Map<String, List<AppointmentResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AppointmentResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AppointmentResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'assignmentId',
    'candidates',
    'personId',
    'requiresOverride',
    'userId',
  };
}

