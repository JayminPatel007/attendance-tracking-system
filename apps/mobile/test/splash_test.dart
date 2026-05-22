import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sabha_attendance_mobile/main.dart';

void main() {
  testWidgets('splash screen shows the Sabha Attendance app title', (tester) async {
    await tester.pumpWidget(const SabhaAttendanceApp());
    expect(find.text('Sabha Attendance'), findsOneWidget);
  });
}
