//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AuditEntry {
  /// Returns a new [AuditEntry] instance.
  AuditEntry({
    required this.action,
    required this.actorName,
    required this.actorUserId,
    required this.at,
    required this.detail,
    required this.id,
    required this.onBehalfName,
    required this.onBehalfOfUserId,
    required this.targetId,
    required this.targetType,
  });

  String action;

  String? actorName;

  String? actorUserId;

  DateTime at;

  String? detail;

  String id;

  String? onBehalfName;

  String? onBehalfOfUserId;

  String targetId;

  AuditEntryTargetTypeEnum targetType;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AuditEntry &&
    other.action == action &&
    other.actorName == actorName &&
    other.actorUserId == actorUserId &&
    other.at == at &&
    other.detail == detail &&
    other.id == id &&
    other.onBehalfName == onBehalfName &&
    other.onBehalfOfUserId == onBehalfOfUserId &&
    other.targetId == targetId &&
    other.targetType == targetType;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (action.hashCode) +
    (actorName == null ? 0 : actorName!.hashCode) +
    (actorUserId == null ? 0 : actorUserId!.hashCode) +
    (at.hashCode) +
    (detail == null ? 0 : detail!.hashCode) +
    (id.hashCode) +
    (onBehalfName == null ? 0 : onBehalfName!.hashCode) +
    (onBehalfOfUserId == null ? 0 : onBehalfOfUserId!.hashCode) +
    (targetId.hashCode) +
    (targetType.hashCode);

  @override
  String toString() => 'AuditEntry[action=$action, actorName=$actorName, actorUserId=$actorUserId, at=$at, detail=$detail, id=$id, onBehalfName=$onBehalfName, onBehalfOfUserId=$onBehalfOfUserId, targetId=$targetId, targetType=$targetType]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'action'] = this.action;
    if (this.actorName != null) {
      json[r'actorName'] = this.actorName;
    } else {
      json[r'actorName'] = null;
    }
    if (this.actorUserId != null) {
      json[r'actorUserId'] = this.actorUserId;
    } else {
      json[r'actorUserId'] = null;
    }
      json[r'at'] = this.at.toUtc().toIso8601String();
    if (this.detail != null) {
      json[r'detail'] = this.detail;
    } else {
      json[r'detail'] = null;
    }
      json[r'id'] = this.id;
    if (this.onBehalfName != null) {
      json[r'onBehalfName'] = this.onBehalfName;
    } else {
      json[r'onBehalfName'] = null;
    }
    if (this.onBehalfOfUserId != null) {
      json[r'onBehalfOfUserId'] = this.onBehalfOfUserId;
    } else {
      json[r'onBehalfOfUserId'] = null;
    }
      json[r'targetId'] = this.targetId;
      json[r'targetType'] = this.targetType;
    return json;
  }

  /// Returns a new [AuditEntry] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AuditEntry? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'action'), 'Required key "AuditEntry[action]" is missing from JSON.');
        assert(json[r'action'] != null, 'Required key "AuditEntry[action]" has a null value in JSON.');
        assert(json.containsKey(r'actorName'), 'Required key "AuditEntry[actorName]" is missing from JSON.');
        assert(json.containsKey(r'actorUserId'), 'Required key "AuditEntry[actorUserId]" is missing from JSON.');
        assert(json.containsKey(r'at'), 'Required key "AuditEntry[at]" is missing from JSON.');
        assert(json[r'at'] != null, 'Required key "AuditEntry[at]" has a null value in JSON.');
        assert(json.containsKey(r'detail'), 'Required key "AuditEntry[detail]" is missing from JSON.');
        assert(json.containsKey(r'id'), 'Required key "AuditEntry[id]" is missing from JSON.');
        assert(json[r'id'] != null, 'Required key "AuditEntry[id]" has a null value in JSON.');
        assert(json.containsKey(r'onBehalfName'), 'Required key "AuditEntry[onBehalfName]" is missing from JSON.');
        assert(json.containsKey(r'onBehalfOfUserId'), 'Required key "AuditEntry[onBehalfOfUserId]" is missing from JSON.');
        assert(json.containsKey(r'targetId'), 'Required key "AuditEntry[targetId]" is missing from JSON.');
        assert(json[r'targetId'] != null, 'Required key "AuditEntry[targetId]" has a null value in JSON.');
        assert(json.containsKey(r'targetType'), 'Required key "AuditEntry[targetType]" is missing from JSON.');
        assert(json[r'targetType'] != null, 'Required key "AuditEntry[targetType]" has a null value in JSON.');
        return true;
      }());

      return AuditEntry(
        action: mapValueOfType<String>(json, r'action')!,
        actorName: mapValueOfType<String>(json, r'actorName'),
        actorUserId: mapValueOfType<String>(json, r'actorUserId'),
        at: mapDateTime(json, r'at', r'')!,
        detail: mapValueOfType<String>(json, r'detail'),
        id: mapValueOfType<String>(json, r'id')!,
        onBehalfName: mapValueOfType<String>(json, r'onBehalfName'),
        onBehalfOfUserId: mapValueOfType<String>(json, r'onBehalfOfUserId'),
        targetId: mapValueOfType<String>(json, r'targetId')!,
        targetType: AuditEntryTargetTypeEnum.fromJson(json[r'targetType'])!,
      );
    }
    return null;
  }

  static List<AuditEntry> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AuditEntry>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AuditEntry.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AuditEntry> mapFromJson(dynamic json) {
    final map = <String, AuditEntry>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AuditEntry.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AuditEntry-objects as value to a dart map
  static Map<String, List<AuditEntry>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AuditEntry>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AuditEntry.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'action',
    'actorName',
    'actorUserId',
    'at',
    'detail',
    'id',
    'onBehalfName',
    'onBehalfOfUserId',
    'targetId',
    'targetType',
  };
}


