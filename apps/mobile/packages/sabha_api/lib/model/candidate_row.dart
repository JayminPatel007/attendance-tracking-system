//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CandidateRow {
  /// Returns a new [CandidateRow] instance.
  CandidateRow({
    this.demographic,
    this.homeSabhaId,
    this.kshetraName,
    this.missedStreak,
    this.personId,
    this.personName,
    this.sabhaKind,
    this.tier,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? demographic;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? homeSabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? kshetraName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? missedStreak;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? personId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? personName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaKind;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? tier;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CandidateRow &&
    other.demographic == demographic &&
    other.homeSabhaId == homeSabhaId &&
    other.kshetraName == kshetraName &&
    other.missedStreak == missedStreak &&
    other.personId == personId &&
    other.personName == personName &&
    other.sabhaKind == sabhaKind &&
    other.tier == tier;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic == null ? 0 : demographic!.hashCode) +
    (homeSabhaId == null ? 0 : homeSabhaId!.hashCode) +
    (kshetraName == null ? 0 : kshetraName!.hashCode) +
    (missedStreak == null ? 0 : missedStreak!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (personName == null ? 0 : personName!.hashCode) +
    (sabhaKind == null ? 0 : sabhaKind!.hashCode) +
    (tier == null ? 0 : tier!.hashCode);

  @override
  String toString() => 'CandidateRow[demographic=$demographic, homeSabhaId=$homeSabhaId, kshetraName=$kshetraName, missedStreak=$missedStreak, personId=$personId, personName=$personName, sabhaKind=$sabhaKind, tier=$tier]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
    }
    if (this.homeSabhaId != null) {
      json[r'homeSabhaId'] = this.homeSabhaId;
    } else {
      json[r'homeSabhaId'] = null;
    }
    if (this.kshetraName != null) {
      json[r'kshetraName'] = this.kshetraName;
    } else {
      json[r'kshetraName'] = null;
    }
    if (this.missedStreak != null) {
      json[r'missedStreak'] = this.missedStreak;
    } else {
      json[r'missedStreak'] = null;
    }
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
    if (this.personName != null) {
      json[r'personName'] = this.personName;
    } else {
      json[r'personName'] = null;
    }
    if (this.sabhaKind != null) {
      json[r'sabhaKind'] = this.sabhaKind;
    } else {
      json[r'sabhaKind'] = null;
    }
    if (this.tier != null) {
      json[r'tier'] = this.tier;
    } else {
      json[r'tier'] = null;
    }
    return json;
  }

  /// Returns a new [CandidateRow] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CandidateRow? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CandidateRow(
        demographic: mapValueOfType<String>(json, r'demographic'),
        homeSabhaId: mapValueOfType<String>(json, r'homeSabhaId'),
        kshetraName: mapValueOfType<String>(json, r'kshetraName'),
        missedStreak: mapValueOfType<int>(json, r'missedStreak'),
        personId: mapValueOfType<String>(json, r'personId'),
        personName: mapValueOfType<String>(json, r'personName'),
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind'),
        tier: mapValueOfType<String>(json, r'tier'),
      );
    }
    return null;
  }

  static List<CandidateRow> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CandidateRow>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CandidateRow.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CandidateRow> mapFromJson(dynamic json) {
    final map = <String, CandidateRow>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CandidateRow.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CandidateRow-objects as value to a dart map
  static Map<String, List<CandidateRow>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CandidateRow>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CandidateRow.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

