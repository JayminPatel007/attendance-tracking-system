//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SabhaView {
  /// Returns a new [SabhaView] instance.
  SabhaView({
    required this.demographic,
    required this.id,
    required this.kshetraId,
    required this.kshetraName,
    required this.occurrenceCount,
    required this.standingVenue,
    required this.track,
  });

  SabhaViewDemographicEnum demographic;

  String id;

  String kshetraId;

  String kshetraName;

  int occurrenceCount;

  String standingVenue;

  SabhaViewTrackEnum track;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SabhaView &&
    other.demographic == demographic &&
    other.id == id &&
    other.kshetraId == kshetraId &&
    other.kshetraName == kshetraName &&
    other.occurrenceCount == occurrenceCount &&
    other.standingVenue == standingVenue &&
    other.track == track;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic.hashCode) +
    (id.hashCode) +
    (kshetraId.hashCode) +
    (kshetraName.hashCode) +
    (occurrenceCount.hashCode) +
    (standingVenue.hashCode) +
    (track.hashCode);

  @override
  String toString() => 'SabhaView[demographic=$demographic, id=$id, kshetraId=$kshetraId, kshetraName=$kshetraName, occurrenceCount=$occurrenceCount, standingVenue=$standingVenue, track=$track]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'demographic'] = this.demographic;
      json[r'id'] = this.id;
      json[r'kshetraId'] = this.kshetraId;
      json[r'kshetraName'] = this.kshetraName;
      json[r'occurrenceCount'] = this.occurrenceCount;
      json[r'standingVenue'] = this.standingVenue;
      json[r'track'] = this.track;
    return json;
  }

  /// Returns a new [SabhaView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SabhaView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'demographic'), 'Required key "SabhaView[demographic]" is missing from JSON.');
        assert(json[r'demographic'] != null, 'Required key "SabhaView[demographic]" has a null value in JSON.');
        assert(json.containsKey(r'id'), 'Required key "SabhaView[id]" is missing from JSON.');
        assert(json[r'id'] != null, 'Required key "SabhaView[id]" has a null value in JSON.');
        assert(json.containsKey(r'kshetraId'), 'Required key "SabhaView[kshetraId]" is missing from JSON.');
        assert(json[r'kshetraId'] != null, 'Required key "SabhaView[kshetraId]" has a null value in JSON.');
        assert(json.containsKey(r'kshetraName'), 'Required key "SabhaView[kshetraName]" is missing from JSON.');
        assert(json[r'kshetraName'] != null, 'Required key "SabhaView[kshetraName]" has a null value in JSON.');
        assert(json.containsKey(r'occurrenceCount'), 'Required key "SabhaView[occurrenceCount]" is missing from JSON.');
        assert(json[r'occurrenceCount'] != null, 'Required key "SabhaView[occurrenceCount]" has a null value in JSON.');
        assert(json.containsKey(r'standingVenue'), 'Required key "SabhaView[standingVenue]" is missing from JSON.');
        assert(json[r'standingVenue'] != null, 'Required key "SabhaView[standingVenue]" has a null value in JSON.');
        assert(json.containsKey(r'track'), 'Required key "SabhaView[track]" is missing from JSON.');
        assert(json[r'track'] != null, 'Required key "SabhaView[track]" has a null value in JSON.');
        return true;
      }());

      return SabhaView(
        demographic: SabhaViewDemographicEnum.fromJson(json[r'demographic'])!,
        id: mapValueOfType<String>(json, r'id')!,
        kshetraId: mapValueOfType<String>(json, r'kshetraId')!,
        kshetraName: mapValueOfType<String>(json, r'kshetraName')!,
        occurrenceCount: mapValueOfType<int>(json, r'occurrenceCount')!,
        standingVenue: mapValueOfType<String>(json, r'standingVenue')!,
        track: SabhaViewTrackEnum.fromJson(json[r'track'])!,
      );
    }
    return null;
  }

  static List<SabhaView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SabhaView> mapFromJson(dynamic json) {
    final map = <String, SabhaView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SabhaView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SabhaView-objects as value to a dart map
  static Map<String, List<SabhaView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SabhaView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SabhaView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'demographic',
    'id',
    'kshetraId',
    'kshetraName',
    'occurrenceCount',
    'standingVenue',
    'track',
  };
}


