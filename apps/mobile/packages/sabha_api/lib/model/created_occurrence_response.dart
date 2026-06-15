//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CreatedOccurrenceResponse {
  /// Returns a new [CreatedOccurrenceResponse] instance.
  CreatedOccurrenceResponse({
    this.occurrenceId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? occurrenceId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CreatedOccurrenceResponse &&
    other.occurrenceId == occurrenceId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (occurrenceId == null ? 0 : occurrenceId!.hashCode);

  @override
  String toString() => 'CreatedOccurrenceResponse[occurrenceId=$occurrenceId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.occurrenceId != null) {
      json[r'occurrenceId'] = this.occurrenceId;
    } else {
      json[r'occurrenceId'] = null;
    }
    return json;
  }

  /// Returns a new [CreatedOccurrenceResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CreatedOccurrenceResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CreatedOccurrenceResponse(
        occurrenceId: mapValueOfType<String>(json, r'occurrenceId'),
      );
    }
    return null;
  }

  static List<CreatedOccurrenceResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CreatedOccurrenceResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CreatedOccurrenceResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CreatedOccurrenceResponse> mapFromJson(dynamic json) {
    final map = <String, CreatedOccurrenceResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CreatedOccurrenceResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CreatedOccurrenceResponse-objects as value to a dart map
  static Map<String, List<CreatedOccurrenceResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CreatedOccurrenceResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CreatedOccurrenceResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

