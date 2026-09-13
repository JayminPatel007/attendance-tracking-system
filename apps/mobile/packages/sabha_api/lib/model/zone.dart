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
    required this.candidateCount,
    this.kshetras = const [],
    required this.zoneId,
    required this.zoneName,
  });

  int candidateCount;

  List<Kshetra> kshetras;

  String? zoneId;

  String zoneName;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Zone &&
    other.candidateCount == candidateCount &&
    _deepEquality.equals(other.kshetras, kshetras) &&
    other.zoneId == zoneId &&
    other.zoneName == zoneName;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidateCount.hashCode) +
    (kshetras.hashCode) +
    (zoneId == null ? 0 : zoneId!.hashCode) +
    (zoneName.hashCode);

  @override
  String toString() => 'Zone[candidateCount=$candidateCount, kshetras=$kshetras, zoneId=$zoneId, zoneName=$zoneName]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'candidateCount'] = this.candidateCount;
      json[r'kshetras'] = this.kshetras;
    if (this.zoneId != null) {
      json[r'zoneId'] = this.zoneId;
    } else {
      json[r'zoneId'] = null;
    }
      json[r'zoneName'] = this.zoneName;
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
        assert(json.containsKey(r'candidateCount'), 'Required key "Zone[candidateCount]" is missing from JSON.');
        assert(json[r'candidateCount'] != null, 'Required key "Zone[candidateCount]" has a null value in JSON.');
        assert(json.containsKey(r'kshetras'), 'Required key "Zone[kshetras]" is missing from JSON.');
        assert(json[r'kshetras'] != null, 'Required key "Zone[kshetras]" has a null value in JSON.');
        assert(json.containsKey(r'zoneId'), 'Required key "Zone[zoneId]" is missing from JSON.');
        assert(json.containsKey(r'zoneName'), 'Required key "Zone[zoneName]" is missing from JSON.');
        assert(json[r'zoneName'] != null, 'Required key "Zone[zoneName]" has a null value in JSON.');
        return true;
      }());

      return Zone(
        candidateCount: mapValueOfType<int>(json, r'candidateCount')!,
        kshetras: Kshetra.listFromJson(json[r'kshetras']),
        zoneId: mapValueOfType<String>(json, r'zoneId'),
        zoneName: mapValueOfType<String>(json, r'zoneName')!,
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
    'candidateCount',
    'kshetras',
    'zoneId',
    'zoneName',
  };
}

