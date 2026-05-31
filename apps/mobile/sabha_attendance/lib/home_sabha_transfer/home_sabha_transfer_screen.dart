import 'package:flutter/material.dart';

import 'home_sabha_transfer_controller.dart';

/// The `mobile-home-sabha-transfer` flow (Slice 8, ADR-0002): find the Person by
/// mobile → confirm the direction (this Person → your Sabha) → send the OTP to
/// their mobile → enter it → done. Always Person-initiated; the OTP is the
/// Person's explicit consent. All async I/O is the controller's job.
class HomeSabhaTransferScreen extends StatefulWidget {
  const HomeSabhaTransferScreen({
    super.key,
    required this.controller,
    required this.destinationLabel,
    this.onDone,
  });

  final HomeSabhaTransferController controller;

  /// Human label for the destination Sabha (the Sanchalak's current Sabha).
  final String destinationLabel;

  /// Optional callback once the transfer commits (e.g. pop the route).
  final VoidCallback? onDone;

  @override
  State<HomeSabhaTransferScreen> createState() => _HomeSabhaTransferScreenState();
}

class _HomeSabhaTransferScreenState extends State<HomeSabhaTransferScreen> {
  final _mobile = TextEditingController();
  final _otp = TextEditingController();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    _mobile.dispose();
    _otp.dispose();
    super.dispose();
  }

  void _onChange() {
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    return switch (state.view) {
      HomeSabhaTransferView.find => _findStep(state),
      HomeSabhaTransferView.confirm => _confirmStep(state),
      HomeSabhaTransferView.otp => _otpStep(state),
      HomeSabhaTransferView.done => _doneStep(),
    };
  }

  Widget _findStep(HomeSabhaTransferViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Find the Person standing in front of you by their mobile. They must be '
          'reachable on it to receive the consent OTP.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 12),
        TextField(
          key: const Key('hsat-mobile-field'),
          controller: _mobile,
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(
            labelText: 'Mobile number',
            hintText: 'e.g. +9198201…',
          ),
          onChanged: (_) => setState(() {}),
          onSubmitted: _runFind,
        ),
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('hsat-find-button'),
          onPressed:
              state.busy || _mobile.text.trim().isEmpty ? null : () => _runFind(_mobile.text),
          child: const Text('Find Person'),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _Banner(icon: Icons.error_outline, color: Colors.red, message: state.error!),
        ],
      ],
    );
  }

  Widget _confirmStep(HomeSabhaTransferViewState state) {
    final person = state.person!;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(person.fullName, style: const TextStyle(fontWeight: FontWeight.w600)),
            subtitle: person.mobile == null ? null : Text(person.mobile!),
          ),
        ),
        const SizedBox(height: 16),
        const Icon(Icons.south, size: 28),
        const SizedBox(height: 8),
        Card(
          color: Theme.of(context).colorScheme.primaryContainer,
          child: ListTile(
            leading: const Icon(Icons.home_work_outlined),
            title: Text(widget.destinationLabel,
                style: const TextStyle(fontWeight: FontWeight.w600)),
            subtitle: const Text('New Home Sabha after consent'),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Sending the OTP to ${person.mobile ?? 'their mobile'} asks ${person.fullName} '
          'to confirm this Home Sabha change. They asked you for this.',
          style: const TextStyle(color: Colors.grey),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _Banner(icon: Icons.error_outline, color: Colors.red, message: state.error!),
        ],
        const SizedBox(height: 20),
        FilledButton.icon(
          key: const Key('hsat-send-otp-button'),
          icon: const Icon(Icons.sms_outlined),
          label: const Text('Send OTP'),
          onPressed: state.busy ? null : widget.controller.sendOtp,
        ),
        const SizedBox(height: 8),
        TextButton(
          key: const Key('hsat-confirm-back-button'),
          onPressed: state.busy ? null : widget.controller.backToFind,
          child: const Text('Different Person'),
        ),
      ],
    );
  }

  Widget _otpStep(HomeSabhaTransferViewState state) {
    final person = state.person!;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Icon(Icons.lock_outline, size: 40),
        const SizedBox(height: 12),
        Text(
          'Enter the 6-digit code sent to ${person.mobile ?? 'their mobile'}. '
          'Ask ${person.fullName.split(' ').first} to read it out.',
          style: const TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 16),
        TextField(
          key: const Key('hsat-otp-field'),
          controller: _otp,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: const InputDecoration(
            labelText: 'OTP',
            counterText: '',
          ),
          onChanged: (_) => setState(() {}),
        ),
        if (state.otpError != null) ...[
          const SizedBox(height: 8),
          _Banner(icon: Icons.error_outline, color: Colors.red, message: state.otpError!),
        ],
        const SizedBox(height: 8),
        TextButton(
          key: const Key('hsat-resend-button'),
          onPressed: state.busy ? null : widget.controller.sendOtp,
          child: const Text('Resend OTP'),
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          key: const Key('hsat-verify-button'),
          icon: const Icon(Icons.verified_outlined),
          label: const Text('Verify & Transfer'),
          onPressed: state.busy || _otp.text.trim().length < 6
              ? null
              : () => widget.controller.confirm(_otp.text.trim()),
        ),
        const SizedBox(height: 8),
        TextButton(
          key: const Key('hsat-otp-back-button'),
          onPressed: state.busy ? null : widget.controller.backToConfirm,
          child: const Text('Back'),
        ),
      ],
    );
  }

  Widget _doneStep() {
    return Center(
      key: const Key('hsat-done'),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 48),
        const SizedBox(height: 12),
        const Text('Home Sabha transferred.', style: TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 16),
        TextButton(
          key: const Key('hsat-done-button'),
          onPressed: widget.onDone,
          child: const Text('Done'),
        ),
      ]),
    );
  }

  void _runFind(String value) {
    final mobile = value.trim();
    if (mobile.isEmpty) return;
    widget.controller.findByMobile(mobile);
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.icon, required this.color, required this.message});

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
