import 'package:flutter/material.dart';

import 'walk_in_controller.dart';

/// The `mobile-walk-in` flow (Slice 7, issue #8): search the Directory for a
/// visitor by name or mobile, confirm their current Home Sabha, and record the
/// Walk-in. Online search hits the full Directory; offline it falls back to the
/// cached Roster and nudges the Sanchalak to "search the wider Directory when
/// online" on no match (ADR-0007). All async I/O is the controller's job.
class WalkInScreen extends StatefulWidget {
  const WalkInScreen({super.key, required this.controller});

  final WalkInController controller;

  @override
  State<WalkInScreen> createState() => _WalkInScreenState();
}

class _WalkInScreenState extends State<WalkInScreen> {
  final _query = TextEditingController();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    _query.dispose();
    super.dispose();
  }

  void _onChange() {
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    return switch (state.view) {
      WalkInView.search => _searchStep(state),
      WalkInView.confirm => _confirmStep(state),
      WalkInView.recorded => _recordedStep(),
    };
  }

  Widget _searchStep(WalkInViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          key: const Key('walk-in-search-field'),
          controller: _query,
          decoration: const InputDecoration(
            labelText: 'Name or mobile',
            hintText: 'Search the Directory for the visitor',
          ),
          onChanged: (_) => setState(() {}),
          onSubmitted: (v) => _runSearch(v),
        ),
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('walk-in-search-button'),
          onPressed: state.busy || _query.text.trim().isEmpty ? null : () => _runSearch(_query.text),
          child: const Text('Search'),
        ),
        if (state.offline) ...[
          const SizedBox(height: 12),
          const _Banner(
            key: Key('walk-in-offline-banner'),
            icon: Icons.cloud_off,
            color: Colors.orange,
            message: 'Offline — searching this Sabha\'s cached Roster only.',
          ),
        ],
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _Banner(icon: Icons.error_outline, color: Colors.red, message: state.error!),
        ],
        if (state.showWiderDirectoryHint) ...[
          const SizedBox(height: 12),
          const _Banner(
            key: Key('walk-in-wider-directory-hint'),
            icon: Icons.travel_explore,
            color: Colors.blue,
            message: 'No match in the cached Roster. Search the wider Directory when you\'re back online, '
                'or capture details on paper and add the Person after the Sabha.',
          ),
        ],
        const SizedBox(height: 8),
        ...state.results.map((c) => Card(
              child: ListTile(
                leading: const Icon(Icons.directions_walk),
                title: Text(c.fullName),
                subtitle: c.homeSabhas.isEmpty ? null : Text('Home Sabha · ${c.homeSabhasLabel}'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => widget.controller.select(c),
              ),
            )),
      ],
    );
  }

  Widget _confirmStep(WalkInViewState state) {
    final person = state.selected!;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(person.fullName, style: const TextStyle(fontWeight: FontWeight.w600)),
            subtitle: Text(person.homeSabhas.isEmpty
                ? 'A visitor to this Sabha'
                : 'Home Sabha · ${person.homeSabhasLabel} (away here)'),
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Recording a Walk-in marks them present here without changing their Home Sabha.',
          style: TextStyle(color: Colors.grey),
        ),
        if (person.homeSabhas.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(person.homeSabhasLabel, style: const TextStyle(fontWeight: FontWeight.w500)),
        ],
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _Banner(icon: Icons.error_outline, color: Colors.red, message: state.error!),
        ],
        const SizedBox(height: 20),
        FilledButton.icon(
          key: const Key('record-walk-in-button'),
          icon: const Icon(Icons.directions_walk),
          label: const Text('Record walk-in'),
          onPressed: state.busy ? null : widget.controller.record,
        ),
        const SizedBox(height: 8),
        TextButton(
          key: const Key('walk-in-back-button'),
          onPressed: state.busy ? null : widget.controller.backToSearch,
          child: const Text('Back to search'),
        ),
      ],
    );
  }

  Widget _recordedStep() {
    return Center(
      key: const Key('walk-in-recorded'),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 48),
        const SizedBox(height: 12),
        const Text('Walk-in recorded.', style: TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 16),
        TextButton(
          key: const Key('walk-in-record-another-button'),
          onPressed: () {
            _query.clear();
            widget.controller.backToSearch();
          },
          child: const Text('Record another'),
        ),
      ]),
    );
  }

  void _runSearch(String value) {
    final q = value.trim();
    if (q.isEmpty) return;
    widget.controller.search(q);
  }
}

class _Banner extends StatelessWidget {
  const _Banner({super.key, required this.icon, required this.color, required this.message});

  final IconData icon;
  final Color color;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(children: [
        Icon(icon, color: color, size: 20),
        const SizedBox(width: 8),
        Expanded(child: Text(message)),
      ]),
    );
  }
}
