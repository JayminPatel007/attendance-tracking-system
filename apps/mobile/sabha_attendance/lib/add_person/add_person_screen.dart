import 'package:flutter/material.dart';

import 'add_person_api.dart';
import 'add_person_controller.dart';

/// Two-step add-person flow (Slice 6, ADR-0013), mirroring the
/// `mobile-add-person` prototype: mobile entry first (the de-dup key), then —
/// on no match — name / gender / DOB against a fixed Home Sabha (the
/// Sanchalak's current Sabha). An exact mobile match forces a redirect to the
/// existing Person; a close name surfaces soft-warn candidates with a
/// create-new-anyway override. Online-only (ADR-0007).
class AddPersonScreen extends StatefulWidget {
  const AddPersonScreen({
    super.key,
    required this.controller,
    required this.homeSabhaLabel,
    required this.onSelectExisting,
    required this.onCreated,
  });

  final AddPersonController controller;

  /// Human label for the Home Sabha a new Person is registered to.
  final String homeSabhaLabel;

  /// The adder chose the existing Person surfaced by the mobile hard block.
  final void Function(String personId) onSelectExisting;

  /// A Person was created; carries the new Person id.
  final void Function(String personId) onCreated;

  @override
  State<AddPersonScreen> createState() => _AddPersonScreenState();
}

class _AddPersonScreenState extends State<AddPersonScreen> {
  final _mobile = TextEditingController();
  bool _createdHandled = false;

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    _mobile.dispose();
    super.dispose();
  }

  void _onChange() {
    if (!mounted) return;
    final state = widget.controller.state;
    if (state.view == AddPersonView.created && !_createdHandled && state.createdPersonId != null) {
      _createdHandled = true;
      widget.onCreated(state.createdPersonId!);
    }
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    return switch (state.view) {
      AddPersonView.mobile => _mobileStep(state),
      AddPersonView.profile => _profileStep(state),
      AddPersonView.details => _DetailsForm(
          controller: widget.controller,
          state: state,
          homeSabhaLabel: widget.homeSabhaLabel,
        ),
      AddPersonView.created => _createdStep(state),
    };
  }

  Widget _mobileStep(AddPersonViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const _StepHeader(step: 1, label: 'Mobile number'),
        const SizedBox(height: 16),
        TextField(
          key: const Key('add-person-mobile-field'),
          controller: _mobile,
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(
            labelText: 'Mobile number',
            hintText: 'Checked against the Directory first',
          ),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 8),
        const Text(
          'If this number is already in the Directory, we open that Person instead — '
          'preventing duplicates (ADR-0013).',
          style: TextStyle(color: Colors.grey),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _ErrorBanner(message: state.error!),
        ],
        const SizedBox(height: 20),
        FilledButton(
          key: const Key('check-mobile-button'),
          onPressed: state.busy || _mobile.text.trim().isEmpty
              ? null
              : () => widget.controller.checkMobile(_mobile.text.trim()),
          child: const Text('Continue'),
        ),
      ],
    );
  }

  Widget _profileStep(AddPersonViewState state) {
    final person = state.existingPerson!;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.amber.withOpacity(0.12),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.amber.withOpacity(0.4)),
          ),
          child: const Text('This mobile is already registered. Continuing means using this Person.'),
        ),
        const SizedBox(height: 16),
        Card(
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(person.fullName, style: const TextStyle(fontWeight: FontWeight.w600)),
            subtitle: Text([
              if (person.mobile != null) person.mobile!,
              person.gender.toLowerCase(),
              if (person.dateOfBirth != null) 'DOB ${person.dateOfBirth}',
            ].join(' · ')),
          ),
        ),
        const SizedBox(height: 20),
        FilledButton(
          key: const Key('use-existing-button'),
          onPressed: () => widget.onSelectExisting(person.id),
          child: const Text('Use this Person'),
        ),
        const SizedBox(height: 8),
        TextButton(
          key: const Key('different-number-button'),
          onPressed: () {
            _mobile.clear();
            _createdHandled = false;
            widget.controller.backToMobile();
          },
          child: const Text('Different number'),
        ),
      ],
    );
  }

  Widget _createdStep(AddPersonViewState state) {
    return Center(
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 48),
        const SizedBox(height: 12),
        const Text('Added to the Directory.', style: TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 16),
        TextButton(
          key: const Key('add-another-button'),
          onPressed: () {
            _mobile.clear();
            _createdHandled = false;
            widget.controller.backToMobile();
          },
          child: const Text('Add another'),
        ),
      ]),
    );
  }
}

