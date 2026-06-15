//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PendingNominationItem {
  /// Returns a new [PendingNominationItem] instance.
  PendingNominationItem({
    this.demographic,
    this.nominatedAt,
    this.nominatedBy,
    this.nominatedByName,
    this.nominationId,
    this.personId,
    this.personName,
    this.regularSabhaId,
    this.selectiveSabhaId,
    this.track,
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
  DateTime? nominatedAt;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? nominatedBy;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? nominatedByName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? nominationId;

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
  String? regularSabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? selectiveSabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? track;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PendingNominationItem &&
    other.demographic == demographic &&
    other.nominatedAt == nominatedAt &&
    other.nominatedBy == nominatedBy &&
    other.nominatedByName == nominatedByName &&
    other.nominationId == nominationId &&
    other.personId == personId &&
    other.personName == personName &&
    other.regularSabhaId == regularSabhaId &&
    other.selectiveSabhaId == selectiveSabhaId &&
    other.track == track;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic == null ? 0 : demographic!.hashCode) +
    (nominatedAt == null ? 0 : nominatedAt!.hashCode) +
    (nominatedBy == null ? 0 : nominatedBy!.hashCode) +
    (nominatedByName == null ? 0 : nominatedByName!.hashCode) +
    (nominationId == null ? 0 : nominationId!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (personName == null ? 0 : personName!.hashCode) +
    (regularSabhaId == null ? 0 : regularSabhaId!.hashCode) +
    (selectiveSabhaId == null ? 0 : selectiveSabhaId!.hashCode) +
    (track == null ? 0 : track!.hashCode);

  @override
  String toString() => 'PendingNominationItem[demographic=$demographic, nominatedAt=$nominatedAt, nominatedBy=$nominatedBy, nominatedByName=$nominatedByName, nominationId=$nominationId, personId=$personId, personName=$personName, regularSabhaId=$regularSabhaId, selectiveSabhaId=$selectiveSabhaId, track=$track]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
    }
    if (this.nominatedAt != null) {
      json[r'nominatedAt'] = this.nominatedAt!.toUtc().toIso8601String();
    } else {
      json[r'nominatedAt'] = null;
    }
    if (this.nominatedBy != null) {
      json[r'nominatedBy'] = this.nominatedBy;
    } else {
      json[r'nominatedBy'] = null;
    }
    if (this.nominatedByName != null) {
      json[r'nominatedByName'] = this.nominatedByName;
    } else {
      json[r'nominatedByName'] = null;
    }
    if (this.nominationId != null) {
      json[r'nominationId'] = this.nominationId;
    } else {
      json[r'nominationId'] = null;
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
    if (this.regularSabhaId != null) {
      json[r'regularSabhaId'] = this.regularSabhaId;
    } else {
      json[r'regularSabhaId'] = null;
    }
    if (this.selectiveSabhaId != null) {
      json[r'selectiveSabhaId'] = this.selectiveSabhaId;
    } else {
      json[r'selectiveSabhaId'] = null;
    }
    if (this.track != null) {
      json[r'track'] = this.track;
    } else {
      json[r'track'] = null;
    }
    return json;
  }

  /// Returns a new [PendingNominationItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PendingNominationItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return PendingNominationItem(
        demographic: mapValueOfType<String>(json, r'demographic'),
        nominatedAt: mapDateTime(json, r'nominatedAt', r''),
        nominatedBy: mapValueOfType<String>(json, r'nominatedBy'),
        nominatedByName: mapValueOfType<String>(json, r'nominatedByName'),
        nominationId: mapValueOfType<String>(json, r'nominationId'),
        personId: mapValueOfType<String>(json, r'personId'),
        personName: mapValueOfType<String>(json, r'personName'),
        regularSabhaId: mapValueOfType<String>(json, r'regularSabhaId'),
        selectiveSabhaId: mapValueOfType<String>(json, r'selectiveSabhaId'),
        track: mapValueOfType<String>(json, r'track'),
      );
    }
    return null;
  }

  static List<PendingNominationItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PendingNominationItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PendingNominationItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PendingNominationItem> mapFromJson(dynamic json) {
    final map = <String, PendingNominationItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PendingNominationItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PendingNominationItem-objects as value to a dart map
  static Map<String, List<PendingNominationItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PendingNominationItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PendingNominationItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

