//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AppointmentRequest {
  /// Returns a new [AppointmentRequest] instance.
  AppointmentRequest({
    this.cityId,
    this.demographic,
    this.existingPersonId,
    this.kshetraId,
    this.newPerson,
    this.rawPassword,
    this.role,
    this.sabhaId,
    this.username,
    this.zoneId,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? cityId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? demographic;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? existingPersonId;

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
  NewPersonPayload? newPerson;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? rawPassword;

  AppointmentRequestRoleEnum? role;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? username;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? zoneId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AppointmentRequest &&
    other.cityId == cityId &&
    other.demographic == demographic &&
    other.existingPersonId == existingPersonId &&
    other.kshetraId == kshetraId &&
    other.newPerson == newPerson &&
    other.rawPassword == rawPassword &&
    other.role == role &&
    other.sabhaId == sabhaId &&
    other.username == username &&
    other.zoneId == zoneId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (cityId == null ? 0 : cityId!.hashCode) +
    (demographic == null ? 0 : demographic!.hashCode) +
    (existingPersonId == null ? 0 : existingPersonId!.hashCode) +
    (kshetraId == null ? 0 : kshetraId!.hashCode) +
    (newPerson == null ? 0 : newPerson!.hashCode) +
    (rawPassword == null ? 0 : rawPassword!.hashCode) +
    (role == null ? 0 : role!.hashCode) +
    (sabhaId == null ? 0 : sabhaId!.hashCode) +
    (username == null ? 0 : username!.hashCode) +
    (zoneId == null ? 0 : zoneId!.hashCode);

  @override
  String toString() => 'AppointmentRequest[cityId=$cityId, demographic=$demographic, existingPersonId=$existingPersonId, kshetraId=$kshetraId, newPerson=$newPerson, rawPassword=$rawPassword, role=$role, sabhaId=$sabhaId, username=$username, zoneId=$zoneId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.cityId != null) {
      json[r'cityId'] = this.cityId;
    } else {
      json[r'cityId'] = null;
    }
    if (this.demographic != null) {
      json[r'demographic'] = this.demographic;
    } else {
      json[r'demographic'] = null;
    }
    if (this.existingPersonId != null) {
      json[r'existingPersonId'] = this.existingPersonId;
    } else {
      json[r'existingPersonId'] = null;
    }
    if (this.kshetraId != null) {
      json[r'kshetraId'] = this.kshetraId;
    } else {
      json[r'kshetraId'] = null;
    }
    if (this.newPerson != null) {
      json[r'newPerson'] = this.newPerson;
    } else {
      json[r'newPerson'] = null;
    }
    if (this.rawPassword != null) {
      json[r'rawPassword'] = this.rawPassword;
    } else {
      json[r'rawPassword'] = null;
    }
    if (this.role != null) {
      json[r'role'] = this.role;
    } else {
      json[r'role'] = null;
    }
    if (this.sabhaId != null) {
      json[r'sabhaId'] = this.sabhaId;
    } else {
      json[r'sabhaId'] = null;
    }
    if (this.username != null) {
      json[r'username'] = this.username;
    } else {
      json[r'username'] = null;
    }
    if (this.zoneId != null) {
      json[r'zoneId'] = this.zoneId;
    } else {
      json[r'zoneId'] = null;
    }
    return json;
  }

  /// Returns a new [AppointmentRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AppointmentRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return AppointmentRequest(
        cityId: mapValueOfType<String>(json, r'cityId'),
        demographic: mapValueOfType<String>(json, r'demographic'),
        existingPersonId: mapValueOfType<String>(json, r'existingPersonId'),
        kshetraId: mapValueOfType<String>(json, r'kshetraId'),
        newPerson: NewPersonPayload.fromJson(json[r'newPerson']),
        rawPassword: mapValueOfType<String>(json, r'rawPassword'),
        role: AppointmentRequestRoleEnum.fromJson(json[r'role']),
        sabhaId: mapValueOfType<String>(json, r'sabhaId'),
        username: mapValueOfType<String>(json, r'username'),
        zoneId: mapValueOfType<String>(json, r'zoneId'),
      );
    }
    return null;
  }

  static List<AppointmentRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AppointmentRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AppointmentRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AppointmentRequest> mapFromJson(dynamic json) {
    final map = <String, AppointmentRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AppointmentRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AppointmentRequest-objects as value to a dart map
  static Map<String, List<AppointmentRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AppointmentRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AppointmentRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class AppointmentRequestRoleEnum {
  /// Instantiate a new enum with the provided [value].
  const AppointmentRequestRoleEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const SANCHALAK = AppointmentRequestRoleEnum._(r'SANCHALAK');
  static const SAH_SANCHALAK = AppointmentRequestRoleEnum._(r'SAH_SANCHALAK');
  static const NIRIKSHAK = AppointmentRequestRoleEnum._(r'NIRIKSHAK');
  static const SAH_NIRDESHAK = AppointmentRequestRoleEnum._(r'SAH_NIRDESHAK');
  static const NIRDESHAK = AppointmentRequestRoleEnum._(r'NIRDESHAK');
  static const SANYOJAK = AppointmentRequestRoleEnum._(r'SANYOJAK');
  static const REGIONAL_TEAM = AppointmentRequestRoleEnum._(r'REGIONAL_TEAM');
  static const SANT = AppointmentRequestRoleEnum._(r'SANT');

  /// List of all possible values in this [enum][AppointmentRequestRoleEnum].
  static const values = <AppointmentRequestRoleEnum>[
    SANCHALAK,
    SAH_SANCHALAK,
    NIRIKSHAK,
    SAH_NIRDESHAK,
    NIRDESHAK,
    SANYOJAK,
    REGIONAL_TEAM,
    SANT,
  ];

  static AppointmentRequestRoleEnum? fromJson(dynamic value) => AppointmentRequestRoleEnumTypeTransformer().decode(value);

  static List<AppointmentRequestRoleEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AppointmentRequestRoleEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AppointmentRequestRoleEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [AppointmentRequestRoleEnum] to String,
/// and [decode] dynamic data back to [AppointmentRequestRoleEnum].
class AppointmentRequestRoleEnumTypeTransformer {
  factory AppointmentRequestRoleEnumTypeTransformer() => _instance ??= const AppointmentRequestRoleEnumTypeTransformer._();

  const AppointmentRequestRoleEnumTypeTransformer._();

  String encode(AppointmentRequestRoleEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a AppointmentRequestRoleEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  AppointmentRequestRoleEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'SANCHALAK': return AppointmentRequestRoleEnum.SANCHALAK;
        case r'SAH_SANCHALAK': return AppointmentRequestRoleEnum.SAH_SANCHALAK;
        case r'NIRIKSHAK': return AppointmentRequestRoleEnum.NIRIKSHAK;
        case r'SAH_NIRDESHAK': return AppointmentRequestRoleEnum.SAH_NIRDESHAK;
        case r'NIRDESHAK': return AppointmentRequestRoleEnum.NIRDESHAK;
        case r'SANYOJAK': return AppointmentRequestRoleEnum.SANYOJAK;
        case r'REGIONAL_TEAM': return AppointmentRequestRoleEnum.REGIONAL_TEAM;
        case r'SANT': return AppointmentRequestRoleEnum.SANT;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [AppointmentRequestRoleEnumTypeTransformer] instance.
  static AppointmentRequestRoleEnumTypeTransformer? _instance;
}


