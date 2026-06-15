//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class Kshetra {
  /// Returns a new [Kshetra] instance.
  Kshetra({
    this.candidateCount,
    this.kshetraId,
    this.kshetraName,
    this.sabhas = const [],
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
  String? kshetraId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? kshetraName;

  List<Sabha> sabhas;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Kshetra &&
    other.candidateCount == candidateCount &&
    other.kshetraId == kshetraId &&
    other.kshetraName == kshetraName &&
    _deepEquality.equals(other.sabhas, sabhas);

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidateCount == null ? 0 : candidateCount!.hashCode) +
    (kshetraId == null ? 0 : kshetraId!.hashCode) +
    (kshetraName == null ? 0 : kshetraName!.hashCode) +
    (sabhas.hashCode);

  @override
  String toString() => 'Kshetra[candidateCount=$candidateCount, kshetraId=$kshetraId, kshetraName=$kshetraName, sabhas=$sabhas]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.candidateCount != null) {
      json[r'candidateCount'] = this.candidateCount;
    } else {
      json[r'candidateCount'] = null;
    }
    if (this.kshetraId != null) {
      json[r'kshetraId'] = this.kshetraId;
    } else {
      json[r'kshetraId'] = null;
    }
    if (this.kshetraName != null) {
      json[r'kshetraName'] = this.kshetraName;
    } else {
      json[r'kshetraName'] = null;
    }
      json[r'sabhas'] = this.sabhas;
    return json;
  }

  /// Returns a new [Kshetra] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static Kshetra? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return Kshetra(
        candidateCount: mapValueOfType<int>(json, r'candidateCount'),
        kshetraId: mapValueOfType<String>(json, r'kshetraId'),
        kshetraName: mapValueOfType<String>(json, r'kshetraName'),
        sabhas: Sabha.listFromJson(json[r'sabhas']),
      );
    }
    return null;
  }

  static List<Kshetra> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Kshetra>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Kshetra.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Kshetra> mapFromJson(dynamic json) {
    final map = <String, Kshetra>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = Kshetra.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Kshetra-objects as value to a dart map
  static Map<String, List<Kshetra>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Kshetra>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Kshetra.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

