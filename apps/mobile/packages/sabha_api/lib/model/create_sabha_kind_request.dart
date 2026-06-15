//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CreateSabhaKindRequest {
  /// Returns a new [CreateSabhaKindRequest] instance.
  CreateSabhaKindRequest({
    this.demographic,
    this.track,
  });

  CreateSabhaKindRequestDemographicEnum? demographic;

  CreateSabhaKindRequestTrackEnum? track;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CreateSabhaKindRequest &&
    other.demographic == demographic &&
    other.track == track;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (demographic == null ? 0 : demographic!.hashCode) +
    (track == null ? 0 : track!.hashCode);

  @override
  String toString() => 'CreateSabhaKindRequest[demographic=$demographic, track=$track]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
    }
    if (this.track != null) {
      json[r'track'] = this.track;
    } else {
      json[r'track'] = null;
    }
    return json;
  }

  /// Returns a new [CreateSabhaKindRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CreateSabhaKindRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CreateSabhaKindRequest(
        demographic: CreateSabhaKindRequestDemographicEnum.fromJson(json[r'demographic']),
        track: CreateSabhaKindRequestTrackEnum.fromJson(json[r'track']),
      );
    }
    return null;
  }

  static List<CreateSabhaKindRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CreateSabhaKindRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CreateSabhaKindRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CreateSabhaKindRequest> mapFromJson(dynamic json) {
    final map = <String, CreateSabhaKindRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CreateSabhaKindRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CreateSabhaKindRequest-objects as value to a dart map
  static Map<String, List<CreateSabhaKindRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CreateSabhaKindRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CreateSabhaKindRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class CreateSabhaKindRequestDemographicEnum {
  /// Instantiate a new enum with the provided [value].
  const CreateSabhaKindRequestDemographicEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const BAAL = CreateSabhaKindRequestDemographicEnum._(r'BAAL');
  static const BALIKA = CreateSabhaKindRequestDemographicEnum._(r'BALIKA');
  static const YUVAK = CreateSabhaKindRequestDemographicEnum._(r'YUVAK');
  static const YUVATI = CreateSabhaKindRequestDemographicEnum._(r'YUVATI');
  static const SANYUKTA = CreateSabhaKindRequestDemographicEnum._(r'SANYUKTA');

  /// List of all possible values in this [enum][CreateSabhaKindRequestDemographicEnum].
  static const values = <CreateSabhaKindRequestDemographicEnum>[
    BAAL,
    BALIKA,
    YUVAK,
    YUVATI,
    SANYUKTA,
  ];

  static CreateSabhaKindRequestDemographicEnum? fromJson(dynamic value) => CreateSabhaKindRequestDemographicEnumTypeTransformer().decode(value);

  static List<CreateSabhaKindRequestDemographicEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CreateSabhaKindRequestDemographicEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CreateSabhaKindRequestDemographicEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [CreateSabhaKindRequestDemographicEnum] to String,
/// and [decode] dynamic data back to [CreateSabhaKindRequestDemographicEnum].
class CreateSabhaKindRequestDemographicEnumTypeTransformer {
  factory CreateSabhaKindRequestDemographicEnumTypeTransformer() => _instance ??= const CreateSabhaKindRequestDemographicEnumTypeTransformer._();

  const CreateSabhaKindRequestDemographicEnumTypeTransformer._();

  String encode(CreateSabhaKindRequestDemographicEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a CreateSabhaKindRequestDemographicEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  CreateSabhaKindRequestDemographicEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'BAAL': return CreateSabhaKindRequestDemographicEnum.BAAL;
        case r'BALIKA': return CreateSabhaKindRequestDemographicEnum.BALIKA;
        case r'YUVAK': return CreateSabhaKindRequestDemographicEnum.YUVAK;
        case r'YUVATI': return CreateSabhaKindRequestDemographicEnum.YUVATI;
        case r'SANYUKTA': return CreateSabhaKindRequestDemographicEnum.SANYUKTA;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [CreateSabhaKindRequestDemographicEnumTypeTransformer] instance.
  static CreateSabhaKindRequestDemographicEnumTypeTransformer? _instance;
}



class CreateSabhaKindRequestTrackEnum {
  /// Instantiate a new enum with the provided [value].
  const CreateSabhaKindRequestTrackEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const REGULAR = CreateSabhaKindRequestTrackEnum._(r'REGULAR');
  static const BSS = CreateSabhaKindRequestTrackEnum._(r'BSS');
  static const YSS = CreateSabhaKindRequestTrackEnum._(r'YSS');

  /// List of all possible values in this [enum][CreateSabhaKindRequestTrackEnum].
  static const values = <CreateSabhaKindRequestTrackEnum>[
    REGULAR,
    BSS,
    YSS,
  ];

  static CreateSabhaKindRequestTrackEnum? fromJson(dynamic value) => CreateSabhaKindRequestTrackEnumTypeTransformer().decode(value);

  static List<CreateSabhaKindRequestTrackEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CreateSabhaKindRequestTrackEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CreateSabhaKindRequestTrackEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [CreateSabhaKindRequestTrackEnum] to String,
/// and [decode] dynamic data back to [CreateSabhaKindRequestTrackEnum].
class CreateSabhaKindRequestTrackEnumTypeTransformer {
  factory CreateSabhaKindRequestTrackEnumTypeTransformer() => _instance ??= const CreateSabhaKindRequestTrackEnumTypeTransformer._();

  const CreateSabhaKindRequestTrackEnumTypeTransformer._();

  String encode(CreateSabhaKindRequestTrackEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a CreateSabhaKindRequestTrackEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  CreateSabhaKindRequestTrackEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'REGULAR': return CreateSabhaKindRequestTrackEnum.REGULAR;
        case r'BSS': return CreateSabhaKindRequestTrackEnum.BSS;
        case r'YSS': return CreateSabhaKindRequestTrackEnum.YSS;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [CreateSabhaKindRequestTrackEnumTypeTransformer] instance.
  static CreateSabhaKindRequestTrackEnumTypeTransformer? _instance;
}


