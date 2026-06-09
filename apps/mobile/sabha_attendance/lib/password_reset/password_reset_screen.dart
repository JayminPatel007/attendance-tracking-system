import 'package:flutter/material.dart';

import 'password_reset_controller.dart';
import 'who_appointed_me_screen.dart';

/// Self-service password-reset screen (ADR-0004, Slice 18B), reached from the
/// login screen without auth: enter username → receive an OTP on the registered
/// mobile → enter it → set a new password → done. All async I/O is the
/// controller's job. A "Who appointed me?" link covers the lost-mobile case.
class PasswordResetScreen extends StatefulWidget {
  const PasswordResetScreen({super.key, required this.controller, this.onDone});

  final PasswordResetController controller;

  /// Optional callback once the password has been reset (e.g. pop the route).
  final VoidCallback? onDone;

  @override
  State<PasswordResetScreen> createState() => _PasswordResetScreenState();
}

class _PasswordResetScreenState extends State<PasswordResetScreen> {
  final _username = TextEditingController();
  final _otp = TextEditingController();
  final _password = TextEditingController();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onChange);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onChange);
    _username.dispose();
    _otp.dispose();
    _password.dispose();
    super.dispose();
  }

  void _onChange() {
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.controller.state;
    return switch (state.view) {
      PasswordResetView.username => _usernameStep(state),
      PasswordResetView.otp => _otpStep(state),
      PasswordResetView.password => _passwordStep(state),
      PasswordResetView.done => _doneStep(),
    };
  }

  Widget _usernameStep(PasswordResetViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Enter your username and we\'ll send a one-time code to your registered mobile.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 12),
        TextField(
          key: const Key('reset-username-field'),
          controller: _username,
          autofillHints: const [AutofillHints.username],
          decoration: const InputDecoration(labelText: 'Username'),
          onChanged: (_) => setState(() {}),
          onSubmitted: (v) => _runRequest(v),
        ),
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('reset-send-button'),
          onPressed: state.busy || _username.text.trim().isEmpty
              ? null
              : () => _runRequest(_username.text),
          child: const Text('Send code'),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 12),
          _Banner(message: state.error!),
        ],
        const SizedBox(height: 8),
        TextButton(
          key: const Key('reset-who-appointed-link'),
          onPressed: _openWhoAppointedMe,
          child: const Text('Who appointed me?'),
        ),
      ],
    );
  }

  Widget _otpStep(PasswordResetViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Icon(Icons.lock_outline, size: 40),
        const SizedBox(height: 12),
        const Text(
          'Enter the 6-digit code sent to your registered mobile. It expires in 5 minutes.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 16),
        TextField(
          key: const Key('reset-otp-field'),
          controller: _otp,
          keyboardType: TextInputType.number,
          maxLength: 6,
          autofillHints: const [AutofillHints.oneTimeCode],
          decoration: const InputDecoration(labelText: 'OTP', counterText: ''),
          onChanged: (_) => setState(() {}),
        ),
        if (state.otpError != null) ...[
          const SizedBox(height: 8),
          _Banner(message: state.otpError!),
        ],
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('reset-verify-button'),
          onPressed: state.busy || _otp.text.trim().length < 6
              ? null
              : () => widget.controller.verify(_otp.text.trim()),
          child: const Text('Verify code'),
        ),
      ],
    );
  }

  Widget _passwordStep(PasswordResetViewState state) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text('Choose a new password.', style: TextStyle(color: Colors.grey)),
        const SizedBox(height: 16),
        TextField(
          key: const Key('reset-password-field'),
          controller: _password,
          obscureText: true,
          autofillHints: const [AutofillHints.newPassword],
          decoration: const InputDecoration(labelText: 'New password'),
          onChanged: (_) => setState(() {}),
        ),
        if (state.error != null) ...[
          const SizedBox(height: 8),
          _Banner(message: state.error!),
        ],
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('reset-complete-button'),
          onPressed: state.busy || _password.text.isEmpty
              ? null
              : () => widget.controller.complete(_password.text),
          child: const Text('Set new password'),
        ),
      ],
    );
  }

  Widget _doneStep() {
    return Center(
      key: const Key('reset-done'),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 48),
        const SizedBox(height: 12),
        const Text('Your password has been reset.',
            style: TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 16),
        TextButton(
          key: const Key('reset-done-button'),
          onPressed: widget.onDone ?? () => Navigator.of(context).maybePop(),
          child: const Text('Back to sign in'),
        ),
      ]),
    );
  }

  void _runRequest(String value) {
    final username = value.trim();
    if (username.isEmpty) return;
    widget.controller.requestOtp(username);
  }

  void _openWhoAppointedMe() {
    Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Who appointed me?')),
        body: WhoAppointedMeScreen(api: widget.controller.api),
      ),
    ));
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.message});

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
