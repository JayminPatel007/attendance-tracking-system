//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AddPersonResponse {
  /// Returns a new [AddPersonResponse] instance.
  AddPersonResponse({
    this.candidates = const [],
    this.personId,
    required this.requiresOverride,
  });

  List<NameCandidate> candidates;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? personId;

  bool requiresOverride;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AddPersonResponse &&
    _deepEquality.equals(other.candidates, candidates) &&
    other.personId == personId &&
    other.requiresOverride == requiresOverride;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidates.hashCode) +
    (personId == null ? 0 : personId!.hashCode) +
    (requiresOverride.hashCode);

  @override
  String toString() => 'AddPersonResponse[candidates=$candidates, personId=$personId, requiresOverride=$requiresOverride]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'candidates'] = this.candidates;
    if (this.personId != null) {
      json[r'personId'] = this.personId;
    } else {
      json[r'personId'] = null;
    }
      json[r'requiresOverride'] = this.requiresOverride;
    return json;
  }

  /// Returns a new [AddPersonResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AddPersonResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'candidates'), 'Required key "AddPersonResponse[candidates]" is missing from JSON.');
        assert(json[r'candidates'] != null, 'Required key "AddPersonResponse[candidates]" has a null value in JSON.');
        assert(json.containsKey(r'requiresOverride'), 'Required key "AddPersonResponse[requiresOverride]" is missing from JSON.');
        assert(json[r'requiresOverride'] != null, 'Required key "AddPersonResponse[requiresOverride]" has a null value in JSON.');
        return true;
      }());

      return AddPersonResponse(
        candidates: NameCandidate.listFromJson(json[r'candidates']),
        personId: mapValueOfType<String>(json, r'personId'),
        requiresOverride: mapValueOfType<bool>(json, r'requiresOverride')!,
      );
    }
    return null;
  }

  static List<AddPersonResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AddPersonResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AddPersonResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AddPersonResponse> mapFromJson(dynamic json) {
    final map = <String, AddPersonResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AddPersonResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AddPersonResponse-objects as value to a dart map
  static Map<String, List<AddPersonResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AddPersonResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AddPersonResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'candidates',
    'requiresOverride',
  };
}

