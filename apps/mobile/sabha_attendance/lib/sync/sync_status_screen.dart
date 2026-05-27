import 'package:flutter/material.dart';

import '../roster/roster_controller.dart';
import 'pending_marking.dart';

/// Shows the local pending-marking queue, the last successful sync time, and
/// a manual "Sync now" trigger (ADR-0007). Backs onto the [RosterController]
/// so widget tests can drive it deterministically.
class SyncStatusScreen extends StatefulWidget {
  const SyncStatusScreen({super.key, required this.controller, this.now});

  final RosterController controller;
  final DateTime Function()? now;

  @override
  State<SyncStatusScreen> createState() => _SyncStatusScreenState();
}

class _SyncStatusScreenState extends State<SyncStatusScreen> {
  List<PendingMarking> _pending = const [];
  DateTime? _lastSyncedAt;
  bool _syncing = false;
  String? _lastError;

  DateTime _now() => widget.now?.call() ?? DateTime.now().toUtc();

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    final pending = await widget.controller.store.pendingMarkings();
    final lastSync = await widget.controller.lastSyncedAt();
    if (!mounted) return;
    setState(() {
      _pending = pending;
      _lastSyncedAt = lastSync;
    });
  }

  Future<void> _syncNow() async {
    setState(() {
      _syncing = true;
      _lastError = null;
    });
    try {
      final outcome = await widget.controller.syncNow();
      if (outcome.staleRoster) {
        _lastError = 'Roster is stale on the server — refresh it and retry.';
      }
    } catch (e) {
      _lastError = 'Sync failed: $e';
    } finally {
      await _refresh();
      if (mounted) setState(() => _syncing = false);
    }
  }

  String _formatRelative(DateTime? at) {
    if (at == null) return 'never';
    final diff = _now().difference(at);
    if (diff.inSeconds < 60) return '${diff.inSeconds}s ago';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${diff.inDays}d ago';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sync & offline')),
      body: ListView(children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Pending', style: TextStyle(fontWeight: FontWeight.w600)),
                Text('${_pending.length} queued for sync'),
              ]),
            ),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Last sync', style: TextStyle(fontWeight: FontWeight.w600)),
                Text(_formatRelative(_lastSyncedAt)),
              ]),
            ),
          ]),
        ),
        if (_lastError != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text(_lastError!, style: const TextStyle(color: Colors.red)),
          ),
        const Divider(),
        if (_pending.isEmpty)
          const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: Text('No pending markings. All up to date.')),
          )
        else
          ..._pending.map((m) => ListTile(
                leading: Icon(m.present ? Icons.check_circle : Icons.cancel,
                    color: m.present ? Colors.green : Colors.red),
                title: Text('Person ${m.personId} → ${m.present ? "Present" : "Absent"}'),
                subtitle: Text('Marked ${_formatRelative(m.clientMarkedAt)}'),
              )),
        const Divider(),
        Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton.icon(
            icon: _syncing
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.sync),
            label: Text(_syncing ? 'Syncing…' : 'Sync now'),
            onPressed: _syncing ? null : _syncNow,
          ),
        ),
      ]),
    );
  }
}
