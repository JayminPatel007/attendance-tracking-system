//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CandidateRow {
  /// Returns a new [CandidateRow] instance.
  CandidateRow({
    required this.demographic,
    required this.homeSabhaId,
    required this.kshetraName,
    required this.missedStreak,
    required this.personId,
    required this.personName,
    required this.sabhaKind,
    required this.tier,
  });

  String demographic;

  String homeSabhaId;

  String kshetraName;

  int missedStreak;

  String personId;

  String personName;

  String sabhaKind;

  CandidateRowTierEnum tier;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CandidateRow &&
    other.demographic == demographic &&
    other.homeSabhaId == homeSabhaId &&
    other.kshetraName == kshetraName &&
    other.missedStreak == missedStreak &&
    other.personId == personId &&
    other.personName == personName &&
    other.sabhaKind == sabhaKind &&
    other.tier == tier;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic.hashCode) +
    (homeSabhaId.hashCode) +
    (kshetraName.hashCode) +
    (missedStreak.hashCode) +
    (personId.hashCode) +
    (personName.hashCode) +
    (sabhaKind.hashCode) +
    (tier.hashCode);

  @override
  String toString() => 'CandidateRow[demographic=$demographic, homeSabhaId=$homeSabhaId, kshetraName=$kshetraName, missedStreak=$missedStreak, personId=$personId, personName=$personName, sabhaKind=$sabhaKind, tier=$tier]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'demographic'] = this.demographic;
      json[r'homeSabhaId'] = this.homeSabhaId;
      json[r'kshetraName'] = this.kshetraName;
      json[r'missedStreak'] = this.missedStreak;
      json[r'personId'] = this.personId;
      json[r'personName'] = this.personName;
      json[r'sabhaKind'] = this.sabhaKind;
      json[r'tier'] = this.tier;
    return json;
  }

  /// Returns a new [CandidateRow] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CandidateRow? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'demographic'), 'Required key "CandidateRow[demographic]" is missing from JSON.');
        assert(json[r'demographic'] != null, 'Required key "CandidateRow[demographic]" has a null value in JSON.');
        assert(json.containsKey(r'homeSabhaId'), 'Required key "CandidateRow[homeSabhaId]" is missing from JSON.');
        assert(json[r'homeSabhaId'] != null, 'Required key "CandidateRow[homeSabhaId]" has a null value in JSON.');
        assert(json.containsKey(r'kshetraName'), 'Required key "CandidateRow[kshetraName]" is missing from JSON.');
        assert(json[r'kshetraName'] != null, 'Required key "CandidateRow[kshetraName]" has a null value in JSON.');
        assert(json.containsKey(r'missedStreak'), 'Required key "CandidateRow[missedStreak]" is missing from JSON.');
        assert(json[r'missedStreak'] != null, 'Required key "CandidateRow[missedStreak]" has a null value in JSON.');
        assert(json.containsKey(r'personId'), 'Required key "CandidateRow[personId]" is missing from JSON.');
        assert(json[r'personId'] != null, 'Required key "CandidateRow[personId]" has a null value in JSON.');
        assert(json.containsKey(r'personName'), 'Required key "CandidateRow[personName]" is missing from JSON.');
        assert(json[r'personName'] != null, 'Required key "CandidateRow[personName]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaKind'), 'Required key "CandidateRow[sabhaKind]" is missing from JSON.');
        assert(json[r'sabhaKind'] != null, 'Required key "CandidateRow[sabhaKind]" has a null value in JSON.');
        assert(json.containsKey(r'tier'), 'Required key "CandidateRow[tier]" is missing from JSON.');
        assert(json[r'tier'] != null, 'Required key "CandidateRow[tier]" has a null value in JSON.');
        return true;
      }());

      return CandidateRow(
        demographic: mapValueOfType<String>(json, r'demographic')!,
        homeSabhaId: mapValueOfType<String>(json, r'homeSabhaId')!,
        kshetraName: mapValueOfType<String>(json, r'kshetraName')!,
        missedStreak: mapValueOfType<int>(json, r'missedStreak')!,
        personId: mapValueOfType<String>(json, r'personId')!,
        personName: mapValueOfType<String>(json, r'personName')!,
        sabhaKind: mapValueOfType<String>(json, r'sabhaKind')!,
        tier: CandidateRowTierEnum.fromJson(json[r'tier'])!,
      );
    }
    return null;
  }

  static List<CandidateRow> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CandidateRow>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CandidateRow.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CandidateRow> mapFromJson(dynamic json) {
    final map = <String, CandidateRow>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CandidateRow.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CandidateRow-objects as value to a dart map
  static Map<String, List<CandidateRow>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CandidateRow>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CandidateRow.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'demographic',
    'homeSabhaId',
    'kshetraName',
    'missedStreak',
    'personId',
    'personName',
    'sabhaKind',
    'tier',
  };
}


class CandidateRowTierEnum {
  /// Instantiate a new enum with the provided [value].
  const CandidateRowTierEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const CANDIDATE = CandidateRowTierEnum._(r'CANDIDATE');
  static const PRIORITY = CandidateRowTierEnum._(r'PRIORITY');

  /// List of all possible values in this [enum][CandidateRowTierEnum].
  static const values = <CandidateRowTierEnum>[
    CANDIDATE,
    PRIORITY,
  ];

  static CandidateRowTierEnum? fromJson(dynamic value) => CandidateRowTierEnumTypeTransformer().decode(value);

  static List<CandidateRowTierEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CandidateRowTierEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CandidateRowTierEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [CandidateRowTierEnum] to String,
/// and [decode] dynamic data back to [CandidateRowTierEnum].
class CandidateRowTierEnumTypeTransformer {
  factory CandidateRowTierEnumTypeTransformer() => _instance ??= const CandidateRowTierEnumTypeTransformer._();

  const CandidateRowTierEnumTypeTransformer._();

  String encode(CandidateRowTierEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a CandidateRowTierEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  CandidateRowTierEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'CANDIDATE': return CandidateRowTierEnum.CANDIDATE;
        case r'PRIORITY': return CandidateRowTierEnum.PRIORITY;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [CandidateRowTierEnumTypeTransformer] instance.
  static CandidateRowTierEnumTypeTransformer? _instance;
}


