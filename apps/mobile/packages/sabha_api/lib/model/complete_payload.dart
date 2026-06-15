//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CompletePayload {
  /// Returns a new [CompletePayload] instance.
  CompletePayload({
    this.newPassword,
    this.resetToken,
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
  String? resetToken;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CompletePayload &&
    other.newPassword == newPassword &&
    other.resetToken == resetToken;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (newPassword == null ? 0 : newPassword!.hashCode) +
    (resetToken == null ? 0 : resetToken!.hashCode);

  @override
  String toString() => 'CompletePayload[newPassword=$newPassword, resetToken=$resetToken]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.newPassword != null) {
      json[r'newPassword'] = this.newPassword;
    } else {
      json[r'newPassword'] = null;
    }
    if (this.resetToken != null) {
      json[r'resetToken'] = this.resetToken;
    } else {
      json[r'resetToken'] = null;
    }
    return json;
  }

  /// Returns a new [CompletePayload] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CompletePayload? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CompletePayload(
        newPassword: mapValueOfType<String>(json, r'newPassword'),
        resetToken: mapValueOfType<String>(json, r'resetToken'),
      );
    }
    return null;
  }

  static List<CompletePayload> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CompletePayload>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CompletePayload.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CompletePayload> mapFromJson(dynamic json) {
    final map = <String, CompletePayload>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CompletePayload.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CompletePayload-objects as value to a dart map
  static Map<String, List<CompletePayload>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CompletePayload>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CompletePayload.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

