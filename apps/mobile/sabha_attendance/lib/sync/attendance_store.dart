import 'dart:convert';

import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../roster/roster_api.dart';
import 'pending_marking.dart';

/// Local SQLite-backed cache for the Sanchalak's offline workflow (ADR-0007):
/// the last-known [CurrentRoster] snapshot, the queue of [PendingMarking]s
/// awaiting sync, and the timestamp of the last successful sync.
///
/// The store is intentionally tiny and synchronous-feeling — there's only one
/// Sanchalak per device and one open Occurrence at a time, so a single-row
/// `roster_cache` table is enough.
class AttendanceStore {
  AttendanceStore._(this._db);

  final Database _db;

  /// Production wiring: opens (or creates) the on-device SQLite file under the
  /// platform-specific app documents directory.
  static Future<AttendanceStore> open({
    required DatabaseFactory factory,
    required String directory,
    String filename = 'sabha_attendance.db',
  }) async {
    final db = await factory.openDatabase(
      p.join(directory, filename),
      options: OpenDatabaseOptions(version: 1, onCreate: _onCreate),
    );
    return AttendanceStore._(db);
  }

  /// In-memory test factory. Uses sqflite_common_ffi's in-memory database so
  /// tests exercise real SQL without touching disk.
  static Future<AttendanceStore> openInMemory({required DatabaseFactory factory}) async {
    final db = await factory.openDatabase(
      inMemoryDatabasePath,
      options: OpenDatabaseOptions(version: 1, onCreate: _onCreate),
    );
    return AttendanceStore._(db);
  }

  Future<void> close() => _db.close();

  static Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE roster_cache (
        id              INTEGER PRIMARY KEY CHECK (id = 1),
        roster_version  TEXT NOT NULL,
        payload         TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE pending_markings (
        occurrence_id     TEXT NOT NULL,
        person_id         TEXT NOT NULL,
        present           INTEGER NOT NULL,
        client_marked_at  TEXT NOT NULL,
        PRIMARY KEY (occurrence_id, person_id)
      )
    ''');
    await db.execute('''
      CREATE TABLE sync_meta (
        key   TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');
  }

  Future<void> cacheRoster(CurrentRoster snapshot) async {
    final payload = jsonEncode({
      'occurrence': {
        'id': snapshot.occurrence.id,
        'date': snapshot.occurrence.date,
        'state': snapshot.occurrence.state,
        'sabhaId': snapshot.occurrence.sabhaId,
      },
      'roster': snapshot.roster
          .map((e) => {'personId': e.personId, 'fullName': e.fullName, 'present': e.present})
          .toList(),
    });
    await _db.insert(
      'roster_cache',
      {
        'id': 1,
        'roster_version': snapshot.rosterVersion.toUtc().toIso8601String(),
        'payload': payload,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<CurrentRoster?> cachedRoster() async {
    final rows = await _db.query('roster_cache', limit: 1);
    if (rows.isEmpty) return null;
    final row = rows.first;
    final payload = jsonDecode(row['payload'] as String) as Map<String, dynamic>;
    final occ = payload['occurrence'] as Map<String, dynamic>;
    final roster = (payload['roster'] as List<dynamic>)
        .map((e) => RosterEntry.fromJson(e as Map<String, dynamic>))
        .toList();
    return CurrentRoster(
      occurrence: OccurrenceView(
        id: occ['id'] as String,
        date: occ['date'] as String,
        state: occ['state'] as String,
        sabhaId: occ['sabhaId'] as String,
      ),
      roster: roster,
      rosterVersion: DateTime.parse(row['roster_version'] as String),
    );
  }

  Future<void> enqueueMarking(PendingMarking m) async {
    await _db.insert(
      'pending_markings',
      {
        'occurrence_id': m.occurrenceId,
        'person_id': m.personId,
        'present': m.present ? 1 : 0,
        'client_marked_at': m.clientMarkedAt.toUtc().toIso8601String(),
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<PendingMarking>> pendingMarkings() async {
    final rows = await _db.query('pending_markings', orderBy: 'client_marked_at ASC');
    return rows
        .map((r) => PendingMarking(
              occurrenceId: r['occurrence_id'] as String,
              personId: r['person_id'] as String,
              present: (r['present'] as int) == 1,
              clientMarkedAt: DateTime.parse(r['client_marked_at'] as String),
            ))
        .toList();
  }

  Future<void> clearPending(List<PendingMarking> markings) async {
    final batch = _db.batch();
    for (final m in markings) {
      batch.delete(
        'pending_markings',
        where: 'occurrence_id = ? AND person_id = ?',
        whereArgs: [m.occurrenceId, m.personId],
      );
    }
    await batch.commit(noResult: true);
  }

  Future<DateTime?> lastSyncedAt() async {
    final rows = await _db.query('sync_meta', where: 'key = ?', whereArgs: ['last_synced_at']);
    if (rows.isEmpty) return null;
    return DateTime.parse(rows.first['value'] as String);
  }

  Future<void> setLastSyncedAt(DateTime at) async {
    await _db.insert(
      'sync_meta',
      {'key': 'last_synced_at', 'value': at.toUtc().toIso8601String()},
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }
}
