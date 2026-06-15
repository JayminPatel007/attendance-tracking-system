//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class CurrentOccurrence {
  /// Returns a new [CurrentOccurrence] instance.
  CurrentOccurrence({
    this.date,
    this.id,
    this.rescheduledDate,
    this.rescheduledEndTime,
    this.rescheduledStartTime,
    this.sabhaId,
    this.state,
    this.venueOverride,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? date;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? id;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? rescheduledDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? rescheduledEndTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? rescheduledStartTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaId;

  CurrentOccurrenceStateEnum? state;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? venueOverride;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CurrentOccurrence &&
    other.date == date &&
    other.id == id &&
    other.rescheduledDate == rescheduledDate &&
    other.rescheduledEndTime == rescheduledEndTime &&
    other.rescheduledStartTime == rescheduledStartTime &&
    other.sabhaId == sabhaId &&
    other.state == state &&
    other.venueOverride == venueOverride;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (date == null ? 0 : date!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (rescheduledDate == null ? 0 : rescheduledDate!.hashCode) +
    (rescheduledEndTime == null ? 0 : rescheduledEndTime!.hashCode) +
    (rescheduledStartTime == null ? 0 : rescheduledStartTime!.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (state == null ? 0 : state!.hashCode) +
    (venueOverride == null ? 0 : venueOverride!.hashCode);

  @override
  String toString() => 'CurrentOccurrence[date=$date, id=$id, rescheduledDate=$rescheduledDate, rescheduledEndTime=$rescheduledEndTime, rescheduledStartTime=$rescheduledStartTime, sabhaId=$sabhaId, state=$state, venueOverride=$venueOverride]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.date != null) {
      json[r'date'] = _dateFormatter.format(this.date!.toUtc());
    } else {
      json[r'date'] = null;
    }
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
    if (this.rescheduledDate != null) {
      json[r'rescheduledDate'] = _dateFormatter.format(this.rescheduledDate!.toUtc());
    } else {
      json[r'rescheduledDate'] = null;
    }
    if (this.rescheduledEndTime != null) {
      json[r'rescheduledEndTime'] = this.rescheduledEndTime;
    } else {
      json[r'rescheduledEndTime'] = null;
    }
    if (this.rescheduledStartTime != null) {
      json[r'rescheduledStartTime'] = this.rescheduledStartTime;
    } else {
      json[r'rescheduledStartTime'] = null;
    }
    if (this.sabhaId != null) {
      json[r'sabhaId'] = this.sabhaId;
    } else {
      json[r'sabhaId'] = null;
    }
    if (this.state != null) {
      json[r'state'] = this.state;
    } else {
      json[r'state'] = null;
    }
    if (this.venueOverride != null) {
      json[r'venueOverride'] = this.venueOverride;
    } else {
      json[r'venueOverride'] = null;
    }
    return json;
  }

  /// Returns a new [CurrentOccurrence] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static CurrentOccurrence? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return CurrentOccurrence(
        date: mapDateTime(json, r'date', r''),
        id: mapValueOfType<String>(json, r'id'),
        rescheduledDate: mapDateTime(json, r'rescheduledDate', r''),
        rescheduledEndTime: mapValueOfType<String>(json, r'rescheduledEndTime'),
        rescheduledStartTime: mapValueOfType<String>(json, r'rescheduledStartTime'),
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        state: CurrentOccurrenceStateEnum.fromJson(json[r'state']),
        venueOverride: mapValueOfType<String>(json, r'venueOverride'),
      );
    }
    return null;
  }

  static List<CurrentOccurrence> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CurrentOccurrence>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CurrentOccurrence.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CurrentOccurrence> mapFromJson(dynamic json) {
    final map = <String, CurrentOccurrence>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = CurrentOccurrence.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CurrentOccurrence-objects as value to a dart map
  static Map<String, List<CurrentOccurrence>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CurrentOccurrence>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CurrentOccurrence.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class CurrentOccurrenceStateEnum {
  /// Instantiate a new enum with the provided [value].
  const CurrentOccurrenceStateEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const SCHEDULED = CurrentOccurrenceStateEnum._(r'SCHEDULED');
  static const RESCHEDULED = CurrentOccurrenceStateEnum._(r'RESCHEDULED');
  static const OPEN_FOR_MARKING = CurrentOccurrenceStateEnum._(r'OPEN_FOR_MARKING');
  static const FINALIZED = CurrentOccurrenceStateEnum._(r'FINALIZED');
  static const CANCELLED = CurrentOccurrenceStateEnum._(r'CANCELLED');

  /// List of all possible values in this [enum][CurrentOccurrenceStateEnum].
  static const values = <CurrentOccurrenceStateEnum>[
    SCHEDULED,
    RESCHEDULED,
    OPEN_FOR_MARKING,
    FINALIZED,
    CANCELLED,
  ];

  static CurrentOccurrenceStateEnum? fromJson(dynamic value) => CurrentOccurrenceStateEnumTypeTransformer().decode(value);

  static List<CurrentOccurrenceStateEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CurrentOccurrenceStateEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CurrentOccurrenceStateEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [CurrentOccurrenceStateEnum] to String,
/// and [decode] dynamic data back to [CurrentOccurrenceStateEnum].
class CurrentOccurrenceStateEnumTypeTransformer {
  factory CurrentOccurrenceStateEnumTypeTransformer() => _instance ??= const CurrentOccurrenceStateEnumTypeTransformer._();

  const CurrentOccurrenceStateEnumTypeTransformer._();

  String encode(CurrentOccurrenceStateEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a CurrentOccurrenceStateEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  CurrentOccurrenceStateEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'SCHEDULED': return CurrentOccurrenceStateEnum.SCHEDULED;
        case r'RESCHEDULED': return CurrentOccurrenceStateEnum.RESCHEDULED;
        case r'OPEN_FOR_MARKING': return CurrentOccurrenceStateEnum.OPEN_FOR_MARKING;
        case r'FINALIZED': return CurrentOccurrenceStateEnum.FINALIZED;
        case r'CANCELLED': return CurrentOccurrenceStateEnum.CANCELLED;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [CurrentOccurrenceStateEnumTypeTransformer] instance.
  static CurrentOccurrenceStateEnumTypeTransformer? _instance;
}


