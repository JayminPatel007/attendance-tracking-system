import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/add_person/add_person_api.dart';
import 'package:sabha_attendance_mobile/add_person/add_person_controller.dart';

/// Overrides the two network methods so the controller's state machine can be
/// driven without HTTP. Each test wires the two closures it needs.
class _FakeApi extends AddPersonApi {
  _FakeApi({this.onFind, this.onAdd}) : super(baseUrl: 'http://test', accessToken: 'tok');

  Future<DirectoryPerson?> Function(String mobile)? onFind;
  Future<AddPersonOutcome> Function(AddPersonRequest req)? onAdd;

  @override
  Future<DirectoryPerson?> findByMobile(String mobile) => onFind!(mobile);

  @override
  Future<AddPersonOutcome> add(AddPersonRequest req) => onAdd!(req);
}

void main() {
  DirectoryPerson person(String id) =>
      DirectoryPerson(id: id, fullName: 'Ravi Patel', gender: 'MALE', mobile: '+919820111122');

  AddPersonController controllerWith(_FakeApi api) =>
      AddPersonController(api: api, homeSabhaId: 'sabha-1');

  test('a matching mobile moves to the existing-Person profile (forced redirect)', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => person('person-9')));

    await controller.checkMobile('+919820111122');

    expect(controller.state.view, AddPersonView.profile);
    expect(controller.state.existingPerson?.id, 'person-9');
  });

  test('a new mobile advances to the details step', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => null));

    await controller.checkMobile('+910000000000');

    expect(controller.state.view, AddPersonView.details);
    expect(controller.state.existingPerson, isNull);
  });

  test('submitting clean details creates the Person', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => null,
      onAdd: (_) async => AddPersonOutcome(createdPersonId: 'new-1', candidates: const [], requiresOverride: false),
    ));
    await controller.checkMobile('+919999000111');

    await controller.submitDetails(fullName: 'Jay Mehta', gender: 'MALE');

    expect(controller.state.view, AddPersonView.created);
    expect(controller.state.createdPersonId, 'new-1');
  });

  test('a name soft-warn surfaces candidates and stays on details without creating', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => null,
      onAdd: (_) async => AddPersonOutcome(
        candidates: [NameCandidate(personId: 'cand-1', fullName: 'Jai Mehta', homeSabhaName: 'REGULAR_YUVAK')],
        requiresOverride: true,
      ),
    ));
    await controller.checkMobile('+919999000222');

    await controller.submitDetails(fullName: 'Jay Mehta', gender: 'MALE');

    expect(controller.state.view, AddPersonView.details);
    expect(controller.state.requiresOverride, isTrue);
    expect(controller.state.candidates, hasLength(1));
    expect(controller.state.createdPersonId, isNull);
  });

  test('create-new-anyway re-submits with the override flag and creates', () async {
    final overrides = <bool>[];
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => null,
      onAdd: (req) async {
        overrides.add(req.overrideDuplicateWarning);
        if (!req.overrideDuplicateWarning) {
          return AddPersonOutcome(
            candidates: [NameCandidate(personId: 'cand-1', fullName: 'Jai Mehta', homeSabhaName: 'REGULAR_YUVAK')],
            requiresOverride: true,
          );
        }
        return AddPersonOutcome(createdPersonId: 'new-2', candidates: const [], requiresOverride: false);
      },
    ));
    await controller.checkMobile('+919999000222');
    await controller.submitDetails(fullName: 'Jay Mehta', gender: 'MALE');

    await controller.createNewAnyway();

    expect(overrides, [false, true]);
    expect(controller.state.view, AddPersonView.created);
    expect(controller.state.createdPersonId, 'new-2');
  });

  test('a hard-block race at submit surfaces the existing Person id as an error', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => null,
      onAdd: (_) async => throw MobileAlreadyRegisteredException(
          existingPersonId: 'person-9', message: 'Mobile already registered'),
    ));
    await controller.checkMobile('+919999000333');

    await controller.submitDetails(fullName: 'Race', gender: 'MALE');

    expect(controller.state.view, AddPersonView.details);
    expect(controller.state.error, isNotNull);
    expect(controller.state.blockedByExistingId, 'person-9');
  });

  test('going back from the profile returns to the mobile step', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => person('person-9')));
    await controller.checkMobile('+919820111122');

    controller.backToMobile();

    expect(controller.state.view, AddPersonView.mobile);
    expect(controller.state.existingPerson, isNull);
  });
}
