import 'package:flutter/material.dart';

import 'occurrence_control_api.dart';
import 'occurrence_control_controller.dart';

/// Sanchalak-only Occurrence-control screen (Slice 5, ADR-0001). Surfaces the
/// three Sabha-shaping actions — reschedule, venue override, cancel — as
/// collapsible cards, plus a Revert affordance once an Occurrence is Cancelled.
/// All actions are online-only (ADR-0007); failures (incl. a Sah-Sanchalak's
/// 403) surface as an inline banner. State is owned by
/// [OccurrenceControlController]; this widget only renders it and dispatches.
class OccurrenceControlScreen extends StatefulWidget {
  const OccurrenceControlScreen({super.key, required this.controller});

  final OccurrenceControlController controller;

  @override
  State<OccurrenceControlScreen> createState() => _OccurrenceControlScreenState();
}

class _OccurrenceControlScreenState extends State<OccurrenceControlScreen> {
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

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    if (state.loading) {
      return const Center(child: CircularProgressIndicator());
    }
    final occ = state.occurrence;
    if (occ == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.event_busy, size: 36, color: Colors.grey),
            const SizedBox(height: 12),
            Text(state.error ?? 'No Sabha to manage right now.', textAlign: TextAlign.center),
          ]),
        ),
      );
    }

    // Affordances mirror the domain's from-state guards (ADR-0001): reschedule
    // and cancel are only valid on a Scheduled Occurrence; venue-override is
    // valid on Scheduled or Rescheduled; revert only on Cancelled. Showing an
    // action that the backend would reject with 422 is misleading, so each card
    // appears only where its transition is actually permitted.
    final isScheduled = occ.state == 'SCHEDULED';
    final isCancelled = occ.state == 'CANCELLED';
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _Header(occurrence: occ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _ErrorBanner(message: state.error!),
        ],
        const SizedBox(height: 12),
        if (isCancelled)
          _CancelledCard(busy: state.busy, onRevert: widget.controller.revert)
        else ...[
          if (isScheduled)
            _RescheduleCard(
              busy: state.busy,
              onApply: (date, start, end) =>
                  widget.controller.reschedule(date: date, startTime: start, endTime: end),
            ),
          _VenueCard(
            busy: state.busy,
            current: occ.venueOverride,
            onApply: widget.controller.overrideVenue,
          ),
          if (isScheduled) _CancelCard(busy: state.busy, onCancel: widget.controller.cancel),
        ],
      ],
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.occurrence});
  final ShapeableOccurrence occurrence;

  @override
  Widget build(BuildContext context) {
    final reschedule = occurrence.rescheduledDate != null
        ? 'Rescheduled to ${occurrence.rescheduledDate}'
            '${occurrence.rescheduledStartTime != null ? ' · ${occurrence.rescheduledStartTime}–${occurrence.rescheduledEndTime}' : ''}'
        : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(children: [
          Expanded(
            child: Text(occurrence.date,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          ),
          _StateChip(state: occurrence.state),
        ]),
        if (reschedule != null) ...[
          const SizedBox(height: 6),
          Text(reschedule, style: const TextStyle(color: Colors.indigo)),
        ],
        if (occurrence.venueOverride != null) ...[
          const SizedBox(height: 6),
          Row(children: [
            const Icon(Icons.place, size: 16, color: Colors.grey),
            const SizedBox(width: 4),
            Text('Venue: ${occurrence.venueOverride}'),
          ]),
        ],
      ],
    );
  }
}

class _StateChip extends StatelessWidget {
  const _StateChip({required this.state});
  final String state;

  @override
  Widget build(BuildContext context) {
    final label = state.replaceAll('_', ' ').toLowerCase();
    final color = switch (state) {
      'CANCELLED' => Colors.red,
      'RESCHEDULED' => Colors.indigo,
      _ => Colors.green,
    };
    return Chip(
      label: Text(label),
      backgroundColor: color.withOpacity(0.12),
      labelStyle: TextStyle(color: color, fontWeight: FontWeight.w600),
      side: BorderSide(color: color.withOpacity(0.3)),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.red.withOpacity(0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.red.withOpacity(0.3)),
      ),
      child: Row(children: [
        const Icon(Icons.error_outline, color: Colors.red, size: 20),
        const SizedBox(width: 8),
        Expanded(child: Text(message)),
      ]),
    );
  }
}

class _RescheduleCard extends StatefulWidget {
  const _RescheduleCard({required this.busy, required this.onApply});
  final bool busy;
  final void Function(String date, String startTime, String endTime) onApply;

