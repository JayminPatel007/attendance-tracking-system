import 'package:flutter/material.dart';

import 'monthly_occurrence_api.dart';
import 'monthly_occurrence_controller.dart';

/// Monthly-ad-hoc Occurrence screen (Slice 12, ADR-0012). Lists the monthly
/// Sabhas the Sanchalak presides over; a Sabha with no Occurrence this month
/// past the month's midpoint carries a soft compliance nudge. Expanding a Sabha
/// reveals a small form to create this month's Occurrence on a picked
/// date/time/venue (the standing venue is prefilled, editable per Occurrence).
/// State is owned by [MonthlyOccurrenceController]; this widget renders it and
/// dispatches. Online-only (ADR-0007).
class MonthlyOccurrenceScreen extends StatefulWidget {
  const MonthlyOccurrenceScreen({super.key, required this.controller});

  final MonthlyOccurrenceController controller;

  @override
  State<MonthlyOccurrenceScreen> createState() => _MonthlyOccurrenceScreenState();
}

class _MonthlyOccurrenceScreenState extends State<MonthlyOccurrenceScreen> {
  String? _expandedSabhaId;
  final _date = TextEditingController();
  final _start = TextEditingController();
  final _end = TextEditingController();
  final _venue = TextEditingController();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    _date.dispose();
    _start.dispose();
    _end.dispose();
    _venue.dispose();
    super.dispose();
  }

  void _onChange() {
    if (mounted) setState(() {});
  }

  void _expand(MonthlySabha sabha) {
    setState(() {
      _expandedSabhaId = sabha.sabhaId;
      _date.text = '';
      _start.text = '';
      _end.text = '';
      _venue.text = sabha.standingVenue;
    });
  }

  Future<void> _submit(String sabhaId) async {
    await widget.controller.create(
      sabhaId,
      date: _date.text.trim(),
      startTime: _start.text.trim(),
      endTime: _end.text.trim(),
      venue: _venue.text.trim(),
    );
    if (!mounted) return;
    if (widget.controller.state.error == null) {
      setState(() => _expandedSabhaId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    if (state.loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.sabhas.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('No monthly Sabhas to manage.', textAlign: TextAlign.center),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        if (state.error != null)
          Card(
            color: Colors.red.shade50,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Text(state.error!, style: TextStyle(color: Colors.red.shade900)),
            ),
          ),
        for (final sabha in state.sabhas) _sabhaCard(sabha),
      ],
    );
  }

  Widget _sabhaCard(MonthlySabha sabha) {
    final expanded = _expandedSabhaId == sabha.sabhaId;
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(sabha.sabhaKind, style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 2),
            Text(sabha.standingVenue, style: TextStyle(color: Colors.grey.shade700)),
            if (sabha.needsOccurrence)
              Container(
                key: Key('compliance-nudge-${sabha.sabhaId}'),
                margin: const EdgeInsets.only(top: 10),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.amber.shade50,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: Colors.amber.shade300),
                ),
                child: Row(children: [
                  Icon(Icons.warning_amber_rounded, color: Colors.amber.shade800, size: 20),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text('No Occurrence scheduled this month yet — add one before the month ends.'),
                  ),
                ]),
              ),
            const SizedBox(height: 10),
            if (!expanded)
              Align(
                alignment: Alignment.centerLeft,
                child: OutlinedButton.icon(
                  key: Key('add-occurrence-${sabha.sabhaId}'),
                  onPressed: () => _expand(sabha),
                  icon: const Icon(Icons.add),
                  label: const Text('Add this month\'s Occurrence'),
                ),
              )
            else
              _createForm(sabha),
          ],
        ),
      ),
    );
  }

  Widget _createForm(MonthlySabha sabha) {
    final busy = widget.controller.state.busy;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          key: const Key('occ-date-field'),
          controller: _date,
          decoration: const InputDecoration(labelText: 'Date (YYYY-MM-DD)'),
        ),
        Row(children: [
          Expanded(
            child: TextField(
              key: const Key('occ-start-field'),
              controller: _start,
              decoration: const InputDecoration(labelText: 'Start (HH:mm)'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              key: const Key('occ-end-field'),
              controller: _end,
              decoration: const InputDecoration(labelText: 'End (HH:mm)'),
            ),
          ),
        ]),
        TextField(
          key: const Key('occ-venue-field'),
          controller: _venue,
          decoration: const InputDecoration(labelText: 'Venue'),
        ),
        const SizedBox(height: 12),
        Row(children: [
          TextButton(
            onPressed: busy ? null : () => setState(() => _expandedSabhaId = null),
            child: const Text('Cancel'),
          ),
          const Spacer(),
          FilledButton(
            key: const Key('submit-occurrence-button'),
            onPressed: busy ? null : () => _submit(sabha.sabhaId),
            child: const Text('Create Occurrence'),
          ),
        ]),
      ],
    );
  }
}
