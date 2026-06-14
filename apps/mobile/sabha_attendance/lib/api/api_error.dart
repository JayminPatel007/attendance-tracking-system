import 'dart:convert';

import 'package:http/http.dart' as http;

/// Owns the backend error contract (RFC 9457 ProblemDetail, ADR per #70/#67):
/// it decodes the response body once and exposes the `detail` message, the
/// `code` extension, and any other top-level extension Jackson unwraps. Feature
/// clients declare their own exception types and use [apiError] to map statuses
/// to them, instead of each carrying a private body parser.
class ApiError {
  ApiError(this.statusCode, this.body, this._fields);

  final int statusCode;
  final String body;
  final Map<String, dynamic>? _fields;

  factory ApiError.from(http.Response response) {
    Map<String, dynamic>? fields;
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) fields = decoded;
    } on FormatException {
      // non-JSON body — leave fields null and fall back to caller copy
    }
    return ApiError(response.statusCode, response.body, fields);
  }

  /// The ProblemDetail `detail` message to surface, falling back to [fallback]
  /// when the body carries no parseable detail.
  String message(String fallback) => detail ?? fallback;

  String? get detail => _fields?['detail'] as String?;

  /// The `code` extension that distinguishes same-status failures (e.g.
  /// `ROSTER_STALE`, `MOBILE_ALREADY_REGISTERED`).
  String? get code => _fields?['code'] as String?;

  /// Any other top-level string extension Jackson unwraps from the
  /// ProblemDetail (e.g. `existingPersonId`).
  String? extension(String name) => _fields?[name] as String?;

  /// The generic transport-failure description used when no feature handler
  /// claims the status: `<label> -> <status>: <body>`.
  String describe(String label) => '$label -> $statusCode: $body';
}

/// Dispatches a failed [response] to the feature exception its status maps to.
///
/// Each entry in [handlers] receives the parsed [ApiError] and builds the named
/// exception for that status. A handler may return `null` to decline (e.g. a
/// 409 that only means something with a specific `code`), in which case — and
/// for any unmapped status — the [fallback] generic exception is thrown,
/// carrying [ApiError.describe].
Never apiError(
  http.Response response,
  String label,
  Map<int, Exception? Function(ApiError)> handlers, {
  required Exception Function(String) fallback,
}) {
  final err = ApiError.from(response);
  final mapped = handlers[response.statusCode]?.call(err);
  throw mapped ?? fallback(err.describe(label));
}