  @override
  State<_RescheduleCard> createState() => _RescheduleCardState();
}

class _RescheduleCardState extends State<_RescheduleCard> {
  final _date = TextEditingController();
  final _start = TextEditingController();
  final _end = TextEditingController();

  @override
  void dispose() {
    _date.dispose();
    _start.dispose();
    _end.dispose();
    super.dispose();
  }

  bool get _ready => _date.text.isNotEmpty && _start.text.isNotEmpty && _end.text.isNotEmpty;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ExpansionTile(
        leading: const Icon(Icons.event_repeat),
        title: const Text('Reschedule'),
        subtitle: const Text('New date/time — standing schedule untouched'),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          TextField(
            key: const Key('reschedule-date-field'),
            controller: _date,
            decoration: const InputDecoration(labelText: 'Date', hintText: 'YYYY-MM-DD'),
            onChanged: (_) => setState(() {}),
          ),
          Row(children: [
            Expanded(
              child: TextField(
                key: const Key('reschedule-start-field'),
                controller: _start,
                decoration: const InputDecoration(labelText: 'Start', hintText: 'HH:mm'),
                onChanged: (_) => setState(() {}),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextField(
                key: const Key('reschedule-end-field'),
                controller: _end,
                decoration: const InputDecoration(labelText: 'End', hintText: 'HH:mm'),
                onChanged: (_) => setState(() {}),
              ),
            ),
          ]),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(
              key: const Key('apply-reschedule-button'),
              onPressed: widget.busy || !_ready
                  ? null
                  : () => widget.onApply(_date.text, _start.text, _end.text),
              child: const Text('Apply reschedule'),
            ),
          ),
        ],
      ),
    );
  }
}

class _VenueCard extends StatefulWidget {
  const _VenueCard({required this.busy, required this.current, required this.onApply});
  final bool busy;
  final String? current;
  final void Function(String venue) onApply;

  @override
  State<_VenueCard> createState() => _VenueCardState();
}

class _VenueCardState extends State<_VenueCard> {
  late final TextEditingController _venue = TextEditingController(text: widget.current ?? '');

  @override
  void dispose() {
    _venue.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ExpansionTile(
        leading: const Icon(Icons.place_outlined),
        title: const Text('Venue override'),
        subtitle: const Text('This Occurrence only — standing venue untouched'),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          TextField(
            key: const Key('venue-field'),
            controller: _venue,
            decoration: const InputDecoration(labelText: 'Venue'),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(
              key: const Key('apply-venue-button'),
              onPressed: widget.busy || _venue.text.trim().isEmpty
                  ? null
                  : () => widget.onApply(_venue.text.trim()),
              child: const Text('Apply venue'),
            ),
          ),
        ],
      ),
    );
  }
}

class _CancelCard extends StatefulWidget {
  const _CancelCard({required this.busy, required this.onCancel});
  final bool busy;
  final void Function(String reason) onCancel;

  @override
  State<_CancelCard> createState() => _CancelCardState();
}

class _CancelCardState extends State<_CancelCard> {
  final _reason = TextEditingController();

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ExpansionTile(
        leading: const Icon(Icons.cancel_outlined, color: Colors.red),
        title: const Text('Cancel Sabha'),
        subtitle: const Text('Reason required · reversible within 24h'),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          TextField(
            key: const Key('cancel-reason-field'),
            controller: _reason,
            decoration: const InputDecoration(labelText: 'Reason', hintText: 'Why is this Sabha cancelled?'),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(
              key: const Key('confirm-cancel-button'),
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              onPressed: widget.busy || _reason.text.trim().isEmpty
                  ? null
                  : () => widget.onCancel(_reason.text.trim()),
              child: const Text('Cancel this Sabha'),
            ),
          ),
        ],
      ),
    );
  }
}

class _CancelledCard extends StatelessWidget {
  const _CancelledCard({required this.busy, required this.onRevert});
  final bool busy;
  final Future<void> Function() onRevert;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Colors.red.withOpacity(0.04),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('This Sabha is cancelled.',
              style: TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          const Text(
            'You can revert it back to Scheduled up to 24h after the scheduled end. '
            'After that the cancellation is locked in.',
          ),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton.icon(
              key: const Key('revert-button'),
              onPressed: busy ? null : onRevert,
              icon: const Icon(Icons.undo),
              label: const Text('Revert'),
            ),
          ),
        ]),
      ),
    );
  }
}
