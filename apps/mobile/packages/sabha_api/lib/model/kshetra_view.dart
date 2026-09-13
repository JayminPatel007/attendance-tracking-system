//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class KshetraView {
  /// Returns a new [KshetraView] instance.
  KshetraView({
    required this.id,
    required this.name,
    required this.sabhaCount,
    required this.zoneId,
  });

  String id;

  String name;

  int sabhaCount;

  String zoneId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is KshetraView &&
    other.id == id &&
    other.name == name &&
    other.sabhaCount == sabhaCount &&
    other.zoneId == zoneId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (id.hashCode) +
    (name.hashCode) +
    (sabhaCount.hashCode) +
    (zoneId.hashCode);

  @override
  String toString() => 'KshetraView[id=$id, name=$name, sabhaCount=$sabhaCount, zoneId=$zoneId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'id'] = this.id;
      json[r'name'] = this.name;
      json[r'sabhaCount'] = this.sabhaCount;
      json[r'zoneId'] = this.zoneId;
    return json;
  }

  /// Returns a new [KshetraView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static KshetraView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'id'), 'Required key "KshetraView[id]" is missing from JSON.');
        assert(json[r'id'] != null, 'Required key "KshetraView[id]" has a null value in JSON.');
        assert(json.containsKey(r'name'), 'Required key "KshetraView[name]" is missing from JSON.');
        assert(json[r'name'] != null, 'Required key "KshetraView[name]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaCount'), 'Required key "KshetraView[sabhaCount]" is missing from JSON.');
        assert(json[r'sabhaCount'] != null, 'Required key "KshetraView[sabhaCount]" has a null value in JSON.');
        assert(json.containsKey(r'zoneId'), 'Required key "KshetraView[zoneId]" is missing from JSON.');
        assert(json[r'zoneId'] != null, 'Required key "KshetraView[zoneId]" has a null value in JSON.');
        return true;
      }());

      return KshetraView(
        id: mapValueOfType<String>(json, r'id')!,
        name: mapValueOfType<String>(json, r'name')!,
        sabhaCount: mapValueOfType<int>(json, r'sabhaCount')!,
        zoneId: mapValueOfType<String>(json, r'zoneId')!,
      );
    }
    return null;
  }

  static List<KshetraView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <KshetraView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = KshetraView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, KshetraView> mapFromJson(dynamic json) {
    final map = <String, KshetraView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = KshetraView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of KshetraView-objects as value to a dart map
  static Map<String, List<KshetraView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<KshetraView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = KshetraView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'id',
    'name',
    'sabhaCount',
    'zoneId',
  };
}