class SabhaViewDemographicEnum {
  /// Instantiate a new enum with the provided [value].
  const SabhaViewDemographicEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const BAAL = SabhaViewDemographicEnum._(r'BAAL');
  static const BALIKA = SabhaViewDemographicEnum._(r'BALIKA');
  static const YUVAK = SabhaViewDemographicEnum._(r'YUVAK');
  static const YUVATI = SabhaViewDemographicEnum._(r'YUVATI');
  static const SANYUKTA = SabhaViewDemographicEnum._(r'SANYUKTA');

  /// List of all possible values in this [enum][SabhaViewDemographicEnum].
  static const values = <SabhaViewDemographicEnum>[
    BAAL,
    BALIKA,
    YUVAK,
    YUVATI,
    SANYUKTA,
  ];

  static SabhaViewDemographicEnum? fromJson(dynamic value) => SabhaViewDemographicEnumTypeTransformer().decode(value);

  static List<SabhaViewDemographicEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaViewDemographicEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaViewDemographicEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [SabhaViewDemographicEnum] to String,
/// and [decode] dynamic data back to [SabhaViewDemographicEnum].
class SabhaViewDemographicEnumTypeTransformer {
  factory SabhaViewDemographicEnumTypeTransformer() => _instance ??= const SabhaViewDemographicEnumTypeTransformer._();

  const SabhaViewDemographicEnumTypeTransformer._();

  String encode(SabhaViewDemographicEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a SabhaViewDemographicEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  SabhaViewDemographicEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'BAAL': return SabhaViewDemographicEnum.BAAL;
        case r'BALIKA': return SabhaViewDemographicEnum.BALIKA;
        case r'YUVAK': return SabhaViewDemographicEnum.YUVAK;
        case r'YUVATI': return SabhaViewDemographicEnum.YUVATI;
        case r'SANYUKTA': return SabhaViewDemographicEnum.SANYUKTA;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [SabhaViewDemographicEnumTypeTransformer] instance.
  static SabhaViewDemographicEnumTypeTransformer? _instance;
}



class SabhaViewTrackEnum {
  /// Instantiate a new enum with the provided [value].
  const SabhaViewTrackEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const REGULAR = SabhaViewTrackEnum._(r'REGULAR');
  static const BSS = SabhaViewTrackEnum._(r'BSS');
  static const YSS = SabhaViewTrackEnum._(r'YSS');

  /// List of all possible values in this [enum][SabhaViewTrackEnum].
  static const values = <SabhaViewTrackEnum>[
    REGULAR,
    BSS,
    YSS,
  ];

  static SabhaViewTrackEnum? fromJson(dynamic value) => SabhaViewTrackEnumTypeTransformer().decode(value);

  static List<SabhaViewTrackEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaViewTrackEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaViewTrackEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [SabhaViewTrackEnum] to String,
/// and [decode] dynamic data back to [SabhaViewTrackEnum].
class SabhaViewTrackEnumTypeTransformer {
  factory SabhaViewTrackEnumTypeTransformer() => _instance ??= const SabhaViewTrackEnumTypeTransformer._();

  const SabhaViewTrackEnumTypeTransformer._();

  String encode(SabhaViewTrackEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a SabhaViewTrackEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  SabhaViewTrackEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'REGULAR': return SabhaViewTrackEnum.REGULAR;
        case r'BSS': return SabhaViewTrackEnum.BSS;
        case r'YSS': return SabhaViewTrackEnum.YSS;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [SabhaViewTrackEnumTypeTransformer] instance.
  static SabhaViewTrackEnumTypeTransformer? _instance;
}


