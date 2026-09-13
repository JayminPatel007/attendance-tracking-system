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
    required this.date,
    required this.kshetraName,
    required this.lastReopenReason,
    required this.occurrenceId,
    required this.reopened,
    required this.sabhaKind,
    required this.state,
    required this.venue,
  });

  DateTime date;

  String kshetraName;

  String? lastReopenReason;

  String occurrenceId;

  bool reopened;

  String sabhaKind;

  String state;

  String venue;

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
    (date.hashCode) +
    (kshetraName.hashCode) +
    (lastReopenReason == null ? 0 : lastReopenReason!.hashCode) +
    (occurrenceId.hashCode) +
    (reopened.hashCode) +
    (sabhaKind.hashCode) +
    (state.hashCode) +
    (venue.hashCode);

  @override
  String toString() => 'ReopenListItem[date=$date, kshetraName=$kshetraName, lastReopenReason=$lastReopenReason, occurrenceId=$occurrenceId, reopened=$reopened, sabhaKind=$sabhaKind, state=$state, venue=$venue]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'date'] = _dateFormatter.format(this.date.toUtc());
      json[r'kshetraName'] = this.kshetraName;
    if (this.lastReopenReason != null) {
      json[r'lastReopenReason'] = this.lastReopenReason;
    } else {
      json[r'lastReopenReason'] = null;
    }
      json[r'occurrenceId'] = this.occurrenceId;
      json[r'reopened'] = this.reopened;
      json[r'sabhaKind'] = this.sabhaKind;
      json[r'state'] = this.state;
      json[r'venue'] = this.venue;
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
        assert(json.containsKey(r'date'), 'Required key "ReopenListItem[date]" is missing from JSON.');
        assert(json[r'date'] != null, 'Required key "ReopenListItem[date]" has a null value in JSON.');
        assert(json.containsKey(r'kshetraName'), 'Required key "ReopenListItem[kshetraName]" is missing from JSON.');
        assert(json[r'kshetraName'] != null, 'Required key "ReopenListItem[kshetraName]" has a null value in JSON.');
        assert(json.containsKey(r'lastReopenReason'), 'Required key "ReopenListItem[lastReopenReason]" is missing from JSON.');
        assert(json.containsKey(r'occurrenceId'), 'Required key "ReopenListItem[occurrenceId]" is missing from JSON.');
        assert(json[r'occurrenceId'] != null, 'Required key "ReopenListItem[occurrenceId]" has a null value in JSON.');
        assert(json.containsKey(r'reopened'), 'Required key "ReopenListItem[reopened]" is missing from JSON.');
        assert(json[r'reopened'] != null, 'Required key "ReopenListItem[reopened]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaKind'), 'Required key "ReopenListItem[sabhaKind]" is missing from JSON.');
        assert(json[r'sabhaKind'] != null, 'Required key "ReopenListItem[sabhaKind]" has a null value in JSON.');
        assert(json.containsKey(r'state'), 'Required key "ReopenListItem[state]" is missing from JSON.');
        assert(json[r'state'] != null, 'Required key "ReopenListItem[state]" has a null value in JSON.');
        assert(json.containsKey(r'venue'), 'Required key "ReopenListItem[venue]" is missing from JSON.');
        assert(json[r'venue'] != null, 'Required key "ReopenListItem[venue]" has a null value in JSON.');
        return true;
      }());

      return ReopenListItem(
        date: mapDateTime(json, r'date', r'')!,
        kshetraName: mapValueOfType<String>(json, r'kshetraName')!,
        lastReopenReason: mapValueOfType<String>(json, r'lastReopenReason'),
        occurrenceId: mapValueOfType<String>(json, r'occurrenceId')!,
        reopened: mapValueOfType<bool>(json, r'reopened')!,
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind')!,
        state: mapValueOfType<String>(json, r'state')!,
        venue: mapValueOfType<String>(json, r'venue')!,
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
    'date',
    'kshetraName',
    'lastReopenReason',
    'occurrenceId',
    'reopened',
    'sabhaKind',
    'state',
    'venue',
  };
}

