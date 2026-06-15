//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class VerifyPayload {
  /// Returns a new [VerifyPayload] instance.
  VerifyPayload({
    this.otpCode,
    this.resetId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? otpCode;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? resetId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is VerifyPayload &&
    other.otpCode == otpCode &&
    other.resetId == resetId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (otpCode == null ? 0 : otpCode!.hashCode) +
    (resetId == null ? 0 : resetId!.hashCode);

  @override
  String toString() => 'VerifyPayload[otpCode=$otpCode, resetId=$resetId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.otpCode != null) {
      json[r'otpCode'] = this.otpCode;
    } else {
      json[r'otpCode'] = null;
    }
    if (this.resetId != null) {
      json[r'resetId'] = this.resetId;
    } else {
      json[r'resetId'] = null;
    }
    return json;
  }

  /// Returns a new [VerifyPayload] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static VerifyPayload? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return VerifyPayload(
        otpCode: mapValueOfType<String>(json, r'otpCode'),
        resetId: mapValueOfType<String>(json, r'resetId'),
      );
    }
    return null;
  }

  static List<VerifyPayload> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <VerifyPayload>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = VerifyPayload.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, VerifyPayload> mapFromJson(dynamic json) {
    final map = <String, VerifyPayload>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = VerifyPayload.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of VerifyPayload-objects as value to a dart map
  static Map<String, List<VerifyPayload>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<VerifyPayload>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = VerifyPayload.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

