//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class WalkInCandidate {
  /// Returns a new [WalkInCandidate] instance.
  WalkInCandidate({
    required this.fullName,
    this.homeSabhas = const [],
    required this.personId,
  });

  String fullName;

  List<String> homeSabhas;

  String personId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is WalkInCandidate &&
    other.fullName == fullName &&
    _deepEquality.equals(other.homeSabhas, homeSabhas) &&
    other.personId == personId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (fullName.hashCode) +
    (homeSabhas.hashCode) +
    (personId.hashCode);

  @override
  String toString() => 'WalkInCandidate[fullName=$fullName, homeSabhas=$homeSabhas, personId=$personId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'fullName'] = this.fullName;
      json[r'homeSabhas'] = this.homeSabhas;
      json[r'personId'] = this.personId;
    return json;
  }

  /// Returns a new [WalkInCandidate] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static WalkInCandidate? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'fullName'), 'Required key "WalkInCandidate[fullName]" is missing from JSON.');
        assert(json[r'fullName'] != null, 'Required key "WalkInCandidate[fullName]" has a null value in JSON.');
        assert(json.containsKey(r'homeSabhas'), 'Required key "WalkInCandidate[homeSabhas]" is missing from JSON.');
        assert(json[r'homeSabhas'] != null, 'Required key "WalkInCandidate[homeSabhas]" has a null value in JSON.');
        assert(json.containsKey(r'personId'), 'Required key "WalkInCandidate[personId]" is missing from JSON.');
        assert(json[r'personId'] != null, 'Required key "WalkInCandidate[personId]" has a null value in JSON.');
        return true;
      }());

      return WalkInCandidate(
        fullName: mapValueOfType<String>(json, r'fullName')!,
        homeSabhas: json[r'homeSabhas'] is Iterable
            ? (json[r'homeSabhas'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        personId: mapValueOfType<String>(json, r'personId')!,
      );
    }
    return null;
  }

  static List<WalkInCandidate> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <WalkInCandidate>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = WalkInCandidate.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, WalkInCandidate> mapFromJson(dynamic json) {
    final map = <String, WalkInCandidate>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = WalkInCandidate.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of WalkInCandidate-objects as value to a dart map
  static Map<String, List<WalkInCandidate>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<WalkInCandidate>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = WalkInCandidate.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'fullName',
    'homeSabhas',
    'personId',
  };
}

