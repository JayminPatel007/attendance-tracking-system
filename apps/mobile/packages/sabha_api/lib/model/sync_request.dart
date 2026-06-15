//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SyncRequest {
  /// Returns a new [SyncRequest] instance.
  SyncRequest({
    this.markings = const [],
    this.rosterVersion,
  });

  List<MarkingItem> markings;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? rosterVersion;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SyncRequest &&
    _deepEquality.equals(other.markings, markings) &&
    other.rosterVersion == rosterVersion;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (markings.hashCode) +
    (rosterVersion == null ? 0 : rosterVersion!.hashCode);

  @override
  String toString() => 'SyncRequest[markings=$markings, rosterVersion=$rosterVersion]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'markings'] = this.markings;
    if (this.rosterVersion != null) {
      json[r'rosterVersion'] = this.rosterVersion!.toUtc().toIso8601String();
    } else {
      json[r'rosterVersion'] = null;
    }
    return json;
  }

  /// Returns a new [SyncRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SyncRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return SyncRequest(
        markings: MarkingItem.listFromJson(json[r'markings']),
        rosterVersion: mapDateTime(json, r'rosterVersion', r''),
      );
    }
    return null;
  }

  static List<SyncRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SyncRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SyncRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SyncRequest> mapFromJson(dynamic json) {
    final map = <String, SyncRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SyncRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SyncRequest-objects as value to a dart map
  static Map<String, List<SyncRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SyncRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SyncRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

