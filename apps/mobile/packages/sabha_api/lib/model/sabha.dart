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
    this.candidateCount,
    this.sabhaId,
    this.sabhaKind,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? candidateCount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaKind;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Sabha &&
    other.candidateCount == candidateCount &&
    other.sabhaId == sabhaId &&
    other.sabhaKind == sabhaKind;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidateCount == null ? 0 : candidateCount!.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (sabhaKind == null ? 0 : sabhaKind!.hashCode);

  @override
  String toString() => 'Sabha[candidateCount=$candidateCount, sabhaId=$sabhaId, sabhaKind=$sabhaKind]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.candidateCount != null) {
      json[r'candidateCount'] = this.candidateCount;
    } else {
      json[r'candidateCount'] = null;
    }
    if (this.sabhaId != null) {
      json[r'sabhaId'] = this.sabhaId;
    } else {
      json[r'sabhaId'] = null;
    }
    if (this.sabhaKind != null) {
      json[r'sabhaKind'] = this.sabhaKind;
    } else {
      json[r'sabhaKind'] = null;
    }
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
        return true;
      }());

      return Sabha(
        candidateCount: mapValueOfType<int>(json, r'candidateCount'),
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind'),
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
  };
}