class _DetailsForm extends StatefulWidget {
  const _DetailsForm({required this.controller, required this.state, required this.homeSabhaLabel});

  final AddPersonController controller;
  final AddPersonViewState state;
  final String homeSabhaLabel;

  @override
  State<_DetailsForm> createState() => _DetailsFormState();
}

class _DetailsFormState extends State<_DetailsForm> {
  final _name = TextEditingController();
  final _dob = TextEditingController();
  String? _gender;

  @override
  void dispose() {
    _name.dispose();
    _dob.dispose();
    super.dispose();
  }

  bool get _ready => _name.text.trim().isNotEmpty && _gender != null;

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const _StepHeader(step: 2, label: 'Details'),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.green.withOpacity(0.10),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text('Mobile ${widget.controller.state.mobile} is new to the Directory.'),
        ),
        const SizedBox(height: 16),
        TextField(
          key: const Key('add-person-name-field'),
          controller: _name,
          decoration: const InputDecoration(labelText: 'Full name *'),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 12),
        const Text('Gender *', style: TextStyle(color: Colors.grey)),
        const SizedBox(height: 4),
        Row(children: [
          ChoiceChip(
            key: const Key('gender-male'),
            label: const Text('Male'),
            selected: _gender == 'MALE',
            onSelected: (_) => setState(() => _gender = 'MALE'),
          ),
          const SizedBox(width: 8),
          ChoiceChip(
            key: const Key('gender-female'),
            label: const Text('Female'),
            selected: _gender == 'FEMALE',
            onSelected: (_) => setState(() => _gender = 'FEMALE'),
          ),
        ]),
        const SizedBox(height: 12),
        TextField(
          key: const Key('add-person-dob-field'),
          controller: _dob,
          decoration: const InputDecoration(
            labelText: 'Date of birth (optional)',
            hintText: 'YYYY-MM-DD',
          ),
        ),
        const SizedBox(height: 12),
        InputDecorator(
          decoration: const InputDecoration(labelText: 'Home Sabha'),
          child: Text(widget.homeSabhaLabel),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _ErrorBanner(message: state.error!),
        ],
        if (state.requiresOverride) ...[
          const SizedBox(height: 16),
          _CandidatesCard(
            candidates: state.candidates,
            busy: state.busy,
            onCreateNewAnyway: widget.controller.createNewAnyway,
          ),
        ] else ...[
          const SizedBox(height: 20),
          FilledButton(
            key: const Key('add-to-directory-button'),
            onPressed: state.busy || !_ready
                ? null
                : () => widget.controller
                    .submitDetails(fullName: _name.text.trim(), gender: _gender!, dateOfBirth: _dobOrNull()),
            child: const Text('Add to Directory'),
          ),
        ],
      ],
    );
  }

  String? _dobOrNull() => _dob.text.trim().isEmpty ? null : _dob.text.trim();
}

class _CandidatesCard extends StatelessWidget {
  const _CandidatesCard({required this.candidates, required this.busy, required this.onCreateNewAnyway});

  final List<NameCandidate> candidates;
  final bool busy;
  final Future<void> Function() onCreateNewAnyway;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Colors.amber.withOpacity(0.06),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Possible duplicates in this Kshetra',
              style: TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          const Text('Is the Person you\'re adding one of these?',
              style: TextStyle(color: Colors.grey)),
          const SizedBox(height: 8),
          ...candidates.map((c) => ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.person_outline),
                title: Text(c.fullName),
                subtitle: c.homeSabhas.isEmpty ? null : Text(c.homeSabhasLabel),
              )),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: OutlinedButton(
              key: const Key('create-new-anyway-button'),
              onPressed: busy ? null : onCreateNewAnyway,
              child: const Text('None of these — create new'),
            ),
          ),
        ]),
      ),
    );
  }
}

class _StepHeader extends StatelessWidget {
  const _StepHeader({required this.step, required this.label});
  final int step;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      CircleAvatar(radius: 14, child: Text('$step')),
      const SizedBox(width: 8),
      Text('Step $step of 2 · $label', style: const TextStyle(fontWeight: FontWeight.w500)),
    ]);
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
