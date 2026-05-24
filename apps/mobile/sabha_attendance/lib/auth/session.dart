import 'package:flutter/foundation.dart';

/// In-memory token holder. Tracer-slice persistence: the access token lives
/// for one app session and is dropped on restart. flutter_secure_storage lands
/// in a later slice; for now, a re-login on launch is acceptable.
class Session {
  final ValueNotifier<String?> accessToken = ValueNotifier<String?>(null);

  void set(String token) {
    accessToken.value = token;
  }

  void clear() {
    accessToken.value = null;
  }
}
