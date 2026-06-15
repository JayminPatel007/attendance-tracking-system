//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ReopenListItem {
  /// Returns a new [ReopenListItem] instance.
  ReopenListItem({
    this.date,
    this.kshetraName,
    this.lastReopenReason,
    this.occurrenceId,
    this.reopened,
    this.sabhaKind,
    this.state,
    this.venue,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? date;

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
  String? lastReopenReason;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? occurrenceId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? reopened;

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
  String? state;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? venue;

  @override
  bool operator ==(Object other) => identical(this, other) || other is ReopenListItem &&
    other.date == date &&
    other.kshetraName == kshetraName &&
    other.lastReopenReason == lastReopenReason &&
    other.occurrenceId == occurrenceId &&
    other.reopened == reopened &&
    other.sabhaKind == sabhaKind &&
    other.state == state &&
    other.venue == venue;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (date == null ? 0 : date!.hashCode) +
    (kshetraName == null ? 0 : kshetraName!.hashCode) +
    (lastReopenReason == null ? 0 : lastReopenReason!.hashCode) +
    (occurrenceId == null ? 0 : occurrenceId!.hashCode) +
    (reopened == null ? 0 : reopened!.hashCode) +
    (sabhaKind == null ? 0 : sabhaKind!.hashCode) +
    (state == null ? 0 : state!.hashCode) +
    (venue == null ? 0 : venue!.hashCode);

  @override
  String toString() => 'ReopenListItem[date=$date, kshetraName=$kshetraName, lastReopenReason=$lastReopenReason, occurrenceId=$occurrenceId, reopened=$reopened, sabhaKind=$sabhaKind, state=$state, venue=$venue]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.date != null) {
      json[r'date'] = _dateFormatter.format(this.date!.toUtc());
    } else {
      json[r'date'] = null;
    }
    if (this.kshetraName != null) {
      json[r'kshetraName'] = this.kshetraName;
    } else {
      json[r'kshetraName'] = null;
    }
    if (this.lastReopenReason != null) {
      json[r'lastReopenReason'] = this.lastReopenReason;
    } else {
      json[r'lastReopenReason'] = null;
    }
    if (this.occurrenceId != null) {
      json[r'occurrenceId'] = this.occurrenceId;
    } else {
      json[r'occurrenceId'] = null;
    }
    if (this.reopened != null) {
      json[r'reopened'] = this.reopened;
    } else {
      json[r'reopened'] = null;
    }
    if (this.sabhaKind != null) {
      json[r'sabhaKind'] = this.sabhaKind;
    } else {
      json[r'sabhaKind'] = null;
    }
    if (this.state != null) {
      json[r'state'] = this.state;
    } else {
      json[r'state'] = null;
    }
    if (this.venue != null) {
      json[r'venue'] = this.venue;
    } else {
      json[r'venue'] = null;
    }
    return json;
  }

  /// Returns a new [ReopenListItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ReopenListItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return ReopenListItem(
        date: mapDateTime(json, r'date', r''),
        kshetraName: mapValueOfType<String>(json, r'kshetraName'),
        lastReopenReason: mapValueOfType<String>(json, r'lastReopenReason'),
        occurrenceId: mapValueOfType<String>(json, r'occurrenceId'),
        reopened: mapValueOfType<bool>(json, r'reopened'),
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind'),
        state: mapValueOfType<String>(json, r'state'),
        venue: mapValueOfType<String>(json, r'venue'),
      );
    }
    return null;
  }

  static List<ReopenListItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ReopenListItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ReopenListItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ReopenListItem> mapFromJson(dynamic json) {
    final map = <String, ReopenListItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ReopenListItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ReopenListItem-objects as value to a dart map
  static Map<String, List<ReopenListItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ReopenListItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ReopenListItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

