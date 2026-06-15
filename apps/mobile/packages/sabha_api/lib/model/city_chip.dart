//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CityChip {
  /// Returns a new [CityChip] instance.
  CityChip({
    this.cities = const [],
    this.sant,
    this.selectedCityId,
  });

  List<CityOption> cities;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? sant;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? selectedCityId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CityChip &&
    _deepEquality.equals(other.cities, cities) &&
    other.sant == sant &&
    other.selectedCityId == selectedCityId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (cities.hashCode) +
    (sant == null ? 0 : sant!.hashCode) +
    (selectedCityId == null ? 0 : selectedCityId!.hashCode);

  @override
  String toString() => 'CityChip[cities=$cities, sant=$sant, selectedCityId=$selectedCityId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'cities'] = this.cities;
    if (this.sant != null) {
      json[r'sant'] = this.sant;
    } else {
      json[r'sant'] = null;
    }
    if (this.selectedCityId != null) {
      json[r'selectedCityId'] = this.selectedCityId;
    } else {
      json[r'selectedCityId'] = null;
    }
    return json;
  }

  /// Returns a new [CityChip] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CityChip? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CityChip(
        cities: CityOption.listFromJson(json[r'cities']),
        sant: mapValueOfType<bool>(json, r'sant'),
        selectedCityId: mapValueOfType<String>(json, r'selectedCityId'),
      );
    }
    return null;
  }

  static List<CityChip> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CityChip>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CityChip.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CityChip> mapFromJson(dynamic json) {
    final map = <String, CityChip>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CityChip.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CityChip-objects as value to a dart map
  static Map<String, List<CityChip>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CityChip>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CityChip.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

