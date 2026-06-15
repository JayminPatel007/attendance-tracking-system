//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SabhaTree {
  /// Returns a new [SabhaTree] instance.
  SabhaTree({
    this.zones = const [],
  });

  List<Zone> zones;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SabhaTree &&
    _deepEquality.equals(other.zones, zones);

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (zones.hashCode);

  @override
  String toString() => 'SabhaTree[zones=$zones]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'zones'] = this.zones;
    return json;
  }

  /// Returns a new [SabhaTree] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SabhaTree? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return SabhaTree(
        zones: Zone.listFromJson(json[r'zones']),
      );
    }
    return null;
  }

  static List<SabhaTree> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaTree>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaTree.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SabhaTree> mapFromJson(dynamic json) {
    final map = <String, SabhaTree>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SabhaTree.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SabhaTree-objects as value to a dart map
  static Map<String, List<SabhaTree>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SabhaTree>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SabhaTree.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

