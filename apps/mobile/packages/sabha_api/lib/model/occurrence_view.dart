//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class OccurrenceView {
  /// Returns a new [OccurrenceView] instance.
  OccurrenceView({
    this.date,
    this.id,
    this.sabhaId,
    this.state,
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
  String? sabhaId;

  OccurrenceViewStateEnum? state;

  @override
  bool operator ==(Object other) => identical(this, other) || other is OccurrenceView &&
    other.date == date &&
    other.id == id &&
    other.sabhaId == sabhaId &&
    other.state == state;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (date == null ? 0 : date!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (state == null ? 0 : state!.hashCode);

  @override
  String toString() => 'OccurrenceView[date=$date, id=$id, sabhaId=$sabhaId, state=$state]';

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
    return json;
  }

  /// Returns a new [OccurrenceView] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static OccurrenceView? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return OccurrenceView(
        date: mapDateTime(json, r'date', r''),
        id: mapValueOfType<String>(json, r'id'),
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        state: OccurrenceViewStateEnum.fromJson(json[r'state']),
      );
    }
    return null;
  }

  static List<OccurrenceView> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <OccurrenceView>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = OccurrenceView.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, OccurrenceView> mapFromJson(dynamic json) {
    final map = <String, OccurrenceView>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = OccurrenceView.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of OccurrenceView-objects as value to a dart map
  static Map<String, List<OccurrenceView>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<OccurrenceView>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = OccurrenceView.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class OccurrenceViewStateEnum {
  /// Instantiate a new enum with the provided [value].
  const OccurrenceViewStateEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const SCHEDULED = OccurrenceViewStateEnum._(r'SCHEDULED');
  static const RESCHEDULED = OccurrenceViewStateEnum._(r'RESCHEDULED');
  static const OPEN_FOR_MARKING = OccurrenceViewStateEnum._(r'OPEN_FOR_MARKING');
  static const FINALIZED = OccurrenceViewStateEnum._(r'FINALIZED');
  static const CANCELLED = OccurrenceViewStateEnum._(r'CANCELLED');

  /// List of all possible values in this [enum][OccurrenceViewStateEnum].
  static const values = <OccurrenceViewStateEnum>[
    SCHEDULED,
    RESCHEDULED,
    OPEN_FOR_MARKING,
    FINALIZED,
    CANCELLED,
  ];

  static OccurrenceViewStateEnum? fromJson(dynamic value) => OccurrenceViewStateEnumTypeTransformer().decode(value);

  static List<OccurrenceViewStateEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <OccurrenceViewStateEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = OccurrenceViewStateEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [OccurrenceViewStateEnum] to String,
/// and [decode] dynamic data back to [OccurrenceViewStateEnum].
class OccurrenceViewStateEnumTypeTransformer {
  factory OccurrenceViewStateEnumTypeTransformer() => _instance ??= const OccurrenceViewStateEnumTypeTransformer._();

  const OccurrenceViewStateEnumTypeTransformer._();

  String encode(OccurrenceViewStateEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a OccurrenceViewStateEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  OccurrenceViewStateEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'SCHEDULED': return OccurrenceViewStateEnum.SCHEDULED;
        case r'RESCHEDULED': return OccurrenceViewStateEnum.RESCHEDULED;
        case r'OPEN_FOR_MARKING': return OccurrenceViewStateEnum.OPEN_FOR_MARKING;
        case r'FINALIZED': return OccurrenceViewStateEnum.FINALIZED;
        case r'CANCELLED': return OccurrenceViewStateEnum.CANCELLED;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [OccurrenceViewStateEnumTypeTransformer] instance.
  static OccurrenceViewStateEnumTypeTransformer? _instance;
}


