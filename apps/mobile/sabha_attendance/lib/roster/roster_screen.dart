import 'package:flutter/material.dart';

import 'roster_api.dart';

class RosterScreen extends StatefulWidget {
  const RosterScreen({super.key, required this.api, this.onSignOut});

  final RosterApi api;
  final VoidCallback? onSignOut;

  @override
  State<RosterScreen> createState() => _RosterScreenState();
}

class _RosterScreenState extends State<RosterScreen> {
  late Future<CurrentRoster> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.api.fetch();
  }

  void _refresh() {
    setState(() {
      _future = widget.api.fetch();
    });
  }

  Future<void> _toggle(CurrentRoster current, RosterEntry entry) async {
    final next = !(entry.present ?? false);
    try {
      await widget.api.mark(
        occurrenceId: current.occurrence.id,
        personId: entry.personId,
        present: next,
      );
      _refresh();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Mark failed: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<CurrentRoster>(
      future: _future,
      builder: (context, snap) {
        if (snap.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Could not load roster: ${snap.error}'),
                  const SizedBox(height: 16),
                  FilledButton(onPressed: _refresh, child: const Text('Retry')),
                  if (widget.onSignOut != null) ...[
                    const SizedBox(height: 8),
                    TextButton(onPressed: widget.onSignOut, child: const Text('Sign out')),
                  ],
                ],
              ),
            ),
          );
        }
        final data = snap.data!;
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                '${data.occurrence.date} · ${data.occurrence.state.replaceAll('_', ' ').toLowerCase()}',
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
              ),
            ),
            Expanded(
              child: ListView.separated(
                itemCount: data.roster.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, i) {
                  final e = data.roster[i];
                  return ListTile(
                    title: Text(e.fullName),
                    trailing: _PresenceIcon(present: e.present),
                    onTap: () => _toggle(data, e),
                  );
                },
              ),
            ),
            if (widget.onSignOut != null)
              Padding(
                padding: const EdgeInsets.all(8),
                child: TextButton(onPressed: widget.onSignOut, child: const Text('Sign out')),
              ),
          ],
        );
      },
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
