//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CurrentRoster {
  /// Returns a new [CurrentRoster] instance.
  CurrentRoster({
    this.occurrence,
    this.roster = const [],
    this.rosterVersion,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  OccurrenceView? occurrence;

  List<RosterEntry> roster;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? rosterVersion;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CurrentRoster &&
    other.occurrence == occurrence &&
    _deepEquality.equals(other.roster, roster) &&
    other.rosterVersion == rosterVersion;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (occurrence == null ? 0 : occurrence!.hashCode) +
    (roster.hashCode) +
    (rosterVersion == null ? 0 : rosterVersion!.hashCode);

  @override
  String toString() => 'CurrentRoster[occurrence=$occurrence, roster=$roster, rosterVersion=$rosterVersion]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.occurrence != null) {
      json[r'occurrence'] = this.occurrence;
    } else {
      json[r'occurrence'] = null;
    }
      json[r'roster'] = this.roster;
    if (this.rosterVersion != null) {
      json[r'rosterVersion'] = this.rosterVersion!.toUtc().toIso8601String();
    } else {
      json[r'rosterVersion'] = null;
    }
    return json;
  }

  /// Returns a new [CurrentRoster] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CurrentRoster? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CurrentRoster(
        occurrence: OccurrenceView.fromJson(json[r'occurrence']),
        roster: RosterEntry.listFromJson(json[r'roster']),
        rosterVersion: mapDateTime(json, r'rosterVersion', r''),
      );
    }
    return null;
  }

  static List<CurrentRoster> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CurrentRoster>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CurrentRoster.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CurrentRoster> mapFromJson(dynamic json) {
    final map = <String, CurrentRoster>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CurrentRoster.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CurrentRoster-objects as value to a dart map
  static Map<String, List<CurrentRoster>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CurrentRoster>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CurrentRoster.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

