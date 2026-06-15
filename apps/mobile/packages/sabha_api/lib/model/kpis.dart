//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class Kpis {
  /// Returns a new [Kpis] instance.
  Kpis({
    this.priorityCandidates,
    this.sabhasWithCandidates,
    this.totalCandidates,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? priorityCandidates;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? sabhasWithCandidates;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalCandidates;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Kpis &&
    other.priorityCandidates == priorityCandidates &&
    other.sabhasWithCandidates == sabhasWithCandidates &&
    other.totalCandidates == totalCandidates;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (priorityCandidates == null ? 0 : priorityCandidates!.hashCode) +
    (sabhasWithCandidates == null ? 0 : sabhasWithCandidates!.hashCode) +
    (totalCandidates == null ? 0 : totalCandidates!.hashCode);

  @override
  String toString() => 'Kpis[priorityCandidates=$priorityCandidates, sabhasWithCandidates=$sabhasWithCandidates, totalCandidates=$totalCandidates]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.priorityCandidates != null) {
      json[r'priorityCandidates'] = this.priorityCandidates;
    } else {
      json[r'priorityCandidates'] = null;
    }
    if (this.sabhasWithCandidates != null) {
      json[r'sabhasWithCandidates'] = this.sabhasWithCandidates;
    } else {
      json[r'sabhasWithCandidates'] = null;
    }
    if (this.totalCandidates != null) {
      json[r'totalCandidates'] = this.totalCandidates;
    } else {
      json[r'totalCandidates'] = null;
    }
    return json;
  }

  /// Returns a new [Kpis] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static Kpis? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return Kpis(
        priorityCandidates: mapValueOfType<int>(json, r'priorityCandidates'),
        sabhasWithCandidates: mapValueOfType<int>(json, r'sabhasWithCandidates'),
        totalCandidates: mapValueOfType<int>(json, r'totalCandidates'),
      );
    }
    return null;
  }

  static List<Kpis> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Kpis>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Kpis.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Kpis> mapFromJson(dynamic json) {
    final map = <String, Kpis>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = Kpis.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Kpis-objects as value to a dart map
  static Map<String, List<Kpis>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Kpis>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Kpis.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

