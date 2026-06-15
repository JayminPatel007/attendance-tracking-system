//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ProxyOccurrenceItem {
  /// Returns a new [ProxyOccurrenceItem] instance.
  ProxyOccurrenceItem({
    this.effectiveDate,
    this.id,
    this.state,
    this.venue,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? effectiveDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? id;

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
  bool operator ==(Object other) => identical(this, other) || other is ProxyOccurrenceItem &&
    other.effectiveDate == effectiveDate &&
    other.id == id &&
    other.state == state &&
    other.venue == venue;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (effectiveDate == null ? 0 : effectiveDate!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (state == null ? 0 : state!.hashCode) +
    (venue == null ? 0 : venue!.hashCode);

  @override
  String toString() => 'ProxyOccurrenceItem[effectiveDate=$effectiveDate, id=$id, state=$state, venue=$venue]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.effectiveDate != null) {
      json[r'effectiveDate'] = _dateFormatter.format(this.effectiveDate!.toUtc());
    } else {
      json[r'effectiveDate'] = null;
    }
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
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

  /// Returns a new [ProxyOccurrenceItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ProxyOccurrenceItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return ProxyOccurrenceItem(
        effectiveDate: mapDateTime(json, r'effectiveDate', r''),
        id: mapValueOfType<String>(json, r'id'),
        state: mapValueOfType<String>(json, r'state'),
        venue: mapValueOfType<String>(json, r'venue'),
      );
    }
    return null;
  }

  static List<ProxyOccurrenceItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ProxyOccurrenceItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ProxyOccurrenceItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ProxyOccurrenceItem> mapFromJson(dynamic json) {
    final map = <String, ProxyOccurrenceItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ProxyOccurrenceItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ProxyOccurrenceItem-objects as value to a dart map
  static Map<String, List<ProxyOccurrenceItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ProxyOccurrenceItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ProxyOccurrenceItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

