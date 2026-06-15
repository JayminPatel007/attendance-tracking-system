//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PersonResponse {
  /// Returns a new [PersonResponse] instance.
  PersonResponse({
    this.dateOfBirth,
    this.fullName,
    this.gender,
    this.guardianPersonId,
    this.id,
    this.mobile,
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

  PersonResponseGenderEnum? gender;

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
  String? id;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? mobile;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PersonResponse &&
    other.dateOfBirth == dateOfBirth &&
    other.fullName == fullName &&
    other.gender == gender &&
    other.guardianPersonId == guardianPersonId &&
    other.id == id &&
    other.mobile == mobile;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (dateOfBirth == null ? 0 : dateOfBirth!.hashCode) +
    (fullName == null ? 0 : fullName!.hashCode) +
    (gender == null ? 0 : gender!.hashCode) +
    (guardianPersonId == null ? 0 : guardianPersonId!.hashCode) +
    (id == null ? 0 : id!.hashCode) +
    (mobile == null ? 0 : mobile!.hashCode);

  @override
  String toString() => 'PersonResponse[dateOfBirth=$dateOfBirth, fullName=$fullName, gender=$gender, guardianPersonId=$guardianPersonId, id=$id, mobile=$mobile]';

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
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
    if (this.mobile != null) {
      json[r'mobile'] = this.mobile;
    } else {
      json[r'mobile'] = null;
    }
    return json;
  }

  /// Returns a new [PersonResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PersonResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return PersonResponse(
        dateOfBirth: mapDateTime(json, r'dateOfBirth', r''),
        fullName: mapValueOfType<String>(json, r'fullName'),
        gender: PersonResponseGenderEnum.fromJson(json[r'gender']),
        guardianPersonId: mapValueOfType<String>(json, r'guardianPersonId'),
        id: mapValueOfType<String>(json, r'id'),
        mobile: mapValueOfType<String>(json, r'mobile'),
      );
    }
    return null;
  }

  static List<PersonResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PersonResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PersonResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PersonResponse> mapFromJson(dynamic json) {
    final map = <String, PersonResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PersonResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PersonResponse-objects as value to a dart map
  static Map<String, List<PersonResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PersonResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PersonResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class PersonResponseGenderEnum {
  /// Instantiate a new enum with the provided [value].
  const PersonResponseGenderEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const MALE = PersonResponseGenderEnum._(r'MALE');
  static const FEMALE = PersonResponseGenderEnum._(r'FEMALE');

  /// List of all possible values in this [enum][PersonResponseGenderEnum].
  static const values = <PersonResponseGenderEnum>[
    MALE,
    FEMALE,
  ];

  static PersonResponseGenderEnum? fromJson(dynamic value) => PersonResponseGenderEnumTypeTransformer().decode(value);

  static List<PersonResponseGenderEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PersonResponseGenderEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PersonResponseGenderEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [PersonResponseGenderEnum] to String,
/// and [decode] dynamic data back to [PersonResponseGenderEnum].
class PersonResponseGenderEnumTypeTransformer {
  factory PersonResponseGenderEnumTypeTransformer() => _instance ??= const PersonResponseGenderEnumTypeTransformer._();

  const PersonResponseGenderEnumTypeTransformer._();

  String encode(PersonResponseGenderEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a PersonResponseGenderEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  PersonResponseGenderEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'MALE': return PersonResponseGenderEnum.MALE;
        case r'FEMALE': return PersonResponseGenderEnum.FEMALE;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [PersonResponseGenderEnumTypeTransformer] instance.
  static PersonResponseGenderEnumTypeTransformer? _instance;
}


