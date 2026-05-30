import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart' show databaseFactory;

import 'auth/auth_config.dart';
import 'auth/auth_service.dart';
import 'auth/login_screen.dart';
import 'auth/session.dart';
import 'occurrence_control/occurrence_control_api.dart';
import 'occurrence_control/occurrence_control_controller.dart';
import 'occurrence_control/occurrence_control_screen.dart';
import 'roster/roster_api.dart';
import 'roster/roster_controller.dart';
import 'roster/roster_screen.dart';
import 'sync/attendance_store.dart';
import 'sync/sync_engine.dart';
import 'sync/sync_status_screen.dart';

void main() {
  final config = AuthConfig.fromDartDefines();
  runApp(SabhaAttendanceApp(config: config));
}

class SabhaAttendanceApp extends StatelessWidget {
  const SabhaAttendanceApp({super.key, AuthConfig? config, AuthService? authService})
      : _config = config,
        _authService = authService;

  final AuthConfig? _config;
  final AuthService? _authService;

  @override
  Widget build(BuildContext context) {
    final config = _config ?? AuthConfig.fromDartDefines();
    final session = Session();
    final auth = _authService ?? AuthService(config: config);

    return MaterialApp(
      title: 'Sabha Attendance',
      theme: ThemeData(useMaterial3: true),
      home: AppShell(session: session, auth: auth, config: config),
    );
  }
}

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.session, required this.auth, required this.config});

  final Session session;
  final AuthService auth;
  final AuthConfig config;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  Future<AttendanceStore>? _storeFuture;

  Future<AttendanceStore> _openStore() async {
    final dir = await getApplicationDocumentsDirectory();
    return AttendanceStore.open(factory: databaseFactory, directory: dir.path);
  }

  @override
  void initState() {
    super.initState();
    _storeFuture = _openStore();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sabha Attendance')),
      body: ValueListenableBuilder<String?>(
        valueListenable: widget.session.accessToken,
        builder: (context, token, _) {
          if (token == null) {
            return LoginScreen(session: widget.session, auth: widget.auth);
          }
          return FutureBuilder<AttendanceStore>(
            future: _storeFuture,
            builder: (context, snap) {
              if (!snap.hasData) {
                return const Center(child: CircularProgressIndicator());
              }
              final store = snap.data!;
              final api = RosterApi(baseUrl: widget.config.backendBaseUrl, accessToken: token);
              final engine = SyncEngine(store: store, api: api, clock: () => DateTime.now().toUtc());
              final controller = RosterController(api: api, store: store, syncEngine: engine);
              final occurrenceControlApi =
                  OccurrenceControlApi(baseUrl: widget.config.backendBaseUrl, accessToken: token);
              return _RosterShell(
                controller: controller,
                occurrenceControlApi: occurrenceControlApi,
                onSignOut: widget.session.clear,
              );
            },
          );
        },
      ),
    );
  }
}

class _RosterShell extends StatefulWidget {
  const _RosterShell({
    required this.controller,
    required this.occurrenceControlApi,
    required this.onSignOut,
  });

  final RosterController controller;
  final OccurrenceControlApi occurrenceControlApi;
  final VoidCallback onSignOut;

  @override
  State<_RosterShell> createState() => _RosterShellState();
}

class _RosterShellState extends State<_RosterShell> {
  @override
  void initState() {
    super.initState();
    widget.controller.initialize();
  }

  void _openSyncStatus() {
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => SyncStatusScreen(controller: widget.controller),
    ));
  }

  void _openOccurrenceControl() {
    final controller = OccurrenceControlController(api: widget.occurrenceControlApi);
    controller.initialize();
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Manage Sabha')),
        body: OccurrenceControlScreen(controller: controller),
      ),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return RosterScreen(
      controller: widget.controller,
      onSignOut: widget.onSignOut,
      onOpenSyncStatus: _openSyncStatus,
      onOpenOccurrenceControl: _openOccurrenceControl,
    );
  }
}
