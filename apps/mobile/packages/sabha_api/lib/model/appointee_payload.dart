//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AppointeePayload {
  /// Returns a new [AppointeePayload] instance.
  AppointeePayload({
    this.existingPersonId,
    this.newPerson,
    this.rawPassword,
    this.username,
  });

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
  NewPersonPayload? newPerson;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? rawPassword;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? username;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AppointeePayload &&
    other.existingPersonId == existingPersonId &&
    other.newPerson == newPerson &&
    other.rawPassword == rawPassword &&
    other.username == username;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (existingPersonId == null ? 0 : existingPersonId!.hashCode) +
    (newPerson == null ? 0 : newPerson!.hashCode) +
    (rawPassword == null ? 0 : rawPassword!.hashCode) +
    (username == null ? 0 : username!.hashCode);

  @override
  String toString() => 'AppointeePayload[existingPersonId=$existingPersonId, newPerson=$newPerson, rawPassword=$rawPassword, username=$username]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.existingPersonId != null) {
      json[r'existingPersonId'] = this.existingPersonId;
    } else {
      json[r'existingPersonId'] = null;
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
    if (this.username != null) {
      json[r'username'] = this.username;
    } else {
      json[r'username'] = null;
    }
    return json;
  }

  /// Returns a new [AppointeePayload] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AppointeePayload? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return AppointeePayload(
        existingPersonId: mapValueOfType<String>(json, r'existingPersonId'),
        newPerson: NewPersonPayload.fromJson(json[r'newPerson']),
        rawPassword: mapValueOfType<String>(json, r'rawPassword'),
        username: mapValueOfType<String>(json, r'username'),
      );
    }
    return null;
  }

  static List<AppointeePayload> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AppointeePayload>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AppointeePayload.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AppointeePayload> mapFromJson(dynamic json) {
    final map = <String, AppointeePayload>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AppointeePayload.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AppointeePayload-objects as value to a dart map
  static Map<String, List<AppointeePayload>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AppointeePayload>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AppointeePayload.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

