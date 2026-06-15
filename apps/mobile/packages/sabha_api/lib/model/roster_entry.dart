//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class RosterEntry {
  /// Returns a new [RosterEntry] instance.
  RosterEntry({
    this.fullName,
    this.personId,
    this.present,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? fullName;

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
  bool? present;

  @override
  bool operator ==(Object other) => identical(this, other) || other is RosterEntry &&
    other.fullName == fullName &&
    other.personId == personId &&
    other.present == present;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (fullName == null ? 0 : fullName!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (present == null ? 0 : present!.hashCode);

  @override
  String toString() => 'RosterEntry[fullName=$fullName, personId=$personId, present=$present]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.fullName != null) {
      json[r'fullName'] = this.fullName;
    } else {
      json[r'fullName'] = null;
    }
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
    if (this.present != null) {
      json[r'present'] = this.present;
    } else {
      json[r'present'] = null;
    }
    return json;
  }

  /// Returns a new [RosterEntry] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static RosterEntry? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return RosterEntry(
        fullName: mapValueOfType<String>(json, r'fullName'),
        personId: mapValueOfType<String>(json, r'personId'),
        present: mapValueOfType<bool>(json, r'present'),
      );
    }
    return null;
  }

  static List<RosterEntry> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <RosterEntry>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = RosterEntry.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, RosterEntry> mapFromJson(dynamic json) {
    final map = <String, RosterEntry>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = RosterEntry.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of RosterEntry-objects as value to a dart map
  static Map<String, List<RosterEntry>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<RosterEntry>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = RosterEntry.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

