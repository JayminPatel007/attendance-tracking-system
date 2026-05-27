import 'package:flutter/material.dart';

import 'roster_api.dart';
import 'roster_controller.dart';

/// Offline-tolerant marking screen (ADR-0007). Renders state owned by
/// [RosterController] and dispatches user taps back into it; all async I/O
/// (HTTP, sqflite) is the controller's responsibility.
class RosterScreen extends StatefulWidget {
  const RosterScreen({
    super.key,
    required this.controller,
    this.onSignOut,
    this.onOpenSyncStatus,
  });

  final RosterController controller;
  final VoidCallback? onSignOut;
  final VoidCallback? onOpenSyncStatus;

  @override
  State<RosterScreen> createState() => _RosterScreenState();
}

class _RosterScreenState extends State<RosterScreen> {
  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    super.dispose();
  }

  void _onChange() {
    if (mounted) setState(() {});
  }

  Future<void> _syncNow() async {
    await widget.controller.syncNow();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    if (state.loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.roster == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Text(state.error ?? 'No roster available.'),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () => widget.controller.initialize(),
              child: const Text('Retry'),
            ),
            if (widget.onSignOut != null) ...[
              const SizedBox(height: 8),
              TextButton(onPressed: widget.onSignOut, child: const Text('Sign out')),
            ],
          ]),
        ),
      );
    }

    final body = _RosterBody(
      roster: state.roster!,
      displayedPresence: state.displayedPresence,
      onTap: state.blocked ? null : (RosterEntry e) => widget.controller.toggle(e),
      onOpenSyncStatus: widget.onOpenSyncStatus,
      onSignOut: widget.onSignOut,
    );

    if (state.blocked) {
      return Stack(children: [
        AbsorbPointer(child: body),
        _StaleRosterModal(daysStale: state.staleDays, onSyncNow: _syncNow),
      ]);
    }
    return body;
  }
}

class _RosterBody extends StatelessWidget {
  const _RosterBody({
    required this.roster,
    required this.displayedPresence,
    required this.onTap,
    required this.onOpenSyncStatus,
    required this.onSignOut,
  });

  final CurrentRoster roster;
  final bool? Function(RosterEntry) displayedPresence;
  final Future<void> Function(RosterEntry)? onTap;
  final VoidCallback? onOpenSyncStatus;
  final VoidCallback? onSignOut;

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.all(16),
        child: Row(children: [
          Expanded(
            child: Text(
              '${roster.occurrence.date} · ${roster.occurrence.state.replaceAll('_', ' ').toLowerCase()}',
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
          ),
          if (onOpenSyncStatus != null)
            IconButton(
              icon: const Icon(Icons.sync),
              tooltip: 'Sync status',
              onPressed: onOpenSyncStatus,
            ),
        ]),
      ),
      Expanded(
        child: ListView.separated(
          itemCount: roster.roster.length,
          separatorBuilder: (_, __) => const Divider(height: 1),
          itemBuilder: (context, i) {
            final e = roster.roster[i];
            return ListTile(
              title: Text(e.fullName),
              trailing: _PresenceIcon(present: displayedPresence(e)),
              onTap: onTap == null ? null : () => onTap!(e),
            );
          },
        ),
      ),
      if (onSignOut != null)
        Padding(
          padding: const EdgeInsets.all(8),
          child: TextButton(onPressed: onSignOut, child: const Text('Sign out')),
        ),
    ]);
  }
}

class _StaleRosterModal extends StatelessWidget {
  const _StaleRosterModal({required this.daysStale, required this.onSyncNow});
  final int daysStale;
  final Future<void> Function() onSyncNow;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.black54,
      alignment: Alignment.center,
      padding: const EdgeInsets.all(24),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.cloud_off, size: 36),
            const SizedBox(height: 12),
            Text(
              'Roster is $daysStale days stale — sync before marking',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            const Text(
              'Per the offline policy, attendance can\'t be recorded against a Roster older than 7 days. '
              'Reconnect and pull a fresh snapshot to continue.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              icon: const Icon(Icons.sync),
              label: const Text('Sync now'),
              onPressed: onSyncNow,
            ),
          ]),
        ),
      ),
    );
  }
}

class _PresenceIcon extends StatelessWidget {
  const _PresenceIcon({required this.present});
  final bool? present;

  @override
  Widget build(BuildContext context) {
    if (present == null) {
      return const Icon(Icons.radio_button_unchecked, color: Colors.grey);
    }
    return Icon(
      present! ? Icons.check_circle : Icons.cancel,
      color: present! ? Colors.green : Colors.red,
    );
  }
}
