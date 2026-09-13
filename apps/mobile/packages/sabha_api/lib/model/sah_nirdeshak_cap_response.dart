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
    required this.active,
    required this.cap,
    required this.reached,
  });

  int active;

  int cap;

  bool reached;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SahNirdeshakCapResponse &&
    other.active == active &&
    other.cap == cap &&
    other.reached == reached;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (active.hashCode) +
    (cap.hashCode) +
    (reached.hashCode);

  @override
  String toString() => 'SahNirdeshakCapResponse[active=$active, cap=$cap, reached=$reached]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'active'] = this.active;
      json[r'cap'] = this.cap;
      json[r'reached'] = this.reached;
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
        assert(json.containsKey(r'active'), 'Required key "SahNirdeshakCapResponse[active]" is missing from JSON.');
        assert(json[r'active'] != null, 'Required key "SahNirdeshakCapResponse[active]" has a null value in JSON.');
        assert(json.containsKey(r'cap'), 'Required key "SahNirdeshakCapResponse[cap]" is missing from JSON.');
        assert(json[r'cap'] != null, 'Required key "SahNirdeshakCapResponse[cap]" has a null value in JSON.');
        assert(json.containsKey(r'reached'), 'Required key "SahNirdeshakCapResponse[reached]" is missing from JSON.');
        assert(json[r'reached'] != null, 'Required key "SahNirdeshakCapResponse[reached]" has a null value in JSON.');
        return true;
      }());

      return SahNirdeshakCapResponse(
        active: mapValueOfType<int>(json, r'active')!,
        cap: mapValueOfType<int>(json, r'cap')!,
        reached: mapValueOfType<bool>(json, r'reached')!,
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
    'active',
    'cap',
    'reached',
  };
}

