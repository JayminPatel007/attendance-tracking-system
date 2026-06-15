//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MarkRequest {
  /// Returns a new [MarkRequest] instance.
  MarkRequest({
    this.clientMarkedAt,
    this.personId,
    this.present,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? clientMarkedAt;

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
  bool? present;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MarkRequest &&
    other.clientMarkedAt == clientMarkedAt &&
    other.personId == personId &&
    other.present == present;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (clientMarkedAt == null ? 0 : clientMarkedAt!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (present == null ? 0 : present!.hashCode);

  @override
  String toString() => 'MarkRequest[clientMarkedAt=$clientMarkedAt, personId=$personId, present=$present]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.clientMarkedAt != null) {
      json[r'clientMarkedAt'] = this.clientMarkedAt!.toUtc().toIso8601String();
    } else {
      json[r'clientMarkedAt'] = null;
    }
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
    if (this.present != null) {
      json[r'present'] = this.present;
    } else {
      json[r'present'] = null;
    }
    return json;
  }

  /// Returns a new [MarkRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MarkRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return MarkRequest(
        clientMarkedAt: mapDateTime(json, r'clientMarkedAt', r''),
        personId: mapValueOfType<String>(json, r'personId'),
        present: mapValueOfType<bool>(json, r'present'),
      );
    }
    return null;
  }

  static List<MarkRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MarkRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MarkRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MarkRequest> mapFromJson(dynamic json) {
    final map = <String, MarkRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MarkRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MarkRequest-objects as value to a dart map
  static Map<String, List<MarkRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MarkRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MarkRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

