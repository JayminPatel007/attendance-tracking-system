//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ChooseCityRequest {
  /// Returns a new [ChooseCityRequest] instance.
  ChooseCityRequest({
    this.cityId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? cityId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is ChooseCityRequest &&
    other.cityId == cityId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (cityId == null ? 0 : cityId!.hashCode);

  @override
  String toString() => 'ChooseCityRequest[cityId=$cityId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.cityId != null) {
      json[r'cityId'] = this.cityId;
    } else {
      json[r'cityId'] = null;
    }
    return json;
  }

  /// Returns a new [ChooseCityRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ChooseCityRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return ChooseCityRequest(
        cityId: mapValueOfType<String>(json, r'cityId'),
      );
    }
    return null;
  }

  static List<ChooseCityRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ChooseCityRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ChooseCityRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ChooseCityRequest> mapFromJson(dynamic json) {
    final map = <String, ChooseCityRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ChooseCityRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ChooseCityRequest-objects as value to a dart map
  static Map<String, List<ChooseCityRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ChooseCityRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ChooseCityRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

