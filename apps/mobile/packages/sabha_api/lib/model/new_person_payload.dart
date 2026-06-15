//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class NewPersonPayload {
  /// Returns a new [NewPersonPayload] instance.
  NewPersonPayload({
    this.dateOfBirth,
    this.fullName,
    this.gender,
    this.guardianPersonId,
    this.homeSabhaId,
    this.mobile,
    this.overrideDuplicateWarning,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? dateOfBirth;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? fullName;

  NewPersonPayloadGenderEnum? gender;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? guardianPersonId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? homeSabhaId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? mobile;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? overrideDuplicateWarning;

  @override
  bool operator ==(Object other) => identical(this, other) || other is NewPersonPayload &&
    other.dateOfBirth == dateOfBirth &&
    other.fullName == fullName &&
    other.gender == gender &&
    other.guardianPersonId == guardianPersonId &&
    other.homeSabhaId == homeSabhaId &&
    other.mobile == mobile &&
    other.overrideDuplicateWarning == overrideDuplicateWarning;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (dateOfBirth == null ? 0 : dateOfBirth!.hashCode) +
    (fullName == null ? 0 : fullName!.hashCode) +
    (gender == null ? 0 : gender!.hashCode) +
    (guardianPersonId == null ? 0 : guardianPersonId!.hashCode) +
    (homeSabhaId == null ? 0 : homeSabhaId!.hashCode) +
    (mobile == null ? 0 : mobile!.hashCode) +
    (overrideDuplicateWarning == null ? 0 : overrideDuplicateWarning!.hashCode);

  @override
  String toString() => 'NewPersonPayload[dateOfBirth=$dateOfBirth, fullName=$fullName, gender=$gender, guardianPersonId=$guardianPersonId, homeSabhaId=$homeSabhaId, mobile=$mobile, overrideDuplicateWarning=$overrideDuplicateWarning]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.dateOfBirth != null) {
      json[r'dateOfBirth'] = _dateFormatter.format(this.dateOfBirth!.toUtc());
    } else {
      json[r'dateOfBirth'] = null;
    }
    if (this.fullName != null) {
      json[r'fullName'] = this.fullName;
    } else {
      json[r'fullName'] = null;
    }
    if (this.gender != null) {
      json[r'gender'] = this.gender;
    } else {
      json[r'gender'] = null;
    }
    if (this.guardianPersonId != null) {
      json[r'guardianPersonId'] = this.guardianPersonId;
    } else {
      json[r'guardianPersonId'] = null;
    }
    if (this.homeSabhaId != null) {
      json[r'homeSabhaId'] = this.homeSabhaId;
    } else {
      json[r'homeSabhaId'] = null;
    }
    if (this.mobile != null) {
      json[r'mobile'] = this.mobile;
    } else {
      json[r'mobile'] = null;
    }
    if (this.overrideDuplicateWarning != null) {
      json[r'overrideDuplicateWarning'] = this.overrideDuplicateWarning;
    } else {
      json[r'overrideDuplicateWarning'] = null;
    }
    return json;
  }

  /// Returns a new [NewPersonPayload] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static NewPersonPayload? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return NewPersonPayload(
        dateOfBirth: mapDateTime(json, r'dateOfBirth', r''),
        fullName: mapValueOfType<String>(json, r'fullName'),
        gender: NewPersonPayloadGenderEnum.fromJson(json[r'gender']),
        guardianPersonId: mapValueOfType<String>(json, r'guardianPersonId'),
        homeSabhaId: mapValueOfType<String>(json, r'homeSabhaId'),
        mobile: mapValueOfType<String>(json, r'mobile'),
        overrideDuplicateWarning: mapValueOfType<bool>(json, r'overrideDuplicateWarning'),
      );
    }
    return null;
  }

  static List<NewPersonPayload> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <NewPersonPayload>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = NewPersonPayload.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, NewPersonPayload> mapFromJson(dynamic json) {
    final map = <String, NewPersonPayload>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = NewPersonPayload.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of NewPersonPayload-objects as value to a dart map
  static Map<String, List<NewPersonPayload>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<NewPersonPayload>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = NewPersonPayload.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class NewPersonPayloadGenderEnum {
  /// Instantiate a new enum with the provided [value].
  const NewPersonPayloadGenderEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const MALE = NewPersonPayloadGenderEnum._(r'MALE');
  static const FEMALE = NewPersonPayloadGenderEnum._(r'FEMALE');

  /// List of all possible values in this [enum][NewPersonPayloadGenderEnum].
  static const values = <NewPersonPayloadGenderEnum>[
    MALE,
    FEMALE,
  ];

  static NewPersonPayloadGenderEnum? fromJson(dynamic value) => NewPersonPayloadGenderEnumTypeTransformer().decode(value);

  static List<NewPersonPayloadGenderEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <NewPersonPayloadGenderEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = NewPersonPayloadGenderEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [NewPersonPayloadGenderEnum] to String,
/// and [decode] dynamic data back to [NewPersonPayloadGenderEnum].
class NewPersonPayloadGenderEnumTypeTransformer {
  factory NewPersonPayloadGenderEnumTypeTransformer() => _instance ??= const NewPersonPayloadGenderEnumTypeTransformer._();

  const NewPersonPayloadGenderEnumTypeTransformer._();

  String encode(NewPersonPayloadGenderEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a NewPersonPayloadGenderEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  NewPersonPayloadGenderEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'MALE': return NewPersonPayloadGenderEnum.MALE;
        case r'FEMALE': return NewPersonPayloadGenderEnum.FEMALE;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [NewPersonPayloadGenderEnumTypeTransformer] instance.
  static NewPersonPayloadGenderEnumTypeTransformer? _instance;
}


