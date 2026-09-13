//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class ProxySabhaListItem {
  /// Returns a new [ProxySabhaListItem] instance.
  ProxySabhaListItem({
    required this.lastSeenAt,
    required this.sabhaId,
    required this.sabhaLabel,
    required this.sanchalakName,
    required this.sanchalakUserId,
  });

  DateTime? lastSeenAt;

  String sabhaId;

  String sabhaLabel;

  String? sanchalakName;

  String? sanchalakUserId;

  @override
  bool operator ==(Object other) => identical(this, other) || other is ProxySabhaListItem &&
    other.lastSeenAt == lastSeenAt &&
    other.sabhaId == sabhaId &&
    other.sabhaLabel == sabhaLabel &&
    other.sanchalakName == sanchalakName &&
    other.sanchalakUserId == sanchalakUserId;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (lastSeenAt == null ? 0 : lastSeenAt!.hashCode) +
    (sabhaId.hashCode) +
    (sabhaLabel.hashCode) +
    (sanchalakName == null ? 0 : sanchalakName!.hashCode) +
    (sanchalakUserId == null ? 0 : sanchalakUserId!.hashCode);

  @override
  String toString() => 'ProxySabhaListItem[lastSeenAt=$lastSeenAt, sabhaId=$sabhaId, sabhaLabel=$sabhaLabel, sanchalakName=$sanchalakName, sanchalakUserId=$sanchalakUserId]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.lastSeenAt != null) {
      json[r'lastSeenAt'] = this.lastSeenAt!.toUtc().toIso8601String();
    } else {
      json[r'lastSeenAt'] = null;
    }
      json[r'sabhaId'] = this.sabhaId;
      json[r'sabhaLabel'] = this.sabhaLabel;
    if (this.sanchalakName != null) {
      json[r'sanchalakName'] = this.sanchalakName;
    } else {
      json[r'sanchalakName'] = null;
    }
    if (this.sanchalakUserId != null) {
      json[r'sanchalakUserId'] = this.sanchalakUserId;
    } else {
      json[r'sanchalakUserId'] = null;
    }
    return json;
  }

  /// Returns a new [ProxySabhaListItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static ProxySabhaListItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'lastSeenAt'), 'Required key "ProxySabhaListItem[lastSeenAt]" is missing from JSON.');
        assert(json.containsKey(r'sabhaId'), 'Required key "ProxySabhaListItem[sabhaId]" is missing from JSON.');
        assert(json[r'sabhaId'] != null, 'Required key "ProxySabhaListItem[sabhaId]" has a null value in JSON.');
        assert(json.containsKey(r'sabhaLabel'), 'Required key "ProxySabhaListItem[sabhaLabel]" is missing from JSON.');
        assert(json[r'sabhaLabel'] != null, 'Required key "ProxySabhaListItem[sabhaLabel]" has a null value in JSON.');
        assert(json.containsKey(r'sanchalakName'), 'Required key "ProxySabhaListItem[sanchalakName]" is missing from JSON.');
        assert(json.containsKey(r'sanchalakUserId'), 'Required key "ProxySabhaListItem[sanchalakUserId]" is missing from JSON.');
        return true;
      }());

      return ProxySabhaListItem(
        lastSeenAt: mapDateTime(json, r'lastSeenAt', r''),
        sabhaId: mapValueOfType<String>(json, r'sabhaId')!,
        sabhaLabel: mapValueOfType<String>(json, r'sabhaLabel')!,
        sanchalakName: mapValueOfType<String>(json, r'sanchalakName'),
        sanchalakUserId: mapValueOfType<String>(json, r'sanchalakUserId'),
      );
    }
    return null;
  }

  static List<ProxySabhaListItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ProxySabhaListItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ProxySabhaListItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, ProxySabhaListItem> mapFromJson(dynamic json) {
    final map = <String, ProxySabhaListItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = ProxySabhaListItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of ProxySabhaListItem-objects as value to a dart map
  static Map<String, List<ProxySabhaListItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<ProxySabhaListItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = ProxySabhaListItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'lastSeenAt',
    'sabhaId',
    'sabhaLabel',
    'sanchalakName',
    'sanchalakUserId',
  };
}

