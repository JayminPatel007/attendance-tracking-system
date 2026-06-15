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
