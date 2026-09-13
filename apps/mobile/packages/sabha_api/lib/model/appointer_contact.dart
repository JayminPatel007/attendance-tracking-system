//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AppointerContact {
  /// Returns a new [AppointerContact] instance.
  AppointerContact({
    required this.mobile,
    required this.name,
  });

  String mobile;

  String name;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AppointerContact &&
    other.mobile == mobile &&
    other.name == name;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (mobile.hashCode) +
    (name.hashCode);

  @override
  String toString() => 'AppointerContact[mobile=$mobile, name=$name]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'mobile'] = this.mobile;
      json[r'name'] = this.name;
    return json;
  }

  /// Returns a new [AppointerContact] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AppointerContact? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'mobile'), 'Required key "AppointerContact[mobile]" is missing from JSON.');
        assert(json[r'mobile'] != null, 'Required key "AppointerContact[mobile]" has a null value in JSON.');
        assert(json.containsKey(r'name'), 'Required key "AppointerContact[name]" is missing from JSON.');
        assert(json[r'name'] != null, 'Required key "AppointerContact[name]" has a null value in JSON.');
        return true;
      }());

      return AppointerContact(
        mobile: mapValueOfType<String>(json, r'mobile')!,
        name: mapValueOfType<String>(json, r'name')!,
      );
    }
    return null;
  }

  static List<AppointerContact> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AppointerContact>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AppointerContact.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AppointerContact> mapFromJson(dynamic json) {
    final map = <String, AppointerContact>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AppointerContact.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AppointerContact-objects as value to a dart map
  static Map<String, List<AppointerContact>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AppointerContact>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AppointerContact.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'mobile',
    'name',
  };
}

