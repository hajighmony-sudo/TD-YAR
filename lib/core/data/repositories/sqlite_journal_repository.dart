import 'package:sqflite/sqflite.dart';

import '../../domain/models/journal_entry.dart';
import '../local/app_database.dart';
import 'journal_repository.dart';

class SqliteJournalRepository implements JournalRepository {
  SqliteJournalRepository({AppDatabase? appDatabase})
      : _appDatabase = appDatabase ?? AppDatabase.instance;

  final AppDatabase _appDatabase;
  Future<Database> get _db => _appDatabase.database;

  static void _validate(JournalEntry entry) {
    if (entry.id.trim().isEmpty) {
      throw ArgumentError('Journal entry id is required.');
    }
    final collections = [
      entry.goals, entry.tags, entry.linkedTradeIds,
    ];
    if (collections.any((items) => items.any((item) => item.trim().isEmpty))) {
      throw ArgumentError('Journal goals, tags and trade links cannot be empty.');
    }
  }

  Future<void> _validateTradeLinks(
    DatabaseExecutor executor,
    JournalEntry entry,
  ) async {
    for (final tradeId in entry.linkedTradeIds.toSet()) {
      final rows = await executor.query(
        'trades',
        columns: ['id'],
        where: 'id = ?',
        whereArgs: [tradeId],
        limit: 1,
      );
      if (rows.isEmpty) {
        throw StateError('Journal references missing trade: $tradeId');
      }
    }
  }

  @override
  Future<void> createEntry(JournalEntry entry) async {
    _validate(entry);
    final db = await _db;
    await db.transaction((txn) async {
      await _validateTradeLinks(txn, entry);
      await txn.insert(
        'journal_entries',
        entry.toMap(),
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    });
  }

  @override
  Future<void> updateEntry(JournalEntry entry) async {
    _validate(entry);
    final db = await _db;
    await db.transaction((txn) async {
      await _validateTradeLinks(txn, entry);
      final changed = await txn.update(
        'journal_entries',
        entry.toMap(),
        where: 'id = ?',
        whereArgs: [entry.id],
      );
      if (changed == 0) {
        throw StateError('Journal entry not found: ${entry.id}');
      }
    });
  }

  @override
  Future<JournalEntry?> getEntry(String id) async {
    final rows = await (await _db).query(
      'journal_entries',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    return rows.isEmpty ? null : JournalEntry.fromMap(rows.first);
  }

  @override
  Future<List<JournalEntry>> getEntries({
    DateTime? from,
    DateTime? to,
  }) async {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('Journal end date must not precede start date.');
    }
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
    final db = await _db;
    await db.transaction((txn) async {
      await txn.delete(
        'attachments',
        where: 'owner_type = ? AND owner_id = ?',
        whereArgs: ['journal', id],
      );
      await txn.delete(
        'journal_entries',
        where: 'id = ?',
        whereArgs: [id],
      );
    });
  }
}
