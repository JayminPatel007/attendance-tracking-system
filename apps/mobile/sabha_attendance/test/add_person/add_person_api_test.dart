import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:sabha_attendance_mobile/add_person/add_person_api.dart';

void main() {
  AddPersonApi apiReturning(http.Response Function(http.Request) handler) {
    return AddPersonApi(
      baseUrl: 'http://test',
      accessToken: 'tok',
      client: MockClient((req) async => handler(req)),
    );
  }

  group('findByMobile', () {
    test('returns the existing Person on a 200 hit', () async {
      final api = apiReturning((req) {
        expect(req.method, 'GET');
        expect(req.url.path, '/api/directory/persons');
        expect(req.url.queryParameters['mobile'], '+919820111122');
        expect(req.headers['Authorization'], 'Bearer tok');
        return http.Response(
          jsonEncode({
            'id': 'person-1',
            'fullName': 'Ravi Patel',
            'gender': 'MALE',
            'dateOfBirth': null,
            'mobile': '+919820111122',
            'guardianPersonId': null,
          }),
          200,
        );
      });

      final person = await api.findByMobile('+919820111122');

      expect(person, isNotNull);
      expect(person!.id, 'person-1');
      expect(person.fullName, 'Ravi Patel');
    });

    test('returns null when no Person has that mobile (404)', () async {
      final api = apiReturning((req) => http.Response('', 404));
      expect(await api.findByMobile('+910000000000'), isNull);
    });

    test('surfaces a transport failure rather than reading it as "new number"', () async {
      final api = apiReturning((req) => http.Response('boom', 500));
      await expectLater(
        () => api.findByMobile('+919820111122'),
        throwsA(isA<AddPersonApiException>()),
      );
    });
  });

  group('add', () {
    test('returns a created outcome on 201', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(
          jsonEncode({'personId': 'new-1', 'candidates': <dynamic>[], 'requiresOverride': false}),
          201,
        );
      });

      final outcome = await api.add(const AddPersonRequest(
        fullName: 'Jay Mehta',
        gender: 'MALE',
        mobile: '+919999000111',
        homeSabhaId: 'sabha-1',
      ));

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/directory/persons');
      expect(jsonDecode(captured.body), {
        'fullName': 'Jay Mehta',
        'gender': 'MALE',
        'dateOfBirth': null,
        'mobile': '+919999000111',
        'guardianPersonId': null,
        'homeSabhaId': 'sabha-1',
        'overrideDuplicateWarning': false,
      });
      expect(outcome.created, isTrue);
      expect(outcome.createdPersonId, 'new-1');
      expect(outcome.candidates, isEmpty);
    });

    test('returns a soft-warn outcome with candidates on 200', () async {
      final api = apiReturning((req) => http.Response(
            jsonEncode({
              'personId': null,
              'requiresOverride': true,
              'candidates': [
                {
                  'personId': 'cand-1',
                  'fullName': 'Jai Mehta',
                  'homeSabhas': ['REGULAR_YUVAK', 'REGULAR_SANYUKTA'],
                },
              ],
            }),
            200,
          ));

      final outcome = await api.add(const AddPersonRequest(
        fullName: 'Jay Mehta',
        gender: 'MALE',
        mobile: '+919999000222',
        homeSabhaId: 'sabha-1',
      ));

      expect(outcome.created, isFalse);
      expect(outcome.requiresOverride, isTrue);
      expect(outcome.candidates, hasLength(1));
      expect(outcome.candidates.single.personId, 'cand-1');
      expect(outcome.candidates.single.fullName, 'Jai Mehta');
      expect(outcome.candidates.single.homeSabhas, ['REGULAR_YUVAK', 'REGULAR_SANYUKTA']);
    });

    test('a 409 hard block surfaces the existing Person id', () async {
      final api = apiReturning((req) => http.Response(
            jsonEncode({
              'status': 409,
              'title': 'Conflict',
              'detail': 'Mobile already registered to Person person-9',
              'code': 'MOBILE_ALREADY_REGISTERED',
              'existingPersonId': 'person-9',
            }),
            409,
          ));

      await expectLater(
        () => api.add(const AddPersonRequest(
          fullName: 'Dup',
          gender: 'MALE',
          mobile: '+919820111122',
          homeSabhaId: 'sabha-1',
        )),
        throwsA(isA<MobileAlreadyRegisteredException>()
            .having((e) => e.existingPersonId, 'existingPersonId', 'person-9')),
      );
    });

    test('a 422 surfaces the domain rule message', () async {
      final api = apiReturning((req) => http.Response(
            jsonEncode({'status': 422, 'title': 'Unprocessable Entity', 'detail': 'guardian or mobile required'}),
            422,
          ));

      await expectLater(
        () => api.add(const AddPersonRequest(
          fullName: 'Nobody',
          gender: 'MALE',
          homeSabhaId: 'sabha-1',
        )),
        throwsA(isA<DirectoryRuleException>()
            .having((e) => e.message, 'message', 'guardian or mobile required')),
      );
    });

    /// A birthdate is a calendar date, not an instant. The generated request
    /// model types it as a `DateTime` and serializes `.toUtc()`, which walks the
    /// date back a day on any device east of Greenwich (issue #104). This asserts
    /// the entered date leaves the seam untouched — and holds in every device
    /// timezone, because nothing here builds a local-time value.
    test('sends a birthdate as the entered calendar date, unshifted', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(
          jsonEncode({'personId': 'child-2', 'candidates': <dynamic>[], 'requiresOverride': false}),
          201,
        );
      });

      await api.add(const AddPersonRequest(
        fullName: 'Ravi Patel',
        gender: 'MALE',
        dateOfBirth: '2010-05-01',
        mobile: '+919999000333',
        homeSabhaId: 'sabha-1',
      ));

      expect((jsonDecode(captured.body) as Map<String, dynamic>)['dateOfBirth'], '2010-05-01');
      // The body assertion alone can't catch the shift on a UTC machine (CI runs
      // there): a local-midnight value would serialize to 2010-05-01 too. What
      // makes the generated model's `.toUtc()` a no-op in *every* timezone is
      // that the value is already UTC — assert that directly.
      expect(
        const AddPersonRequest(
          fullName: 'Ravi Patel',
          gender: 'MALE',
          dateOfBirth: '2010-05-01',
          homeSabhaId: 'sabha-1',
        ).toApi().dateOfBirth,
        DateTime.utc(2010, 5, 1),
      );
    });

    test('rejects a gender the contract has no value for instead of dropping it', () async {
      final api = apiReturning((req) => http.Response('', 201));

      await expectLater(
        () => api.add(const AddPersonRequest(
          fullName: 'Mystery Patel',
          gender: 'Male',
          mobile: '+919999000555',
          homeSabhaId: 'sabha-1',
        )),
        throwsA(isA<DirectoryRuleException>()),
      );
    });

    /// The DOB field is free text ("YYYY-MM-DD" is only a hint), so a typo has to
    /// come back as something the adder can act on rather than as the generic
    /// connection error every unhandled failure falls into.
    test('a date of birth that is not a date is rejected with a readable message', () async {
      final api = apiReturning((req) => http.Response('', 201));

      await expectLater(
        () => api.add(const AddPersonRequest(
          fullName: 'Typo Patel',
          gender: 'MALE',
          dateOfBirth: '1 May 2010',
          mobile: '+919999000444',
          homeSabhaId: 'sabha-1',
        )),
        throwsA(isA<DirectoryRuleException>()
            .having((e) => e.message, 'message', contains('YYYY-MM-DD'))),
      );
    });

    test('sends a guardian-linked child with a null mobile', () async {
      late http.Request captured;
      final api = apiReturning((req) {
        captured = req;
        return http.Response(
          jsonEncode({'personId': 'child-1', 'candidates': <dynamic>[], 'requiresOverride': false}),
          201,
        );
      });

      await api.add(const AddPersonRequest(
        fullName: 'Child Patel',
        gender: 'MALE',
        guardianPersonId: 'parent-1',
        homeSabhaId: 'sabha-1',
      ));

      final body = jsonDecode(captured.body) as Map<String, dynamic>;
      expect(body['mobile'], isNull);
      expect(body['guardianPersonId'], 'parent-1');
    });
  });
}
