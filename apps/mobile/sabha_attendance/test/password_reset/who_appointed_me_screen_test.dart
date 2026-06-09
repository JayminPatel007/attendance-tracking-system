import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/password_reset/password_reset_api.dart';
import 'package:sabha_attendance_mobile/password_reset/who_appointed_me_screen.dart';

class _FakeApi extends PasswordResetApi {
  _FakeApi(this.onLookup) : super(baseUrl: 'http://test');

  final Future<List<AppointerContact>> Function(String username) onLookup;

  @override
  Future<List<AppointerContact>> whoAppointedMe(String username) => onLookup(username);
}

void main() {
  Future<void> pump(WidgetTester tester, PasswordResetApi api) {
    return tester.pumpWidget(MaterialApp(home: Scaffold(body: WhoAppointedMeScreen(api: api))));
  }

  testWidgets('shows the contacts who can reissue the password', (tester) async {
    await pump(
      tester,
      _FakeApi((_) async => [AppointerContact(name: 'Suresh', mobile: '+919820000001')]),
    );

    await tester.enterText(find.byKey(const Key('wam-username-field')), 'ramesh.bhai');
    await tester.pump();
    await tester.tap(find.byKey(const Key('wam-lookup-button')));
    await tester.pumpAndSettle();

    expect(find.text('Suresh'), findsOneWidget);
    expect(find.text('+919820000001'), findsOneWidget);
  });

  testWidgets('shows a not-found message when the username is unknown', (tester) async {
    await pump(tester, _FakeApi((_) async => throw UnknownUsernameException('no user')));

    await tester.enterText(find.byKey(const Key('wam-username-field')), 'ghost');
    await tester.pump();
    await tester.tap(find.byKey(const Key('wam-lookup-button')));
    await tester.pumpAndSettle();

    expect(find.textContaining("couldn't find"), findsOneWidget);
  });
}
