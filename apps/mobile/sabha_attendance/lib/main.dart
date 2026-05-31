import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart' show databaseFactory;

import 'add_person/add_person_api.dart';
import 'add_person/add_person_controller.dart';
import 'add_person/add_person_screen.dart';
import 'auth/auth_config.dart';
import 'auth/auth_service.dart';
import 'auth/login_screen.dart';
import 'auth/session.dart';
import 'home_sabha_transfer/home_sabha_transfer_api.dart';
import 'home_sabha_transfer/home_sabha_transfer_controller.dart';
import 'home_sabha_transfer/home_sabha_transfer_screen.dart';
import 'occurrence_control/occurrence_control_api.dart';
import 'occurrence_control/occurrence_control_controller.dart';
import 'occurrence_control/occurrence_control_screen.dart';
import 'roster/roster_api.dart';
import 'roster/roster_controller.dart';
import 'roster/roster_screen.dart';
import 'sync/attendance_store.dart';
import 'walk_in/walk_in_api.dart';
import 'walk_in/walk_in_controller.dart';
import 'walk_in/walk_in_screen.dart';
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
              final addPersonApi =
                  AddPersonApi(baseUrl: widget.config.backendBaseUrl, accessToken: token);
              final walkInApi =
                  WalkInApi(baseUrl: widget.config.backendBaseUrl, accessToken: token);
              final homeSabhaTransferApi =
                  HomeSabhaTransferApi(baseUrl: widget.config.backendBaseUrl, accessToken: token);
              return _RosterShell(
                controller: controller,
                occurrenceControlApi: occurrenceControlApi,
                addPersonApi: addPersonApi,
                walkInApi: walkInApi,
                homeSabhaTransferApi: homeSabhaTransferApi,
                store: store,
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
    required this.addPersonApi,
    required this.walkInApi,
    required this.homeSabhaTransferApi,
    required this.store,
    required this.onSignOut,
  });

  final RosterController controller;
  final OccurrenceControlApi occurrenceControlApi;
  final AddPersonApi addPersonApi;
  final WalkInApi walkInApi;
  final HomeSabhaTransferApi homeSabhaTransferApi;
  final AttendanceStore store;
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

  void _openAddPerson() {
    // The Home Sabha a new Person is registered to is the Sanchalak's current
    // Sabha — the realistic "this Person attended our Sabha" case (ADR-0013).
    final occurrence = widget.controller.state.roster?.occurrence;
    if (occurrence == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Load a roster first to choose a Home Sabha.')),
      );
      return;
    }
    final controller =
        AddPersonController(api: widget.addPersonApi, homeSabhaId: occurrence.sabhaId);
    final navigator = Navigator.of(context);
    navigator.push(MaterialPageRoute(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Add Person')),
        body: AddPersonScreen(
          controller: controller,
          homeSabhaLabel: 'Your current Sabha · ${occurrence.date}',
          onSelectExisting: (_) => navigator.pop(),
          onCreated: (_) => navigator.pop(),
        ),
      ),
    ));
  }

  void _openWalkIn() {
    // A Walk-in is recorded against today's open Occurrence; the search is
    // scoped server-side to that Occurrence's Sabha/Kshetra (ADR-0007).
    final occurrence = widget.controller.state.roster?.occurrence;
    if (occurrence == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Load a roster first to record a Walk-in.')),
      );
      return;
    }
    final controller = WalkInController(
      api: widget.walkInApi,
      store: widget.store,
      occurrenceId: occurrence.id,
      sabhaId: occurrence.sabhaId,
    );
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Walk-in')),
        body: WalkInScreen(controller: controller),
      ),
    ));
  }

  void _openHomeSabhaTransfer() {
    // The destination Sabha is the Sanchalak's current Sabha — the Person is
    // pulled into it after their own OTP-verified consent (ADR-0002).
    final occurrence = widget.controller.state.roster?.occurrence;
    if (occurrence == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Load a roster first to start a transfer.')),
      );
      return;
    }
    final controller = HomeSabhaTransferController(
      api: widget.homeSabhaTransferApi,
      destinationSabhaId: occurrence.sabhaId,
    );
    final navigator = Navigator.of(context);
    navigator.push(MaterialPageRoute(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Home Sabha transfer')),
        body: HomeSabhaTransferScreen(
          controller: controller,
          destinationLabel: 'Your Sabha · ${occurrence.date}',
          onDone: navigator.pop,
        ),
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
      onOpenAddPerson: _openAddPerson,
      onOpenWalkIn: _openWalkIn,
      onOpenHomeSabhaTransfer: _openHomeSabhaTransfer,
    );
  }
}
