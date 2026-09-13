//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CityView {
  /// Returns a new [CityView] instance.
  CityView({
    required this.id,
    required this.name,
    required this.zoneCount,
  });

  String id;

  String name;

  int zoneCount;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CityView &&
    other.id == id &&
    other.name == name &&
    other.zoneCount == zoneCount;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (id.hashCode) +
    (name.hashCode) +
    (zoneCount.hashCode);

  @override
  String toString() => 'CityView[id=$id, name=$name, zoneCount=$zoneCount]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'id'] = this.id;
      json[r'name'] = this.name;
      json[r'zoneCount'] = this.zoneCount;
    return json;
  }

  /// Returns a new [CityView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CityView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'id'), 'Required key "CityView[id]" is missing from JSON.');
        assert(json[r'id'] != null, 'Required key "CityView[id]" has a null value in JSON.');
        assert(json.containsKey(r'name'), 'Required key "CityView[name]" is missing from JSON.');
        assert(json[r'name'] != null, 'Required key "CityView[name]" has a null value in JSON.');
        assert(json.containsKey(r'zoneCount'), 'Required key "CityView[zoneCount]" is missing from JSON.');
        assert(json[r'zoneCount'] != null, 'Required key "CityView[zoneCount]" has a null value in JSON.');
        return true;
      }());

      return CityView(
        id: mapValueOfType<String>(json, r'id')!,
        name: mapValueOfType<String>(json, r'name')!,
        zoneCount: mapValueOfType<int>(json, r'zoneCount')!,
      );
    }
    return null;
  }

  static List<CityView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CityView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CityView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CityView> mapFromJson(dynamic json) {
    final map = <String, CityView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CityView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CityView-objects as value to a dart map
  static Map<String, List<CityView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CityView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CityView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'id',
    'name',
    'zoneCount',
  };
}

