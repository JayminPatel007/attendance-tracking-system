//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class DashboardOverview {
  /// Returns a new [DashboardOverview] instance.
  DashboardOverview({
    this.headlineCandidates = const [],
    this.kpis,
  });

  List<CandidateRow> headlineCandidates;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  Kpis? kpis;

  @override
  bool operator ==(Object other) => identical(this, other) || other is DashboardOverview &&
    _deepEquality.equals(other.headlineCandidates, headlineCandidates) &&
    other.kpis == kpis;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (headlineCandidates.hashCode) +
    (kpis == null ? 0 : kpis!.hashCode);

  @override
  String toString() => 'DashboardOverview[headlineCandidates=$headlineCandidates, kpis=$kpis]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'headlineCandidates'] = this.headlineCandidates;
    if (this.kpis != null) {
      json[r'kpis'] = this.kpis;
    } else {
      json[r'kpis'] = null;
    }
    return json;
  }

  /// Returns a new [DashboardOverview] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static DashboardOverview? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return DashboardOverview(
        headlineCandidates: CandidateRow.listFromJson(json[r'headlineCandidates']),
        kpis: Kpis.fromJson(json[r'kpis']),
      );
    }
    return null;
  }

  static List<DashboardOverview> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <DashboardOverview>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = DashboardOverview.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, DashboardOverview> mapFromJson(dynamic json) {
    final map = <String, DashboardOverview>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = DashboardOverview.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of DashboardOverview-objects as value to a dart map
  static Map<String, List<DashboardOverview>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<DashboardOverview>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = DashboardOverview.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

