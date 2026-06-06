import 'package:flutter/material.dart';

import 'selection_controller.dart';

/// Dedicated BSS/YSS nominate screen (Slice 16, ADR-0006). Lists the Roster
/// People the Regular Sanchalak may nominate for the selective track; each row
/// confirms before posting and then reflects its own outcome (nominated, or the
/// backend's rejection message). The selective Sabha is derived server-side, so
/// the Sanchalak only chooses the Person. All async I/O is the controller's job.
class SelectionScreen extends StatefulWidget {
  const SelectionScreen({super.key, required this.controller});

  final SelectionController controller;

  @override
  State<SelectionScreen> createState() => _SelectionScreenState();
}

class _SelectionScreenState extends State<SelectionScreen> {
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

  Future<void> _confirmAndNominate(Nominee person) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nominate for BSS / YSS'),
        content: Text(
          'Nominate ${person.fullName} for the selective track? '
          'The demographic Nirdeshak decides whether to approve.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            key: const Key('nominate-confirm'),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Nominate'),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await widget.controller.nominate(person.personId);
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    final people = controller.people;
    if (people.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('No People on the Roster to nominate.'),
        ),
      );
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Nominate a Person from your Roster for the selective BSS / YSS track. '
          'The Nirdeshak for your demographic approves or rejects each.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 12),
        for (final person in people) _NomineeRow(
          person: person,
          outcome: controller.outcomeFor(person.personId),
          error: controller.errorFor(person.personId),
          busy: controller.busyPersonId == person.personId,
          onNominate: () => _confirmAndNominate(person),
        ),
      ],
    );
  }
}

class _NomineeRow extends StatelessWidget {
  const _NomineeRow({
    required this.person,
    required this.outcome,
    required this.error,
    required this.busy,
    required this.onNominate,
  });

  final Nominee person;
  final NominationOutcome outcome;
  final String? error;
  final bool busy;
  final VoidCallback onNominate;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListTile(
            title: Text(person.fullName),
            trailing: _trailing(),
          ),
          if (outcome == NominationOutcome.failed && error != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Row(children: [
                const Icon(Icons.error_outline, color: Colors.red, size: 18),
                const SizedBox(width: 8),
                Expanded(child: Text(error!, style: const TextStyle(color: Colors.red))),
              ]),
            ),
        ],
      ),
    );
  }

  Widget _trailing() {
    if (busy) {
      return const SizedBox(
        width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2));
    }
    if (outcome == NominationOutcome.nominated) {
      return const Row(mainAxisSize: MainAxisSize.min, children: [
        Icon(Icons.check_circle, color: Colors.green, size: 18),
        SizedBox(width: 6),
        Text('Nominated', style: TextStyle(color: Colors.green)),
      ]);
    }
    return FilledButton(
      key: Key('nominate-${person.personId}'),
      onPressed: onNominate,
      child: const Text('Nominate'),
    );
  }
}
