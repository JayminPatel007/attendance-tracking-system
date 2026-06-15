//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MarkingItem {
  /// Returns a new [MarkingItem] instance.
  MarkingItem({
    this.clientMarkedAt,
    this.occurrenceId,
    this.personId,
    this.present,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? clientMarkedAt;

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
  String? personId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? present;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MarkingItem &&
    other.clientMarkedAt == clientMarkedAt &&
    other.occurrenceId == occurrenceId &&
    other.personId == personId &&
    other.present == present;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (clientMarkedAt == null ? 0 : clientMarkedAt!.hashCode) +
    (occurrenceId == null ? 0 : occurrenceId!.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (present == null ? 0 : present!.hashCode);

  @override
  String toString() => 'MarkingItem[clientMarkedAt=$clientMarkedAt, occurrenceId=$occurrenceId, personId=$personId, present=$present]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.clientMarkedAt != null) {
      json[r'clientMarkedAt'] = this.clientMarkedAt!.toUtc().toIso8601String();
    } else {
      json[r'clientMarkedAt'] = null;
    }
    if (this.occurrenceId != null) {
      json[r'occurrenceId'] = this.occurrenceId;
    } else {
      json[r'occurrenceId'] = null;
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

  /// Returns a new [MarkingItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MarkingItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return MarkingItem(
        clientMarkedAt: mapDateTime(json, r'clientMarkedAt', r''),
        occurrenceId: mapValueOfType<String>(json, r'occurrenceId'),
        personId: mapValueOfType<String>(json, r'personId'),
        present: mapValueOfType<bool>(json, r'present'),
      );
    }
    return null;
  }

  static List<MarkingItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MarkingItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MarkingItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MarkingItem> mapFromJson(dynamic json) {
    final map = <String, MarkingItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MarkingItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MarkingItem-objects as value to a dart map
  static Map<String, List<MarkingItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MarkingItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MarkingItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

