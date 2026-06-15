//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MonthlySabha {
  /// Returns a new [MonthlySabha] instance.
  MonthlySabha({
    this.needsOccurrence,
    this.sabhaId,
    this.sabhaKind,
    this.standingVenue,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? needsOccurrence;

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

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? standingVenue;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MonthlySabha &&
    other.needsOccurrence == needsOccurrence &&
    other.sabhaId == sabhaId &&
    other.sabhaKind == sabhaKind &&
    other.standingVenue == standingVenue;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (needsOccurrence == null ? 0 : needsOccurrence!.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (sabhaKind == null ? 0 : sabhaKind!.hashCode) +
    (standingVenue == null ? 0 : standingVenue!.hashCode);

  @override
  String toString() => 'MonthlySabha[needsOccurrence=$needsOccurrence, sabhaId=$sabhaId, sabhaKind=$sabhaKind, standingVenue=$standingVenue]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.needsOccurrence != null) {
      json[r'needsOccurrence'] = this.needsOccurrence;
    } else {
      json[r'needsOccurrence'] = null;
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
    if (this.standingVenue != null) {
      json[r'standingVenue'] = this.standingVenue;
    } else {
      json[r'standingVenue'] = null;
    }
    return json;
  }

  /// Returns a new [MonthlySabha] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MonthlySabha? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return MonthlySabha(
        needsOccurrence: mapValueOfType<bool>(json, r'needsOccurrence'),
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind'),
        standingVenue: mapValueOfType<String>(json, r'standingVenue'),
      );
    }
    return null;
  }

  static List<MonthlySabha> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MonthlySabha>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MonthlySabha.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MonthlySabha> mapFromJson(dynamic json) {
    final map = <String, MonthlySabha>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MonthlySabha.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MonthlySabha-objects as value to a dart map
  static Map<String, List<MonthlySabha>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MonthlySabha>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MonthlySabha.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

