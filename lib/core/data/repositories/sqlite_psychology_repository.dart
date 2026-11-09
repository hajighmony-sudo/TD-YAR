import 'package:sqflite/sqflite.dart';

import '../../domain/models/psychology_entry.dart';
import '../local/app_database.dart';
import 'psychology_repository.dart';

class SqlitePsychologyRepository implements PsychologyRepository {
  SqlitePsychologyRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  void _validate(PsychologyEntry entry) {
    for (final value in [entry.confidence, entry.focus, entry.stress]) {
      if (value < 1 || value > 5) {
        throw ArgumentError('Psychology ratings must be between 1 and 5.');
      }
    }
    if (entry.mood.trim().isEmpty) {
      throw ArgumentError.value(entry.mood, 'mood', 'Required.');
    }
  }

  @override
  Future<void> create(PsychologyEntry entry) async {
    _validate(entry);
    await (await _db).insert(
      'psychology_entries', entry.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> update(PsychologyEntry entry) async {
    _validate(entry);
    final changed = await (await _db).update(
      'psychology_entries', entry.toMap(),
      where: 'id = ?', whereArgs: [entry.id],
    );
    if (changed == 0) throw StateError('Psychology entry not found: ${entry.id}');
  }

  @override
  Future<PsychologyEntry?> getById(String id) async {
    final rows = await (await _db).query(
      'psychology_entries', where: 'id = ?', whereArgs: [id], limit: 1,
    );
    return rows.isEmpty ? null : PsychologyEntry.fromMap(rows.first);
  }

  @override
  Future<List<PsychologyEntry>> getEntries({
    DateTime? from,
    DateTime? to,
    String? tradeId,
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
    if (tradeId != null) {
      clauses.add('trade_id = ?');
      args.add(tradeId);
    }
    final rows = await (await _db).query(
      'psychology_entries',
      where: clauses.isEmpty ? null : clauses.join(' AND '),
      whereArgs: clauses.isEmpty ? null : args,
      orderBy: 'entry_date DESC',
    );
    return rows.map(PsychologyEntry.fromMap).toList(growable: false);
  }

  @override
  Future<void> delete(String id) async {
    await (await _db).delete(
      'psychology_entries', where: 'id = ?', whereArgs: [id],
    );
  }
}
