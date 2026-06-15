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
    this.assignmentId,
    this.candidates = const [],
    this.personId,
    this.requiresOverride,
    this.userId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? assignmentId;

  List<NameCandidate> candidates;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? personId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? requiresOverride;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
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
    (requiresOverride == null ? 0 : requiresOverride!.hashCode) +
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
    if (this.requiresOverride != null) {
      json[r'requiresOverride'] = this.requiresOverride;
    } else {
      json[r'requiresOverride'] = null;
    }
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
        return true;
      }());

      return AppointmentResponse(
        assignmentId: mapValueOfType<String>(json, r'assignmentId'),
        candidates: NameCandidate.listFromJson(json[r'candidates']),
        personId: mapValueOfType<String>(json, r'personId'),
        requiresOverride: mapValueOfType<bool>(json, r'requiresOverride'),
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
  };
}

