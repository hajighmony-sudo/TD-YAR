import 'package:sqflite/sqflite.dart';

import '../../domain/models/trade.dart';
import '../../domain/models/trade_execution.dart';
import '../local/app_database.dart';
import 'trade_repository.dart';

class SqliteTradeRepository implements TradeRepository {
  SqliteTradeRepository({AppDatabase? appDatabase})
      : _appDatabase = appDatabase ?? AppDatabase.instance;

  final AppDatabase _appDatabase;

  Future<Database> get _db => _appDatabase.database;

  @override
  Future<void> createTrade(Trade trade) async {
    await (await _db).insert(
      'trades',
      trade.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> updateTrade(Trade trade) async {
    final changed = await (await _db).update(
      'trades',
      trade.toMap(),
      where: 'id = ?',
      whereArgs: [trade.id],
    );
    if (changed == 0) {
      throw StateError('Trade not found: ${trade.id}');
    }
  }

  @override
  Future<Trade?> getTrade(String id) async {
    final rows = await (await _db).query(
      'trades',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return Trade.fromMap(rows.first);
  }

  @override
  Future<List<Trade>> getTrades({String? symbol}) async {
    final normalizedSymbol = symbol?.trim();
    final rows = await (await _db).query(
      'trades',
      where: normalizedSymbol == null || normalizedSymbol.isEmpty
          ? null
          : 'symbol = ?',
      whereArgs: normalizedSymbol == null || normalizedSymbol.isEmpty
          ? null
          : [normalizedSymbol],
      orderBy: 'created_at DESC',
    );
    return rows.map(Trade.fromMap).toList(growable: false);
  }

  @override
  Future<void> addExecution(TradeExecution execution) async {
    await (await _db).insert(
      'trade_executions',
      execution.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<List<TradeExecution>> getExecutions(String tradeId) async {
    final rows = await (await _db).query(
      'trade_executions',
      where: 'trade_id = ?',
      whereArgs: [tradeId],
      orderBy: 'executed_at ASC, id ASC',
    );
    return rows.map(TradeExecution.fromMap).toList(growable: false);
  }

  @override
  Future<void> deleteTrade(String id) async {
    final db = await _db;
    await db.transaction((txn) async {
      await txn.delete(
        'trade_executions',
        where: 'trade_id = ?',
        whereArgs: [id],
      );
      await txn.delete('trades', where: 'id = ?', whereArgs: [id]);
    });
  }
}
