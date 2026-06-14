import 'package:flutter_test/flutter_test.dart';
import 'package:http/testing.dart';
import 'package:http/http.dart' as http;

import 'package:sabha_attendance_mobile/selection/selection_api.dart';
import 'package:sabha_attendance_mobile/selection/selection_controller.dart';

void main() {
  SelectionController controllerReturning(http.Response Function(http.Request) handler) {
    return SelectionController(
      api: SelectionApi(
        baseUrl: 'http://test',
        accessToken: 'tok',
        client: MockClient((req) async => handler(req)),
      ),
      regularSabhaId: 'sabha-2',
      people: const [
        Nominee(personId: 'p-1', fullName: 'Asha Patel'),
        Nominee(personId: 'p-2', fullName: 'Ravi Shah'),
      ],
    );
  }

  test('exposes the roster people it was given', () {
    final c = controllerReturning((req) => http.Response('{}', 200));
    expect(c.people.map((p) => p.personId), ['p-1', 'p-2']);
  });

  test('marks a Person nominated on success', () async {
    final c = controllerReturning((req) => http.Response('{"nominationId":"nom-1"}', 200));

    await c.nominate('p-1');

    expect(c.outcomeFor('p-1'), NominationOutcome.nominated);
    expect(c.errorFor('p-1'), isNull);
  });

  test('surfaces the backend message when a Person is already selected', () async {
    final c = controllerReturning(
        (req) => http.Response('{"detail":"already selected"}', 409));

    await c.nominate('p-1');

    expect(c.outcomeFor('p-1'), NominationOutcome.failed);
    expect(c.errorFor('p-1'), 'already selected');
  });

  test('surfaces a domain rejection (422) for the Person', () async {
    final c = controllerReturning(
        (req) => http.Response('{"detail":"no selective track"}', 422));

    await c.nominate('p-2');

    expect(c.outcomeFor('p-2'), NominationOutcome.failed);
    expect(c.errorFor('p-2'), 'no selective track');
    // Other People are untouched.
    expect(c.outcomeFor('p-1'), NominationOutcome.none);
  });

  test('notifies listeners across a nominate', () async {
    final c = controllerReturning((req) => http.Response('{"nominationId":"nom-1"}', 200));
    var notifications = 0;
    c.addListener(() => notifications++);

    await c.nominate('p-1');

    expect(notifications, greaterThanOrEqualTo(1));
  });
}
