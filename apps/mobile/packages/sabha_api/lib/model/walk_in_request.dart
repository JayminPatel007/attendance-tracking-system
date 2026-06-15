//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class WalkInRequest {
  /// Returns a new [WalkInRequest] instance.
  WalkInRequest({
    this.clientMarkedAt,
    this.personId,
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

  @override
  bool operator ==(Object other) => identical(this, other) || other is WalkInRequest &&
    other.clientMarkedAt == clientMarkedAt &&
    other.personId == personId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (clientMarkedAt == null ? 0 : clientMarkedAt!.hashCode) +
    (personId == null ? 0 : personId!.hashCode);

  @override
  String toString() => 'WalkInRequest[clientMarkedAt=$clientMarkedAt, personId=$personId]';

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
    return json;
  }

  /// Returns a new [WalkInRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static WalkInRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return WalkInRequest(
        clientMarkedAt: mapDateTime(json, r'clientMarkedAt', r''),
        personId: mapValueOfType<String>(json, r'personId'),
      );
    }
    return null;
  }

  static List<WalkInRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <WalkInRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = WalkInRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, WalkInRequest> mapFromJson(dynamic json) {
    final map = <String, WalkInRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = WalkInRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of WalkInRequest-objects as value to a dart map
  static Map<String, List<WalkInRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<WalkInRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = WalkInRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

