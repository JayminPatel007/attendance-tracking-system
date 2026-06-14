import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

import 'package:sabha_attendance_mobile/api/api_error.dart';

void main() {
  group('ApiError.message', () {
    test('returns the ProblemDetail detail when the body carries one', () {
      final err = ApiError.from(http.Response('{"detail":"not a Sanchalak"}', 403));

      expect(err.message('fallback'), 'not a Sanchalak');
    });

    test('falls back to the caller copy when the body is not JSON', () {
      final err = ApiError.from(http.Response('boom', 500));

      expect(err.message('fallback'), 'fallback');
    });

    test('falls back to the caller copy when the JSON has no detail', () {
      final err = ApiError.from(http.Response('{"code":"X"}', 422));

      expect(err.message('fallback'), 'fallback');
    });
  });

  group('extension reads', () {
    test('exposes the code extension', () {
      final err = ApiError.from(http.Response('{"code":"ROSTER_STALE"}', 409));

      expect(err.code, 'ROSTER_STALE');
    });

    test('code is null when absent or body is not JSON', () {
      expect(ApiError.from(http.Response('{"detail":"x"}', 409)).code, isNull);
      expect(ApiError.from(http.Response('boom', 500)).code, isNull);
    });

    test('reads an arbitrary top-level extension', () {
      final err = ApiError.from(http.Response('{"existingPersonId":"p-7"}', 409));

      expect(err.extension('existingPersonId'), 'p-7');
    });
  });

  group('apiError dispatcher', () {
    Never call(http.Response resp, Map<int, Exception? Function(ApiError)> handlers) =>
        apiError(resp, 'POST thing', handlers, fallback: _Generic.new);

    test('throws the handler-mapped exception for a mapped status', () {
      expect(
        () => call(http.Response('{"detail":"nope"}', 403), {
          403: (e) => _Forbidden(e.message('default')),
        }),
        throwsA(isA<_Forbidden>().having((e) => e.message, 'message', 'nope')),
      );
    });

    test('uses the handler default copy when the body has no detail', () {
      expect(
        () => call(http.Response('boom', 403), {
          403: (e) => _Forbidden(e.message('default')),
        }),
        throwsA(isA<_Forbidden>().having((e) => e.message, 'message', 'default')),
      );
    });

    test('falls back to the generic exception for an unmapped status', () {
      expect(
        () => call(http.Response('boom', 500), {
          403: (e) => _Forbidden(e.message('default')),
        }),
        throwsA(isA<_Generic>().having((e) => e.message, 'message', 'POST thing -> 500: boom')),
      );
    });

    test('falls through to the generic exception when a handler returns null', () {
      expect(
        () => call(http.Response('{"code":"OTHER"}', 409), {
          409: (e) => e.code == 'ROSTER_STALE' ? _Forbidden(e.message('x')) : null,
        }),
        throwsA(isA<_Generic>().having((e) => e.message, 'message', contains('409'))),
      );
    });
  });
}

class _Generic implements Exception {
  _Generic(this.message);
  final String message;
}

class _Forbidden implements Exception {
  _Forbidden(this.message);
  final String message;
}
