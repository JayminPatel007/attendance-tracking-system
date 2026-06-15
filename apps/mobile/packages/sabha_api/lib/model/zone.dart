//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class Zone {
  /// Returns a new [Zone] instance.
  Zone({
    this.candidateCount,
    this.kshetras = const [],
    this.zoneId,
    this.zoneName,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? candidateCount;

  List<Kshetra> kshetras;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? zoneId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? zoneName;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Zone &&
    other.candidateCount == candidateCount &&
    _deepEquality.equals(other.kshetras, kshetras) &&
    other.zoneId == zoneId &&
    other.zoneName == zoneName;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidateCount == null ? 0 : candidateCount!.hashCode) +
    (kshetras.hashCode) +
    (zoneId == null ? 0 : zoneId!.hashCode) +
    (zoneName == null ? 0 : zoneName!.hashCode);

  @override
  String toString() => 'Zone[candidateCount=$candidateCount, kshetras=$kshetras, zoneId=$zoneId, zoneName=$zoneName]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.candidateCount != null) {
      json[r'candidateCount'] = this.candidateCount;
    } else {
      json[r'candidateCount'] = null;
    }
      json[r'kshetras'] = this.kshetras;
    if (this.zoneId != null) {
      json[r'zoneId'] = this.zoneId;
    } else {
      json[r'zoneId'] = null;
    }
    if (this.zoneName != null) {
      json[r'zoneName'] = this.zoneName;
    } else {
      json[r'zoneName'] = null;
    }
    return json;
  }

  /// Returns a new [Zone] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static Zone? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return Zone(
        candidateCount: mapValueOfType<int>(json, r'candidateCount'),
        kshetras: Kshetra.listFromJson(json[r'kshetras']),
        zoneId: mapValueOfType<String>(json, r'zoneId'),
        zoneName: mapValueOfType<String>(json, r'zoneName'),
      );
    }
    return null;
  }

  static List<Zone> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Zone>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Zone.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Zone> mapFromJson(dynamic json) {
    final map = <String, Zone>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = Zone.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Zone-objects as value to a dart map
  static Map<String, List<Zone>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Zone>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Zone.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

