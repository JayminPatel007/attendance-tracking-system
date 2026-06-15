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
    this.action,
    this.actorName,
    this.actorUserId,
    this.at,
    this.detail,
    this.id,
    this.onBehalfName,
    this.onBehalfOfUserId,
    this.targetId,
    this.targetType,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? action;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? actorName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? actorUserId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? at;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? detail;

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
  String? onBehalfName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? onBehalfOfUserId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? targetId;

  AuditEntryTargetTypeEnum? targetType;

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
    (action == null ? 0 : action!.hashCode) +
    (actorName == null ? 0 : actorName!.hashCode) +
    (actorUserId == null ? 0 : actorUserId!.hashCode) +
    (at == null ? 0 : at!.hashCode) +
    (detail == null ? 0 : detail!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (onBehalfName == null ? 0 : onBehalfName!.hashCode) +
    (onBehalfOfUserId == null ? 0 : onBehalfOfUserId!.hashCode) +
    (targetId == null ? 0 : targetId!.hashCode) +
    (targetType == null ? 0 : targetType!.hashCode);

  @override
  String toString() => 'AuditEntry[action=$action, actorName=$actorName, actorUserId=$actorUserId, at=$at, detail=$detail, id=$id, onBehalfName=$onBehalfName, onBehalfOfUserId=$onBehalfOfUserId, targetId=$targetId, targetType=$targetType]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.action != null) {
      json[r'action'] = this.action;
    } else {
      json[r'action'] = null;
    }
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
    if (this.at != null) {
      json[r'at'] = this.at!.toUtc().toIso8601String();
    } else {
      json[r'at'] = null;
    }
    if (this.detail != null) {
      json[r'detail'] = this.detail;
    } else {
      json[r'detail'] = null;
    }
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
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
    if (this.targetId != null) {
      json[r'targetId'] = this.targetId;
    } else {
      json[r'targetId'] = null;
    }
    if (this.targetType != null) {
      json[r'targetType'] = this.targetType;
    } else {
      json[r'targetType'] = null;
    }
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
        return true;
      }());

      return AuditEntry(
        action: mapValueOfType<String>(json, r'action'),
        actorName: mapValueOfType<String>(json, r'actorName'),
        actorUserId: mapValueOfType<String>(json, r'actorUserId'),
        at: mapDateTime(json, r'at', r''),
        detail: mapValueOfType<String>(json, r'detail'),
        id: mapValueOfType<String>(json, r'id'),
        onBehalfName: mapValueOfType<String>(json, r'onBehalfName'),
        onBehalfOfUserId: mapValueOfType<String>(json, r'onBehalfOfUserId'),
        targetId: mapValueOfType<String>(json, r'targetId'),
        targetType: AuditEntryTargetTypeEnum.fromJson(json[r'targetType']),
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


