//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class NameCandidate {
  /// Returns a new [NameCandidate] instance.
  NameCandidate({
    this.fullName,
    this.homeSabhas = const [],
    this.personId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? fullName;

  List<String> homeSabhas;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? personId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is NameCandidate &&
    other.fullName == fullName &&
    _deepEquality.equals(other.homeSabhas, homeSabhas) &&
    other.personId == personId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (fullName == null ? 0 : fullName!.hashCode) +
    (homeSabhas.hashCode) +
    (personId == null ? 0 : personId!.hashCode);

  @override
  String toString() => 'NameCandidate[fullName=$fullName, homeSabhas=$homeSabhas, personId=$personId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.fullName != null) {
      json[r'fullName'] = this.fullName;
    } else {
      json[r'fullName'] = null;
    }
      json[r'homeSabhas'] = this.homeSabhas;
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
    return json;
  }

  /// Returns a new [NameCandidate] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static NameCandidate? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return NameCandidate(
        fullName: mapValueOfType<String>(json, r'fullName'),
        homeSabhas: json[r'homeSabhas'] is Iterable
            ? (json[r'homeSabhas'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        personId: mapValueOfType<String>(json, r'personId'),
      );
    }
    return null;
  }

  static List<NameCandidate> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <NameCandidate>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = NameCandidate.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, NameCandidate> mapFromJson(dynamic json) {
    final map = <String, NameCandidate>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = NameCandidate.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of NameCandidate-objects as value to a dart map
  static Map<String, List<NameCandidate>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<NameCandidate>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = NameCandidate.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

