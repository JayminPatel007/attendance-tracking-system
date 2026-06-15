//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class VerifyResponse {
  /// Returns a new [VerifyResponse] instance.
  VerifyResponse({
    this.resetToken,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? resetToken;

  @override
  bool operator ==(Object other) => identical(this, other) || other is VerifyResponse &&
    other.resetToken == resetToken;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (resetToken == null ? 0 : resetToken!.hashCode);

  @override
  String toString() => 'VerifyResponse[resetToken=$resetToken]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.resetToken != null) {
      json[r'resetToken'] = this.resetToken;
    } else {
      json[r'resetToken'] = null;
    }
    return json;
  }

  /// Returns a new [VerifyResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static VerifyResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return VerifyResponse(
        resetToken: mapValueOfType<String>(json, r'resetToken'),
      );
    }
    return null;
  }

  static List<VerifyResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <VerifyResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = VerifyResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, VerifyResponse> mapFromJson(dynamic json) {
    final map = <String, VerifyResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = VerifyResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of VerifyResponse-objects as value to a dart map
  static Map<String, List<VerifyResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<VerifyResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = VerifyResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