class AuditEntryTargetTypeEnum {
  /// Instantiate a new enum with the provided [value].
  const AuditEntryTargetTypeEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const OCCURRENCE = AuditEntryTargetTypeEnum._(r'OCCURRENCE');
  static const SABHA = AuditEntryTargetTypeEnum._(r'SABHA');
  static const ROLE_ASSIGNMENT = AuditEntryTargetTypeEnum._(r'ROLE_ASSIGNMENT');
  static const STRUCTURAL = AuditEntryTargetTypeEnum._(r'STRUCTURAL');
  static const PERSON = AuditEntryTargetTypeEnum._(r'PERSON');

  /// List of all possible values in this [enum][AuditEntryTargetTypeEnum].
  static const values = <AuditEntryTargetTypeEnum>[
    OCCURRENCE,
    SABHA,
    ROLE_ASSIGNMENT,
    STRUCTURAL,
    PERSON,
  ];

  static AuditEntryTargetTypeEnum? fromJson(dynamic value) => AuditEntryTargetTypeEnumTypeTransformer().decode(value);

  static List<AuditEntryTargetTypeEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AuditEntryTargetTypeEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AuditEntryTargetTypeEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [AuditEntryTargetTypeEnum] to String,
/// and [decode] dynamic data back to [AuditEntryTargetTypeEnum].
class AuditEntryTargetTypeEnumTypeTransformer {
  factory AuditEntryTargetTypeEnumTypeTransformer() => _instance ??= const AuditEntryTargetTypeEnumTypeTransformer._();

  const AuditEntryTargetTypeEnumTypeTransformer._();

  String encode(AuditEntryTargetTypeEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a AuditEntryTargetTypeEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  AuditEntryTargetTypeEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'OCCURRENCE': return AuditEntryTargetTypeEnum.OCCURRENCE;
        case r'SABHA': return AuditEntryTargetTypeEnum.SABHA;
        case r'ROLE_ASSIGNMENT': return AuditEntryTargetTypeEnum.ROLE_ASSIGNMENT;
        case r'STRUCTURAL': return AuditEntryTargetTypeEnum.STRUCTURAL;
        case r'PERSON': return AuditEntryTargetTypeEnum.PERSON;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [AuditEntryTargetTypeEnumTypeTransformer] instance.
  static AuditEntryTargetTypeEnumTypeTransformer? _instance;
}


