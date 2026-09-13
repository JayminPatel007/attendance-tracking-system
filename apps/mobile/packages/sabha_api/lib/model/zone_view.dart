//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ZoneView {
  /// Returns a new [ZoneView] instance.
  ZoneView({
    required this.cityId,
    required this.cityName,
    required this.id,
    required this.kshetraCount,
    required this.name,
  });

  String cityId;

  String cityName;

  String id;

  int kshetraCount;

  String name;

  @override
  bool operator ==(Object other) => identical(this, other) || other is ZoneView &&
    other.cityId == cityId &&
    other.cityName == cityName &&
    other.id == id &&
    other.kshetraCount == kshetraCount &&
    other.name == name;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (cityId.hashCode) +
    (cityName.hashCode) +
    (id.hashCode) +
    (kshetraCount.hashCode) +
    (name.hashCode);

  @override
  String toString() => 'ZoneView[cityId=$cityId, cityName=$cityName, id=$id, kshetraCount=$kshetraCount, name=$name]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'cityId'] = this.cityId;
      json[r'cityName'] = this.cityName;
      json[r'id'] = this.id;
      json[r'kshetraCount'] = this.kshetraCount;
      json[r'name'] = this.name;
    return json;
  }

  /// Returns a new [ZoneView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ZoneView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'cityId'), 'Required key "ZoneView[cityId]" is missing from JSON.');
        assert(json[r'cityId'] != null, 'Required key "ZoneView[cityId]" has a null value in JSON.');
        assert(json.containsKey(r'cityName'), 'Required key "ZoneView[cityName]" is missing from JSON.');
        assert(json[r'cityName'] != null, 'Required key "ZoneView[cityName]" has a null value in JSON.');
        assert(json.containsKey(r'id'), 'Required key "ZoneView[id]" is missing from JSON.');
        assert(json[r'id'] != null, 'Required key "ZoneView[id]" has a null value in JSON.');
        assert(json.containsKey(r'kshetraCount'), 'Required key "ZoneView[kshetraCount]" is missing from JSON.');
        assert(json[r'kshetraCount'] != null, 'Required key "ZoneView[kshetraCount]" has a null value in JSON.');
        assert(json.containsKey(r'name'), 'Required key "ZoneView[name]" is missing from JSON.');
        assert(json[r'name'] != null, 'Required key "ZoneView[name]" has a null value in JSON.');
        return true;
      }());

      return ZoneView(
        cityId: mapValueOfType<String>(json, r'cityId')!,
        cityName: mapValueOfType<String>(json, r'cityName')!,
        id: mapValueOfType<String>(json, r'id')!,
        kshetraCount: mapValueOfType<int>(json, r'kshetraCount')!,
        name: mapValueOfType<String>(json, r'name')!,
      );
    }
    return null;
  }

  static List<ZoneView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ZoneView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ZoneView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ZoneView> mapFromJson(dynamic json) {
    final map = <String, ZoneView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ZoneView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ZoneView-objects as value to a dart map
  static Map<String, List<ZoneView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ZoneView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ZoneView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'cityId',
    'cityName',
    'id',
    'kshetraCount',
    'name',
  };
}

