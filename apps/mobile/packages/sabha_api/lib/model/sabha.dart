//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class Sabha {
  /// Returns a new [Sabha] instance.
  Sabha({
    required this.candidateCount,
    required this.sabhaId,
    required this.sabhaKind,
  });

  int candidateCount;

  String sabhaId;

  String sabhaKind;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Sabha &&
    other.candidateCount == candidateCount &&
    other.sabhaId == sabhaId &&
    other.sabhaKind == sabhaKind;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidateCount.hashCode) +
    (sabhaId.hashCode) +
    (sabhaKind.hashCode);

  @override
  String toString() => 'Sabha[candidateCount=$candidateCount, sabhaId=$sabhaId, sabhaKind=$sabhaKind]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'candidateCount'] = this.candidateCount;
      json[r'sabhaId'] = this.sabhaId;
      json[r'sabhaKind'] = this.sabhaKind;
    return json;
  }

  /// Returns a new [Sabha] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static Sabha? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'candidateCount'), 'Required key "Sabha[candidateCount]" is missing from JSON.');
        assert(json[r'candidateCount'] != null, 'Required key "Sabha[candidateCount]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaId'), 'Required key "Sabha[sabhaId]" is missing from JSON.');
        assert(json[r'sabhaId'] != null, 'Required key "Sabha[sabhaId]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaKind'), 'Required key "Sabha[sabhaKind]" is missing from JSON.');
        assert(json[r'sabhaKind'] != null, 'Required key "Sabha[sabhaKind]" has a null value in JSON.');
        return true;
      }());

      return Sabha(
        candidateCount: mapValueOfType<int>(json, r'candidateCount')!,
        sabhaId: mapValueOfType<String>(json, r'sabhaId')!,
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind')!,
      );
    }
    return null;
  }

  static List<Sabha> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Sabha>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Sabha.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Sabha> mapFromJson(dynamic json) {
    final map = <String, Sabha>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = Sabha.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Sabha-objects as value to a dart map
  static Map<String, List<Sabha>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Sabha>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Sabha.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'candidateCount',
    'sabhaId',
    'sabhaKind',
  };
}

