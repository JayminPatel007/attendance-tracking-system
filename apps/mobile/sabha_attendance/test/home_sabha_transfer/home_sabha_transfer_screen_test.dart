import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_api.dart';
import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_controller.dart';
import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_screen.dart';

class _FakeApi extends HomeSabhaTransferApi {
  _FakeApi({this.onFind, this.onInitiate, this.onConfirm})
      : super(baseUrl: 'http://test', accessToken: 'tok');

  Future<TransferPerson?> Function(String mobile)? onFind;
  Future<String> Function(String personId, String destinationSabhaId)? onInitiate;
  Future<void> Function(String transferId, String otpCode)? onConfirm;

  @override
  Future<TransferPerson?> findByMobile(String mobile) => onFind!(mobile);

  @override
  Future<String> initiate({required String personId, required String destinationSabhaId}) =>
      onInitiate!(personId, destinationSabhaId);

  @override
  Future<void> confirm({required String transferId, required String otpCode}) =>
      onConfirm!(transferId, otpCode);
}

void main() {
  HomeSabhaTransferController controllerWith(_FakeApi api) =>
      HomeSabhaTransferController(api: api, destinationSabhaId: 'sabha-2');

  Future<void> pump(WidgetTester tester, HomeSabhaTransferController controller) {
    return tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: HomeSabhaTransferScreen(controller: controller, destinationLabel: 'Your Sabha'),
      ),
    ));
  }

  testWidgets('full flow: find Person → confirm → send OTP → enter OTP → done', (tester) async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => TransferPerson(id: 'p-1', fullName: 'Ravi Patel', mobile: '+919820100200'),
      onInitiate: (_, __) async => 't-9',
      onConfirm: (_, __) async {},
    ));
    await pump(tester, controller);

    await tester.enterText(find.byKey(const Key('hsat-mobile-field')), '+919820100200');
    await tester.pump();
    await tester.tap(find.byKey(const Key('hsat-find-button')));
    await tester.pumpAndSettle();

    expect(find.text('Ravi Patel'), findsOneWidget);

    await tester.tap(find.byKey(const Key('hsat-send-otp-button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('hsat-otp-field')), '123456');
    await tester.pump();
    await tester.tap(find.byKey(const Key('hsat-verify-button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('hsat-done')), findsOneWidget);
  });

  testWidgets('a wrong OTP shows the error and stays on the OTP step', (tester) async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => TransferPerson(id: 'p-1', fullName: 'Ravi Patel', mobile: '+919820100200'),
      onInitiate: (_, __) async => 't-9',
      onConfirm: (_, __) async => throw TransferRejectedException('Incorrect OTP'),
    ));
    await pump(tester, controller);
    await tester.enterText(find.byKey(const Key('hsat-mobile-field')), '+919820100200');
    await tester.pump();
    await tester.tap(find.byKey(const Key('hsat-find-button')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('hsat-send-otp-button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('hsat-otp-field')), '000000');
    await tester.pump();
    await tester.tap(find.byKey(const Key('hsat-verify-button')));
    await tester.pumpAndSettle();

    expect(find.text('Incorrect OTP'), findsOneWidget);
    expect(find.byKey(const Key('hsat-verify-button')), findsOneWidget);
    expect(find.byKey(const Key('hsat-done')), findsNothing);
  });
}
