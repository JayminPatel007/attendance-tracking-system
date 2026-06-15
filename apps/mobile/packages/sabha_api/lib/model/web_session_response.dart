//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class WebSessionResponse {
  /// Returns a new [WebSessionResponse] instance.
  WebSessionResponse({
    this.madhyasthaKaryalaya,
    this.sections = const {},
    this.username,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? madhyasthaKaryalaya;

  Set<WebSessionResponseSectionsEnum> sections;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? username;

  @override
  bool operator ==(Object other) => identical(this, other) || other is WebSessionResponse &&
    other.madhyasthaKaryalaya == madhyasthaKaryalaya &&
    _deepEquality.equals(other.sections, sections) &&
    other.username == username;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (madhyasthaKaryalaya == null ? 0 : madhyasthaKaryalaya!.hashCode) +
    (sections.hashCode) +
    (username == null ? 0 : username!.hashCode);

  @override
  String toString() => 'WebSessionResponse[madhyasthaKaryalaya=$madhyasthaKaryalaya, sections=$sections, username=$username]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.madhyasthaKaryalaya != null) {
      json[r'madhyasthaKaryalaya'] = this.madhyasthaKaryalaya;
    } else {
      json[r'madhyasthaKaryalaya'] = null;
    }
      json[r'sections'] = this.sections.toList(growable: false);
    if (this.username != null) {
      json[r'username'] = this.username;
    } else {
      json[r'username'] = null;
    }
    return json;
  }

  /// Returns a new [WebSessionResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static WebSessionResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return WebSessionResponse(
        madhyasthaKaryalaya: mapValueOfType<bool>(json, r'madhyasthaKaryalaya'),
        sections: WebSessionResponseSectionsEnum.listFromJson(json[r'sections']).toSet(),
        username: mapValueOfType<String>(json, r'username'),
      );
    }
    return null;
  }

  static List<WebSessionResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <WebSessionResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = WebSessionResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, WebSessionResponse> mapFromJson(dynamic json) {
    final map = <String, WebSessionResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = WebSessionResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of WebSessionResponse-objects as value to a dart map
  static Map<String, List<WebSessionResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<WebSessionResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = WebSessionResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class WebSessionResponseSectionsEnum {
  /// Instantiate a new enum with the provided [value].
  const WebSessionResponseSectionsEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const DASHBOARD = WebSessionResponseSectionsEnum._(r'DASHBOARD');
  static const ROLE_APPOINTMENT = WebSessionResponseSectionsEnum._(r'ROLE_APPOINTMENT');
  static const STRUCTURAL_ADMIN = WebSessionResponseSectionsEnum._(r'STRUCTURAL_ADMIN');
  static const SABHA_DEFINITION = WebSessionResponseSectionsEnum._(r'SABHA_DEFINITION');
  static const OCCURRENCE_REOPEN = WebSessionResponseSectionsEnum._(r'OCCURRENCE_REOPEN');
  static const SANCHALAK_PROXY = WebSessionResponseSectionsEnum._(r'SANCHALAK_PROXY');
  static const SELECTION = WebSessionResponseSectionsEnum._(r'SELECTION');
  static const AUDIT_LOG = WebSessionResponseSectionsEnum._(r'AUDIT_LOG');

  /// List of all possible values in this [enum][WebSessionResponseSectionsEnum].
  static const values = <WebSessionResponseSectionsEnum>[
    DASHBOARD,
    ROLE_APPOINTMENT,
    STRUCTURAL_ADMIN,
    SABHA_DEFINITION,
    OCCURRENCE_REOPEN,
    SANCHALAK_PROXY,
    SELECTION,
    AUDIT_LOG,
  ];

  static WebSessionResponseSectionsEnum? fromJson(dynamic value) => WebSessionResponseSectionsEnumTypeTransformer().decode(value);

  static List<WebSessionResponseSectionsEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <WebSessionResponseSectionsEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = WebSessionResponseSectionsEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [WebSessionResponseSectionsEnum] to String,
/// and [decode] dynamic data back to [WebSessionResponseSectionsEnum].
class WebSessionResponseSectionsEnumTypeTransformer {
  factory WebSessionResponseSectionsEnumTypeTransformer() => _instance ??= const WebSessionResponseSectionsEnumTypeTransformer._();

  const WebSessionResponseSectionsEnumTypeTransformer._();

  String encode(WebSessionResponseSectionsEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a WebSessionResponseSectionsEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  WebSessionResponseSectionsEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'DASHBOARD': return WebSessionResponseSectionsEnum.DASHBOARD;
        case r'ROLE_APPOINTMENT': return WebSessionResponseSectionsEnum.ROLE_APPOINTMENT;
        case r'STRUCTURAL_ADMIN': return WebSessionResponseSectionsEnum.STRUCTURAL_ADMIN;
        case r'SABHA_DEFINITION': return WebSessionResponseSectionsEnum.SABHA_DEFINITION;
        case r'OCCURRENCE_REOPEN': return WebSessionResponseSectionsEnum.OCCURRENCE_REOPEN;
        case r'SANCHALAK_PROXY': return WebSessionResponseSectionsEnum.SANCHALAK_PROXY;
        case r'SELECTION': return WebSessionResponseSectionsEnum.SELECTION;
        case r'AUDIT_LOG': return WebSessionResponseSectionsEnum.AUDIT_LOG;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [WebSessionResponseSectionsEnumTypeTransformer] instance.
  static WebSessionResponseSectionsEnumTypeTransformer? _instance;
}


