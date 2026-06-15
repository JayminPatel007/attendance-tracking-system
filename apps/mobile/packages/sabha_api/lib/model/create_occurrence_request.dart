//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CreateOccurrenceRequest {
  /// Returns a new [CreateOccurrenceRequest] instance.
  CreateOccurrenceRequest({
    this.date,
    this.endTime,
    this.startTime,
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
  String? endTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? startTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? venue;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CreateOccurrenceRequest &&
    other.date == date &&
    other.endTime == endTime &&
    other.startTime == startTime &&
    other.venue == venue;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (date == null ? 0 : date!.hashCode) +
    (endTime == null ? 0 : endTime!.hashCode) +
    (startTime == null ? 0 : startTime!.hashCode) +
    (venue == null ? 0 : venue!.hashCode);

  @override
  String toString() => 'CreateOccurrenceRequest[date=$date, endTime=$endTime, startTime=$startTime, venue=$venue]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.date != null) {
      json[r'date'] = _dateFormatter.format(this.date!.toUtc());
    } else {
      json[r'date'] = null;
    }
    if (this.endTime != null) {
      json[r'endTime'] = this.endTime;
    } else {
      json[r'endTime'] = null;
    }
    if (this.startTime != null) {
      json[r'startTime'] = this.startTime;
    } else {
      json[r'startTime'] = null;
    }
    if (this.venue != null) {
      json[r'venue'] = this.venue;
    } else {
      json[r'venue'] = null;
    }
    return json;
  }

  /// Returns a new [CreateOccurrenceRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CreateOccurrenceRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CreateOccurrenceRequest(
        date: mapDateTime(json, r'date', r''),
        endTime: mapValueOfType<String>(json, r'endTime'),
        startTime: mapValueOfType<String>(json, r'startTime'),
        venue: mapValueOfType<String>(json, r'venue'),
      );
    }
    return null;
  }

  static List<CreateOccurrenceRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CreateOccurrenceRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CreateOccurrenceRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CreateOccurrenceRequest> mapFromJson(dynamic json) {
    final map = <String, CreateOccurrenceRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CreateOccurrenceRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CreateOccurrenceRequest-objects as value to a dart map
  static Map<String, List<CreateOccurrenceRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CreateOccurrenceRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CreateOccurrenceRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

