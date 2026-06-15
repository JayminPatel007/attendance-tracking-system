import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:sabha_attendance_mobile/roster/roster_api.dart';
import 'package:sabha_attendance_mobile/sync/attendance_store.dart';
import 'package:sabha_attendance_mobile/walk_in/walk_in_api.dart';
import 'package:sabha_attendance_mobile/walk_in/walk_in_controller.dart';

/// Overrides the two network calls so the controller's state machine runs
/// without HTTP. Each test wires the closures it needs.
class _FakeApi extends WalkInApi {
  _FakeApi({this.onSearch, this.onRecord}) : super(baseUrl: 'http://test', accessToken: 'tok');

  Future<List<WalkInCandidate>> Function(String sabhaId, String query)? onSearch;
  Future<void> Function(String occurrenceId, String personId)? onRecord;

  @override
  Future<List<WalkInCandidate>> search({required String sabhaId, required String query}) =>
      onSearch!(sabhaId, query);

  @override
  Future<void> recordWalkIn({required String occurrenceId, required String personId}) =>
      onRecord!(occurrenceId, personId);
}

void main() {
  sqfliteFfiInit();
  late AttendanceStore store;

  setUp(() async {
    store = await AttendanceStore.openInMemory(factory: databaseFactoryFfi);
  });

  tearDown(() async {
    await store.close();
  });

  WalkInController controllerWith(_FakeApi api) => WalkInController(
        api: api,
        store: store,
        occurrenceId: 'occ-1',
        sabhaId: 'sabha-1',
      );

  Future<void> cacheRosterWith(List<RosterEntry> entries) => store.cacheRoster(CurrentRoster(
        occurrence: OccurrenceView(id: 'occ-1', date: '2026-05-31', state: 'OPEN_FOR_MARKING', sabhaId: 'sabha-1'),
        roster: entries,
        rosterVersion: DateTime.utc(2026, 5, 31),
      ));

  test('an online search lists Directory candidates', () async {
    final controller = controllerWith(_FakeApi(onSearch: (sabhaId, query) async {
      expect(sabhaId, 'sabha-1');
      return [WalkInCandidate(personId: 'p-110', fullName: 'Ramesh Shah', homeSabhas: const ['REGULAR_BAAL'])];
    }));

    await controller.search('Ramesh');

    expect(controller.state.offline, isFalse);
    expect(controller.state.results, hasLength(1));
    expect(controller.state.results.single.fullName, 'Ramesh Shah');
    expect(controller.state.showWiderDirectoryHint, isFalse);
  });

  test('selecting a candidate moves to confirm and exposes their Home Sabha', () async {
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async =>
          [WalkInCandidate(personId: 'p-110', fullName: 'Ramesh Shah', homeSabhas: const ['REGULAR_BAAL'])],
    ));
    await controller.search('Ramesh');

    controller.select(controller.state.results.single);

    expect(controller.state.view, WalkInView.confirm);
    expect(controller.state.selected?.homeSabhas, const ['REGULAR_BAAL']);
  });

  test('recording a selected candidate posts and moves to recorded', () async {
    final recorded = <String>[];
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async =>
          [WalkInCandidate(personId: 'p-110', fullName: 'Ramesh Shah', homeSabhas: const ['REGULAR_BAAL'])],
      onRecord: (occ, person) async => recorded.add('$occ/$person'),
    ));
    await controller.search('Ramesh');
    controller.select(controller.state.results.single);

    await controller.record();

    expect(recorded, ['occ-1/p-110']);
    expect(controller.state.view, WalkInView.recorded);
  });

  test('offline, search falls back to the cached Roster only', () async {
    await cacheRosterWith([
      RosterEntry(personId: 'r1', fullName: 'Ramesh Roster', present: null),
      RosterEntry(personId: 'r2', fullName: 'Suresh Roster', present: null),
    ]);
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => throw http.ClientException('offline'),
    ));

    await controller.search('ramesh');

    expect(controller.state.offline, isTrue);
    expect(controller.state.results, hasLength(1));
    expect(controller.state.results.single.personId, 'r1');
    expect(controller.state.showWiderDirectoryHint, isFalse);
  });

  test('offline with no cached-Roster match flags searching the wider Directory online', () async {
    await cacheRosterWith([RosterEntry(personId: 'r1', fullName: 'Ramesh Roster', present: null)]);
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => throw http.ClientException('offline'),
    ));

    await controller.search('Visitor Not In Roster');

    expect(controller.state.offline, isTrue);
    expect(controller.state.results, isEmpty);
    expect(controller.state.showWiderDirectoryHint, isTrue);
  });

  test('a server error while online surfaces an error, not an offline fallback', () async {
    final controller = controllerWith(_FakeApi(
      onSearch: (_, __) async => throw WalkInApiException('GET walk-in-search -> 500'),
    ));

    await controller.search('Ramesh');

    expect(controller.state.offline, isFalse);
    expect(controller.state.error, isNotNull);
  });
}
