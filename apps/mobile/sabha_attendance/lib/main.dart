import 'package:flutter/material.dart';

void main() {
  runApp(const SabhaAttendanceApp());
}

class SabhaAttendanceApp extends StatelessWidget {
  const SabhaAttendanceApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Sabha Attendance',
      theme: ThemeData(useMaterial3: true),
      home: const SplashScreen(),
    );
  }
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Text(
          'Sabha Attendance',
          style: TextStyle(fontSize: 28, fontWeight: FontWeight.w600),
        ),
      ),
    );
  }
}
