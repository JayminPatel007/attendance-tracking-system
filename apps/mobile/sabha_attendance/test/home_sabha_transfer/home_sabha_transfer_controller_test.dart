import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_api.dart';
import 'package:sabha_attendance_mobile/home_sabha_transfer/home_sabha_transfer_controller.dart';

/// Overrides the three network methods so the controller's state machine can be
/// driven without HTTP. Each test wires only the closures it needs.
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
  TransferPerson person(String id) =>
      TransferPerson(id: id, fullName: 'Ravi Patel', mobile: '+919820100200');

  HomeSabhaTransferController controllerWith(_FakeApi api) =>
      HomeSabhaTransferController(api: api, destinationSabhaId: 'sabha-2');

  test('finding a Person by mobile moves to the confirm step', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => person('p-1')));

    await controller.findByMobile('+919820100200');

    expect(controller.state.view, HomeSabhaTransferView.confirm);
    expect(controller.state.person?.id, 'p-1');
  });

  test('a mobile with no Directory match stays on find with an error', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => null));

    await controller.findByMobile('+910000000000');

    expect(controller.state.view, HomeSabhaTransferView.find);
    expect(controller.state.person, isNull);
    expect(controller.state.error, isNotNull);
  });

  test('sending the OTP initiates for the Person + destination and moves to OTP', () async {
    String? capturedPerson;
    String? capturedDest;
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => person('p-1'),
      onInitiate: (pid, dest) async {
        capturedPerson = pid;
        capturedDest = dest;
        return 't-9';
      },
    ));
    await controller.findByMobile('+919820100200');

    await controller.sendOtp();

    expect(capturedPerson, 'p-1');
    expect(capturedDest, 'sabha-2');
    expect(controller.state.view, HomeSabhaTransferView.otp);
    expect(controller.state.transferId, 't-9');
  });

  test('a rate-limited OTP send keeps the confirm step and shows the message', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => person('p-1'),
      onInitiate: (_, __) async => throw TransferRateLimitedException('Too many OTPs'),
    ));
    await controller.findByMobile('+919820100200');

    await controller.sendOtp();

    expect(controller.state.view, HomeSabhaTransferView.confirm);
    expect(controller.state.error, 'Too many OTPs');
  });

  test('a correct OTP confirms the transfer id and completes the flow', () async {
    String? capturedId;
    String? capturedCode;
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => person('p-1'),
      onInitiate: (_, __) async => 't-9',
      onConfirm: (id, code) async {
        capturedId = id;
        capturedCode = code;
      },
    ));
    await controller.findByMobile('+919820100200');
    await controller.sendOtp();

    await controller.confirm('123456');

    expect(capturedId, 't-9');
    expect(capturedCode, '123456');
    expect(controller.state.view, HomeSabhaTransferView.done);
  });

  test('a wrong OTP stays on the OTP step with the server message', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => person('p-1'),
      onInitiate: (_, __) async => 't-9',
      onConfirm: (_, __) async => throw TransferRejectedException('Incorrect OTP'),
    ));
    await controller.findByMobile('+919820100200');
    await controller.sendOtp();

    await controller.confirm('000000');

    expect(controller.state.view, HomeSabhaTransferView.otp);
    expect(controller.state.otpError, 'Incorrect OTP');
  });

  test('going back from confirm returns to find and clears the Person', () async {
    final controller = controllerWith(_FakeApi(onFind: (_) async => person('p-1')));
    await controller.findByMobile('+919820100200');

    controller.backToFind();

    expect(controller.state.view, HomeSabhaTransferView.find);
    expect(controller.state.person, isNull);
  });

  test('going back from OTP returns to confirm and clears the OTP error', () async {
    final controller = controllerWith(_FakeApi(
      onFind: (_) async => person('p-1'),
      onInitiate: (_, __) async => 't-9',
      onConfirm: (_, __) async => throw TransferRejectedException('Incorrect OTP'),
    ));
    await controller.findByMobile('+919820100200');
    await controller.sendOtp();
    await controller.confirm('000000');

    controller.backToConfirm();

    expect(controller.state.view, HomeSabhaTransferView.confirm);
    expect(controller.state.otpError, isNull);
  });
}
