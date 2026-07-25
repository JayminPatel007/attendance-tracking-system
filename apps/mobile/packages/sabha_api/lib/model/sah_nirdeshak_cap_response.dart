//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SahNirdeshakCapResponse {
  /// Returns a new [SahNirdeshakCapResponse] instance.
  SahNirdeshakCapResponse({
    this.active,
    this.cap,
    this.reached,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? active;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? cap;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? reached;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SahNirdeshakCapResponse &&
    other.active == active &&
    other.cap == cap &&
    other.reached == reached;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (active == null ? 0 : active!.hashCode) +
    (cap == null ? 0 : cap!.hashCode) +
    (reached == null ? 0 : reached!.hashCode);

  @override
  String toString() => 'SahNirdeshakCapResponse[active=$active, cap=$cap, reached=$reached]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.active != null) {
      json[r'active'] = this.active;
    } else {
      json[r'active'] = null;
    }
    if (this.cap != null) {
      json[r'cap'] = this.cap;
    } else {
      json[r'cap'] = null;
    }
    if (this.reached != null) {
      json[r'reached'] = this.reached;
    } else {
      json[r'reached'] = null;
    }
    return json;
  }

  /// Returns a new [SahNirdeshakCapResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SahNirdeshakCapResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return SahNirdeshakCapResponse(
        active: mapValueOfType<int>(json, r'active'),
        cap: mapValueOfType<int>(json, r'cap'),
        reached: mapValueOfType<bool>(json, r'reached'),
      );
    }
    return null;
  }

  static List<SahNirdeshakCapResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SahNirdeshakCapResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SahNirdeshakCapResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SahNirdeshakCapResponse> mapFromJson(dynamic json) {
    final map = <String, SahNirdeshakCapResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SahNirdeshakCapResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SahNirdeshakCapResponse-objects as value to a dart map
  static Map<String, List<SahNirdeshakCapResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SahNirdeshakCapResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SahNirdeshakCapResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

