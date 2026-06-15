//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SelectedPersonItem {
  /// Returns a new [SelectedPersonItem] instance.
  SelectedPersonItem({
    this.decidedAt,
    this.decidedBy,
    this.decidedByName,
    this.demographic,
    this.nominationId,
    this.personId,
    this.personName,
    this.selectiveSabhaId,
    this.track,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? decidedAt;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? decidedBy;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? decidedByName;

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
  String? selectiveSabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? track;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SelectedPersonItem &&
    other.decidedAt == decidedAt &&
    other.decidedBy == decidedBy &&
    other.decidedByName == decidedByName &&
    other.demographic == demographic &&
    other.nominationId == nominationId &&
    other.personId == personId &&
    other.personName == personName &&
    other.selectiveSabhaId == selectiveSabhaId &&
    other.track == track;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (decidedAt == null ? 0 : decidedAt!.hashCode) +
    (decidedBy == null ? 0 : decidedBy!.hashCode) +
    (decidedByName == null ? 0 : decidedByName!.hashCode) +
    (demographic == null ? 0 : demographic!.hashCode) +
    (nominationId == null ? 0 : nominationId!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (personName == null ? 0 : personName!.hashCode) +
    (selectiveSabhaId == null ? 0 : selectiveSabhaId!.hashCode) +
    (track == null ? 0 : track!.hashCode);

  @override
  String toString() => 'SelectedPersonItem[decidedAt=$decidedAt, decidedBy=$decidedBy, decidedByName=$decidedByName, demographic=$demographic, nominationId=$nominationId, personId=$personId, personName=$personName, selectiveSabhaId=$selectiveSabhaId, track=$track]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.decidedAt != null) {
      json[r'decidedAt'] = this.decidedAt!.toUtc().toIso8601String();
    } else {
      json[r'decidedAt'] = null;
    }
    if (this.decidedBy != null) {
      json[r'decidedBy'] = this.decidedBy;
    } else {
      json[r'decidedBy'] = null;
    }
    if (this.decidedByName != null) {
      json[r'decidedByName'] = this.decidedByName;
    } else {
      json[r'decidedByName'] = null;
    }
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
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

  /// Returns a new [SelectedPersonItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SelectedPersonItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return SelectedPersonItem(
        decidedAt: mapDateTime(json, r'decidedAt', r''),
        decidedBy: mapValueOfType<String>(json, r'decidedBy'),
        decidedByName: mapValueOfType<String>(json, r'decidedByName'),
        demographic: mapValueOfType<String>(json, r'demographic'),
        nominationId: mapValueOfType<String>(json, r'nominationId'),
        personId: mapValueOfType<String>(json, r'personId'),
        personName: mapValueOfType<String>(json, r'personName'),
        selectiveSabhaId: mapValueOfType<String>(json, r'selectiveSabhaId'),
        track: mapValueOfType<String>(json, r'track'),
      );
    }
    return null;
  }

  static List<SelectedPersonItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SelectedPersonItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SelectedPersonItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SelectedPersonItem> mapFromJson(dynamic json) {
    final map = <String, SelectedPersonItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SelectedPersonItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SelectedPersonItem-objects as value to a dart map
  static Map<String, List<SelectedPersonItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SelectedPersonItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SelectedPersonItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

