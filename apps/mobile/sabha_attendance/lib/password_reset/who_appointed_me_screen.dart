import 'package:flutter/material.dart';

import 'password_reset_api.dart';

/// "Who appointed me?" lookup (ADR-0004, Slice 18B), reached from the reset/login
/// flow without auth. A user who has lost their mobile (and so cannot self-serve
/// a reset) finds, keyed only on their username, the contact details of whoever
/// can reissue their password: their appointer, or a Madhyastha Karyalaya member
/// for Sants. An unknown username is surfaced as a not-found message.
class WhoAppointedMeScreen extends StatefulWidget {
  const WhoAppointedMeScreen({super.key, required this.api});

  final PasswordResetApi api;

  @override
  State<WhoAppointedMeScreen> createState() => _WhoAppointedMeScreenState();
}

class _WhoAppointedMeScreenState extends State<WhoAppointedMeScreen> {
  final _username = TextEditingController();

  bool _busy = false;
  String? _error;
  List<AppointerContact>? _contacts;

  @override
  void dispose() {
    _username.dispose();
    super.dispose();
  }

  Future<void> _lookup() async {
    final username = _username.text.trim();
    if (username.isEmpty || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
      _contacts = null;
    });
    try {
      final contacts = await widget.api.whoAppointedMe(username);
      if (mounted) setState(() => _contacts = contacts);
    } on UnknownUsernameException {
      if (mounted) {
        setState(() =>
            _error = "We couldn't find that username. Check the spelling and try again.");
      }
    } catch (_) {
      if (mounted) setState(() => _error = 'Something went wrong - please try again.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Lost your mobile and can\'t reset your password? Enter your username to find '
          'who can issue you a fresh one.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 12),
        TextField(
          key: const Key('wam-username-field'),
          controller: _username,
          autofillHints: const [AutofillHints.username],
          decoration: const InputDecoration(labelText: 'Username'),
          onChanged: (_) => setState(() {}),
          onSubmitted: (_) => _lookup(),
        ),
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('wam-lookup-button'),
          onPressed: _busy || _username.text.trim().isEmpty ? null : _lookup,
          child: const Text('Find my contact'),
        ),
        if (_error != null) ...[
          const SizedBox(height: 12),
          Text(_error!, style: const TextStyle(color: Colors.red)),
        ],
        if (_contacts != null) ...[
          const SizedBox(height: 16),
          if (_contacts!.isEmpty)
            const Text(
              'No contact is on record - please reach out to your local Sanchalak.',
              style: TextStyle(color: Colors.grey),
            )
          else
            for (final contact in _contacts!)
              Card(
                child: ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.person)),
                  title: Text(contact.name,
                      style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text(contact.mobile),
                  trailing: const Icon(Icons.call_outlined),
                ),
              ),
        ],
      ],
    );
  }
}
