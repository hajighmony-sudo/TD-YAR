import 'package:sqflite/sqflite.dart';

import '../../domain/models/journal_entry.dart';
import '../local/app_database.dart';
import 'journal_repository.dart';

class SqliteJournalRepository implements JournalRepository {
  SqliteJournalRepository({AppDatabase? appDatabase})
      : _appDatabase = appDatabase ?? AppDatabase.instance;

  final AppDatabase _appDatabase;

  Future<Database> get _db => _appDatabase.database;

  @override
  Future<void> createEntry(JournalEntry entry) async {
    await (await _db).insert(
      'journal_entries',
      entry.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> updateEntry(JournalEntry entry) async {
    final changed = await (await _db).update(
      'journal_entries',
      entry.toMap(),
      where: 'id = ?',
      whereArgs: [entry.id],
    );
    if (changed == 0) {
      throw StateError('Journal entry not found: ${entry.id}');
    }
  }

  @override
  Future<JournalEntry?> getEntry(String id) async {
    final rows = await (await _db).query(
      'journal_entries',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return JournalEntry.fromMap(rows.first);
  }

  @override
  Future<List<JournalEntry>> getEntries({
    DateTime? from,
    DateTime? to,
  }) async {
    final clauses = <String>[];
    final args = <Object?>[];
    if (from != null) {
      clauses.add('entry_date >= ?');
      args.add(from.toUtc().toIso8601String());
    }
    if (to != null) {
      clauses.add('entry_date <= ?');
      args.add(to.toUtc().toIso8601String());
    }

    final rows = await (await _db).query(
      'journal_entries',
      where: clauses.isEmpty ? null : clauses.join(' AND '),
      whereArgs: clauses.isEmpty ? null : args,
      orderBy: 'entry_date DESC, created_at DESC',
    );
    return rows.map(JournalEntry.fromMap).toList(growable: false);
  }

  @override
  Future<void> deleteEntry(String id) async {
    await (await _db).delete(
      'journal_entries',
      where: 'id = ?',
      whereArgs: [id],
    );
  }
}
