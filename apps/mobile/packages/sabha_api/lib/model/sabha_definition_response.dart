//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SabhaDefinitionResponse {
  /// Returns a new [SabhaDefinitionResponse] instance.
  SabhaDefinitionResponse({
    this.candidates = const [],
    required this.requiresOverride,
    required this.sabhaId,
    required this.sahSanchalakAssignmentId,
    required this.sanchalakAssignmentId,
  });

  List<NameCandidate> candidates;

  bool requiresOverride;

  String? sabhaId;

  String? sahSanchalakAssignmentId;

  String? sanchalakAssignmentId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SabhaDefinitionResponse &&
    _deepEquality.equals(other.candidates, candidates) &&
    other.requiresOverride == requiresOverride &&
    other.sabhaId == sabhaId &&
    other.sahSanchalakAssignmentId == sahSanchalakAssignmentId &&
    other.sanchalakAssignmentId == sanchalakAssignmentId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (candidates.hashCode) +
    (requiresOverride.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (sahSanchalakAssignmentId == null ? 0 : sahSanchalakAssignmentId!.hashCode) +
    (sanchalakAssignmentId == null ? 0 : sanchalakAssignmentId!.hashCode);

  @override
  String toString() => 'SabhaDefinitionResponse[candidates=$candidates, requiresOverride=$requiresOverride, sabhaId=$sabhaId, sahSanchalakAssignmentId=$sahSanchalakAssignmentId, sanchalakAssignmentId=$sanchalakAssignmentId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'candidates'] = this.candidates;
      json[r'requiresOverride'] = this.requiresOverride;
    if (this.sabhaId != null) {
      json[r'sabhaId'] = this.sabhaId;
    } else {
      json[r'sabhaId'] = null;
    }
    if (this.sahSanchalakAssignmentId != null) {
      json[r'sahSanchalakAssignmentId'] = this.sahSanchalakAssignmentId;
    } else {
      json[r'sahSanchalakAssignmentId'] = null;
    }
    if (this.sanchalakAssignmentId != null) {
      json[r'sanchalakAssignmentId'] = this.sanchalakAssignmentId;
    } else {
      json[r'sanchalakAssignmentId'] = null;
    }
    return json;
  }

  /// Returns a new [SabhaDefinitionResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SabhaDefinitionResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'candidates'), 'Required key "SabhaDefinitionResponse[candidates]" is missing from JSON.');
        assert(json[r'candidates'] != null, 'Required key "SabhaDefinitionResponse[candidates]" has a null value in JSON.');
        assert(json.containsKey(r'requiresOverride'), 'Required key "SabhaDefinitionResponse[requiresOverride]" is missing from JSON.');
        assert(json[r'requiresOverride'] != null, 'Required key "SabhaDefinitionResponse[requiresOverride]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaId'), 'Required key "SabhaDefinitionResponse[sabhaId]" is missing from JSON.');
        assert(json.containsKey(r'sahSanchalakAssignmentId'), 'Required key "SabhaDefinitionResponse[sahSanchalakAssignmentId]" is missing from JSON.');
        assert(json.containsKey(r'sanchalakAssignmentId'), 'Required key "SabhaDefinitionResponse[sanchalakAssignmentId]" is missing from JSON.');
        return true;
      }());

      return SabhaDefinitionResponse(
        candidates: NameCandidate.listFromJson(json[r'candidates']),
        requiresOverride: mapValueOfType<bool>(json, r'requiresOverride')!,
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        sahSanchalakAssignmentId: mapValueOfType<String>(json, r'sahSanchalakAssignmentId'),
        sanchalakAssignmentId: mapValueOfType<String>(json, r'sanchalakAssignmentId'),
      );
    }
    return null;
  }

  static List<SabhaDefinitionResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaDefinitionResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaDefinitionResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SabhaDefinitionResponse> mapFromJson(dynamic json) {
    final map = <String, SabhaDefinitionResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SabhaDefinitionResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SabhaDefinitionResponse-objects as value to a dart map
  static Map<String, List<SabhaDefinitionResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SabhaDefinitionResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SabhaDefinitionResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'candidates',
    'requiresOverride',
    'sabhaId',
    'sahSanchalakAssignmentId',
    'sanchalakAssignmentId',
  };
}

