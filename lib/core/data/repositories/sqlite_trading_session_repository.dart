import 'package:sqflite/sqflite.dart';

import '../../domain/models/trading_session.dart';
import '../local/app_database.dart';
import 'trading_session_repository.dart';

class SqliteTradingSessionRepository implements TradingSessionRepository {
  SqliteTradingSessionRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  @override
  Future<void> create(TradingSessionDefinition session) async {
    session.validate();
    await (await _db).insert(
      'trading_sessions',
      session.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> update(TradingSessionDefinition session) async {
    session.validate();
    final changed = await (await _db).update(
      'trading_sessions',
      session.toMap(),
      where: 'id = ?',
      whereArgs: [session.id],
    );
    if (changed == 0) {
      throw StateError('Trading session not found: ${session.id}');
    }
  }

  @override
  Future<TradingSessionDefinition?> getById(String id) async {
    final rows = await (await _db).query(
      'trading_sessions',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    return rows.isEmpty ? null : TradingSessionDefinition.fromMap(rows.first);
  }

  @override
  Future<List<TradingSessionDefinition>> getAll({
    bool includeDisabled = false,
  }) async {
    final rows = await (await _db).query(
      'trading_sessions',
      where: includeDisabled ? null : 'is_enabled = 1',
      orderBy: 'name COLLATE NOCASE ASC',
    );
    return rows
        .map(TradingSessionDefinition.fromMap)
        .toList(growable: false);
  }

  @override
  Future<void> delete(String id) async {
    await (await _db).delete(
      'trading_sessions',
      where: 'id = ?',
      whereArgs: [id],
    );
  }
}
