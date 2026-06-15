//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ReissueRequest {
  /// Returns a new [ReissueRequest] instance.
  ReissueRequest({
    this.newPassword,
    this.targetUserId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? newPassword;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? targetUserId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is ReissueRequest &&
    other.newPassword == newPassword &&
    other.targetUserId == targetUserId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (newPassword == null ? 0 : newPassword!.hashCode) +
    (targetUserId == null ? 0 : targetUserId!.hashCode);

  @override
  String toString() => 'ReissueRequest[newPassword=$newPassword, targetUserId=$targetUserId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.newPassword != null) {
      json[r'newPassword'] = this.newPassword;
    } else {
      json[r'newPassword'] = null;
    }
    if (this.targetUserId != null) {
      json[r'targetUserId'] = this.targetUserId;
    } else {
      json[r'targetUserId'] = null;
    }
    return json;
  }

  /// Returns a new [ReissueRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ReissueRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return ReissueRequest(
        newPassword: mapValueOfType<String>(json, r'newPassword'),
        targetUserId: mapValueOfType<String>(json, r'targetUserId'),
      );
    }
    return null;
  }

  static List<ReissueRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ReissueRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ReissueRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ReissueRequest> mapFromJson(dynamic json) {
    final map = <String, ReissueRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ReissueRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ReissueRequest-objects as value to a dart map
  static Map<String, List<ReissueRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ReissueRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ReissueRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

