import 'package:flutter/material.dart';

import '../password_reset/password_reset_api.dart';
import '../password_reset/password_reset_controller.dart';
import '../password_reset/password_reset_screen.dart';
import '../password_reset/who_appointed_me_screen.dart';
import 'auth_service.dart';
import 'session.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({
    super.key,
    required this.session,
    required this.auth,
    required this.backendBaseUrl,
  });

  final Session session;
  final AuthService auth;

  /// Backend origin for the unauthenticated password-reset calls (ADR-0004).
  final String backendBaseUrl;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _busy = false;
  String? _error;

  Future<void> _login() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final token = await widget.auth.login();
      widget.session.set(token);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  /// The reset and lookup screens are unauthenticated and call `/api/...`
  /// directly, so they only need the backend origin — never the session token.
  PasswordResetApi _resetApi() => PasswordResetApi(baseUrl: widget.backendBaseUrl);

  void _openForgotPassword() {
    Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (context) => Scaffold(
        appBar: AppBar(title: const Text('Reset password')),
        body: PasswordResetScreen(
          controller: PasswordResetController(api: _resetApi()),
          onDone: () => Navigator.of(context).maybePop(),
        ),
      ),
    ));
  }

  void _openWhoAppointedMe() {
    Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => Scaffold(
        appBar: AppBar(title: const Text('Who appointed me?')),
        body: WhoAppointedMeScreen(api: _resetApi()),
      ),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Sign in to mark attendance.',
              style: TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _busy ? null : _login,
              child: Text(_busy ? 'Signing in…' : 'Sign in'),
            ),
            if (_error != null) ...[
              const SizedBox(height: 16),
              Text(_error!, style: const TextStyle(color: Colors.red)),
            ],
            const SizedBox(height: 24),
            TextButton(
              key: const Key('login-forgot-password'),
              onPressed: _busy ? null : _openForgotPassword,
              child: const Text('Forgot password?'),
            ),
            TextButton(
              key: const Key('login-who-appointed-me'),
              onPressed: _busy ? null : _openWhoAppointedMe,
              child: const Text('Who appointed me?'),
            ),
          ],
        ),
      ),
    );
  }
}
