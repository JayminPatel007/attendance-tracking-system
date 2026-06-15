//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class SabhaKindView {
  /// Returns a new [SabhaKindView] instance.
  SabhaKindView({
    this.demographic,
    this.id,
    this.track,
  });

  SabhaKindViewDemographicEnum? demographic;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? id;

  SabhaKindViewTrackEnum? track;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SabhaKindView &&
    other.demographic == demographic &&
    other.id == id &&
    other.track == track;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic == null ? 0 : demographic!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (track == null ? 0 : track!.hashCode);

  @override
  String toString() => 'SabhaKindView[demographic=$demographic, id=$id, track=$track]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
    }
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
    if (this.track != null) {
      json[r'track'] = this.track;
    } else {
      json[r'track'] = null;
    }
    return json;
  }

  /// Returns a new [SabhaKindView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static SabhaKindView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return SabhaKindView(
        demographic: SabhaKindViewDemographicEnum.fromJson(json[r'demographic']),
        id: mapValueOfType<String>(json, r'id'),
        track: SabhaKindViewTrackEnum.fromJson(json[r'track']),
      );
    }
    return null;
  }

  static List<SabhaKindView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaKindView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaKindView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SabhaKindView> mapFromJson(dynamic json) {
    final map = <String, SabhaKindView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = SabhaKindView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SabhaKindView-objects as value to a dart map
  static Map<String, List<SabhaKindView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SabhaKindView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SabhaKindView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class SabhaKindViewDemographicEnum {
  /// Instantiate a new enum with the provided [value].
  const SabhaKindViewDemographicEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const BAAL = SabhaKindViewDemographicEnum._(r'BAAL');
  static const BALIKA = SabhaKindViewDemographicEnum._(r'BALIKA');
  static const YUVAK = SabhaKindViewDemographicEnum._(r'YUVAK');
  static const YUVATI = SabhaKindViewDemographicEnum._(r'YUVATI');
  static const SANYUKTA = SabhaKindViewDemographicEnum._(r'SANYUKTA');

  /// List of all possible values in this [enum][SabhaKindViewDemographicEnum].
  static const values = <SabhaKindViewDemographicEnum>[
    BAAL,
    BALIKA,
    YUVAK,
    YUVATI,
    SANYUKTA,
  ];

  static SabhaKindViewDemographicEnum? fromJson(dynamic value) => SabhaKindViewDemographicEnumTypeTransformer().decode(value);

  static List<SabhaKindViewDemographicEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaKindViewDemographicEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaKindViewDemographicEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [SabhaKindViewDemographicEnum] to String,
/// and [decode] dynamic data back to [SabhaKindViewDemographicEnum].
class SabhaKindViewDemographicEnumTypeTransformer {
  factory SabhaKindViewDemographicEnumTypeTransformer() => _instance ??= const SabhaKindViewDemographicEnumTypeTransformer._();

  const SabhaKindViewDemographicEnumTypeTransformer._();

  String encode(SabhaKindViewDemographicEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a SabhaKindViewDemographicEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  SabhaKindViewDemographicEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'BAAL': return SabhaKindViewDemographicEnum.BAAL;
        case r'BALIKA': return SabhaKindViewDemographicEnum.BALIKA;
        case r'YUVAK': return SabhaKindViewDemographicEnum.YUVAK;
        case r'YUVATI': return SabhaKindViewDemographicEnum.YUVATI;
        case r'SANYUKTA': return SabhaKindViewDemographicEnum.SANYUKTA;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [SabhaKindViewDemographicEnumTypeTransformer] instance.
  static SabhaKindViewDemographicEnumTypeTransformer? _instance;
}



class SabhaKindViewTrackEnum {
  /// Instantiate a new enum with the provided [value].
  const SabhaKindViewTrackEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const REGULAR = SabhaKindViewTrackEnum._(r'REGULAR');
  static const BSS = SabhaKindViewTrackEnum._(r'BSS');
  static const YSS = SabhaKindViewTrackEnum._(r'YSS');

  /// List of all possible values in this [enum][SabhaKindViewTrackEnum].
  static const values = <SabhaKindViewTrackEnum>[
    REGULAR,
    BSS,
    YSS,
  ];

  static SabhaKindViewTrackEnum? fromJson(dynamic value) => SabhaKindViewTrackEnumTypeTransformer().decode(value);

  static List<SabhaKindViewTrackEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SabhaKindViewTrackEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SabhaKindViewTrackEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [SabhaKindViewTrackEnum] to String,
/// and [decode] dynamic data back to [SabhaKindViewTrackEnum].
class SabhaKindViewTrackEnumTypeTransformer {
  factory SabhaKindViewTrackEnumTypeTransformer() => _instance ??= const SabhaKindViewTrackEnumTypeTransformer._();

  const SabhaKindViewTrackEnumTypeTransformer._();

  String encode(SabhaKindViewTrackEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a SabhaKindViewTrackEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  SabhaKindViewTrackEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'REGULAR': return SabhaKindViewTrackEnum.REGULAR;
        case r'BSS': return SabhaKindViewTrackEnum.BSS;
        case r'YSS': return SabhaKindViewTrackEnum.YSS;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [SabhaKindViewTrackEnumTypeTransformer] instance.
  static SabhaKindViewTrackEnumTypeTransformer? _instance;
}


