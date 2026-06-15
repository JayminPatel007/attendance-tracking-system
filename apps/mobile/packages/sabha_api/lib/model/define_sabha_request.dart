//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class DefineSabhaRequest {
  /// Returns a new [DefineSabhaRequest] instance.
  DefineSabhaRequest({
    this.dayOfWeek,
    this.endTime,
    this.kshetraId,
    this.sabhaKindId,
    this.sahSanchalak,
    this.sanchalak,
    this.standingVenue,
    this.startTime,
    this.weekly,
  });

  DefineSabhaRequestDayOfWeekEnum? dayOfWeek;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? endTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? kshetraId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaKindId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  AppointeePayload? sahSanchalak;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  AppointeePayload? sanchalak;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? standingVenue;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? startTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? weekly;

  @override
  bool operator ==(Object other) => identical(this, other) || other is DefineSabhaRequest &&
    other.dayOfWeek == dayOfWeek &&
    other.endTime == endTime &&
    other.kshetraId == kshetraId &&
    other.sabhaKindId == sabhaKindId &&
    other.sahSanchalak == sahSanchalak &&
    other.sanchalak == sanchalak &&
    other.standingVenue == standingVenue &&
    other.startTime == startTime &&
    other.weekly == weekly;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (dayOfWeek == null ? 0 : dayOfWeek!.hashCode) +
    (endTime == null ? 0 : endTime!.hashCode) +
    (kshetraId == null ? 0 : kshetraId!.hashCode) +
    (sabhaKindId == null ? 0 : sabhaKindId!.hashCode) +
    (sahSanchalak == null ? 0 : sahSanchalak!.hashCode) +
    (sanchalak == null ? 0 : sanchalak!.hashCode) +
    (standingVenue == null ? 0 : standingVenue!.hashCode) +
    (startTime == null ? 0 : startTime!.hashCode) +
    (weekly == null ? 0 : weekly!.hashCode);

  @override
  String toString() => 'DefineSabhaRequest[dayOfWeek=$dayOfWeek, endTime=$endTime, kshetraId=$kshetraId, sabhaKindId=$sabhaKindId, sahSanchalak=$sahSanchalak, sanchalak=$sanchalak, standingVenue=$standingVenue, startTime=$startTime, weekly=$weekly]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.dayOfWeek != null) {
      json[r'dayOfWeek'] = this.dayOfWeek;
    } else {
      json[r'dayOfWeek'] = null;
    }
    if (this.endTime != null) {
      json[r'endTime'] = this.endTime;
    } else {
      json[r'endTime'] = null;
    }
    if (this.kshetraId != null) {
      json[r'kshetraId'] = this.kshetraId;
    } else {
      json[r'kshetraId'] = null;
    }
    if (this.sabhaKindId != null) {
      json[r'sabhaKindId'] = this.sabhaKindId;
    } else {
      json[r'sabhaKindId'] = null;
    }
    if (this.sahSanchalak != null) {
      json[r'sahSanchalak'] = this.sahSanchalak;
    } else {
      json[r'sahSanchalak'] = null;
    }
    if (this.sanchalak != null) {
      json[r'sanchalak'] = this.sanchalak;
    } else {
      json[r'sanchalak'] = null;
    }
    if (this.standingVenue != null) {
      json[r'standingVenue'] = this.standingVenue;
    } else {
      json[r'standingVenue'] = null;
    }
    if (this.startTime != null) {
      json[r'startTime'] = this.startTime;
    } else {
      json[r'startTime'] = null;
    }
    if (this.weekly != null) {
      json[r'weekly'] = this.weekly;
    } else {
      json[r'weekly'] = null;
    }
    return json;
  }

  /// Returns a new [DefineSabhaRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static DefineSabhaRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return DefineSabhaRequest(
        dayOfWeek: DefineSabhaRequestDayOfWeekEnum.fromJson(json[r'dayOfWeek']),
        endTime: mapValueOfType<String>(json, r'endTime'),
        kshetraId: mapValueOfType<String>(json, r'kshetraId'),
        sabhaKindId: mapValueOfType<String>(json, r'sabhaKindId'),
        sahSanchalak: AppointeePayload.fromJson(json[r'sahSanchalak']),
        sanchalak: AppointeePayload.fromJson(json[r'sanchalak']),
        standingVenue: mapValueOfType<String>(json, r'standingVenue'),
        startTime: mapValueOfType<String>(json, r'startTime'),
        weekly: mapValueOfType<bool>(json, r'weekly'),
      );
    }
    return null;
  }

  static List<DefineSabhaRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <DefineSabhaRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = DefineSabhaRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, DefineSabhaRequest> mapFromJson(dynamic json) {
    final map = <String, DefineSabhaRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = DefineSabhaRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of DefineSabhaRequest-objects as value to a dart map
  static Map<String, List<DefineSabhaRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<DefineSabhaRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = DefineSabhaRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class DefineSabhaRequestDayOfWeekEnum {
  /// Instantiate a new enum with the provided [value].
  const DefineSabhaRequestDayOfWeekEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const MONDAY = DefineSabhaRequestDayOfWeekEnum._(r'MONDAY');
  static const TUESDAY = DefineSabhaRequestDayOfWeekEnum._(r'TUESDAY');
  static const WEDNESDAY = DefineSabhaRequestDayOfWeekEnum._(r'WEDNESDAY');
  static const THURSDAY = DefineSabhaRequestDayOfWeekEnum._(r'THURSDAY');
  static const FRIDAY = DefineSabhaRequestDayOfWeekEnum._(r'FRIDAY');
  static const SATURDAY = DefineSabhaRequestDayOfWeekEnum._(r'SATURDAY');
  static const SUNDAY = DefineSabhaRequestDayOfWeekEnum._(r'SUNDAY');

  /// List of all possible values in this [enum][DefineSabhaRequestDayOfWeekEnum].
  static const values = <DefineSabhaRequestDayOfWeekEnum>[
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
  ];

  static DefineSabhaRequestDayOfWeekEnum? fromJson(dynamic value) => DefineSabhaRequestDayOfWeekEnumTypeTransformer().decode(value);

  static List<DefineSabhaRequestDayOfWeekEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <DefineSabhaRequestDayOfWeekEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = DefineSabhaRequestDayOfWeekEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [DefineSabhaRequestDayOfWeekEnum] to String,
/// and [decode] dynamic data back to [DefineSabhaRequestDayOfWeekEnum].
class DefineSabhaRequestDayOfWeekEnumTypeTransformer {
  factory DefineSabhaRequestDayOfWeekEnumTypeTransformer() => _instance ??= const DefineSabhaRequestDayOfWeekEnumTypeTransformer._();

  const DefineSabhaRequestDayOfWeekEnumTypeTransformer._();

  String encode(DefineSabhaRequestDayOfWeekEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a DefineSabhaRequestDayOfWeekEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  DefineSabhaRequestDayOfWeekEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'MONDAY': return DefineSabhaRequestDayOfWeekEnum.MONDAY;
        case r'TUESDAY': return DefineSabhaRequestDayOfWeekEnum.TUESDAY;
        case r'WEDNESDAY': return DefineSabhaRequestDayOfWeekEnum.WEDNESDAY;
        case r'THURSDAY': return DefineSabhaRequestDayOfWeekEnum.THURSDAY;
        case r'FRIDAY': return DefineSabhaRequestDayOfWeekEnum.FRIDAY;
        case r'SATURDAY': return DefineSabhaRequestDayOfWeekEnum.SATURDAY;
        case r'SUNDAY': return DefineSabhaRequestDayOfWeekEnum.SUNDAY;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [DefineSabhaRequestDayOfWeekEnumTypeTransformer] instance.
  static DefineSabhaRequestDayOfWeekEnumTypeTransformer? _instance;
}


