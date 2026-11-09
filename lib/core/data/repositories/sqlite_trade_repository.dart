import 'package:sqflite/sqflite.dart';

import '../../domain/models/trade.dart';
import '../../domain/models/trade_execution.dart';
import '../../domain/services/trade_calculator.dart';
import '../local/app_database.dart';
import 'trade_repository.dart';

class SqliteTradeRepository implements TradeRepository {
  SqliteTradeRepository({AppDatabase? appDatabase})
      : _appDatabase = appDatabase ?? AppDatabase.instance;

  final AppDatabase _appDatabase;
  Future<Database> get _db => _appDatabase.database;

  static void _validateTrade(Trade trade) {
    if (trade.id.trim().isEmpty || trade.symbol.trim().isEmpty ||
        !trade.contractMultiplier.isFinite || trade.contractMultiplier <= 0) {
      throw ArgumentError('Trade id, symbol and a positive finite multiplier are required.');
    }
  }

  Future<void> _validateStrategy(DatabaseExecutor db, String? strategyId) async {
    if (strategyId == null) return;
    final rows = await db.query(
      'strategies', columns: ['id'],
      where: 'id = ?', whereArgs: [strategyId], limit: 1,
    );
    if (rows.isEmpty) throw StateError('Strategy not found: $strategyId');
  }

  @override
  Future<void> createTrade(Trade trade) async {
    _validateTrade(trade);
    final db = await _db;
    await db.transaction((txn) async {
      await _validateStrategy(txn, trade.strategyId);
      await txn.insert(
        'trades', trade.toMap(),
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    });
  }

  @override
  Future<void> updateTrade(Trade trade) async {
    _validateTrade(trade);
    final db = await _db;
    await db.transaction((txn) async {
      final rows = await txn.query(
        'trades', where: 'id = ?', whereArgs: [trade.id], limit: 1,
      );
      if (rows.isEmpty) throw StateError('Trade not found: ${trade.id}');
      final existing = Trade.fromMap(rows.first);
      final executionCount = Sqflite.firstIntValue(await txn.rawQuery(
        'SELECT COUNT(*) FROM trade_executions WHERE trade_id = ?',
        [trade.id],
      )) ?? 0;
      if (executionCount > 0 &&
          (existing.direction != trade.direction ||
              existing.contractMultiplier != trade.contractMultiplier)) {
        throw StateError(
          'Direction and contract multiplier cannot change after executions exist.',
        );
      }
      await _validateStrategy(txn, trade.strategyId);
      await txn.update(
        'trades', trade.toMap(),
        where: 'id = ?', whereArgs: [trade.id],
      );
    });
  }

  @override
  Future<Trade?> getTrade(String id) async {
    final rows = await (await _db).query(
      'trades', where: 'id = ?', whereArgs: [id], limit: 1,
    );
    return rows.isEmpty ? null : Trade.fromMap(rows.first);
  }

  @override
  Future<List<Trade>> getTrades({String? symbol}) async {
    final normalizedSymbol = symbol?.trim();
    final rows = await (await _db).query(
      'trades',
      where: normalizedSymbol == null || normalizedSymbol.isEmpty
          ? null : 'symbol = ?',
      whereArgs: normalizedSymbol == null || normalizedSymbol.isEmpty
          ? null : [normalizedSymbol],
      orderBy: 'created_at DESC',
    );
    return rows.map(Trade.fromMap).toList(growable: false);
  }

  @override
  Future<void> addExecution(TradeExecution execution) async {
    final db = await _db;
    await db.transaction((txn) async {
      final tradeRows = await txn.query(
        'trades', where: 'id = ?', whereArgs: [execution.tradeId], limit: 1,
      );
      if (tradeRows.isEmpty) {
        throw StateError('Trade not found: ${execution.tradeId}');
      }
      final existingRows = await txn.query(
        'trade_executions',
        where: 'trade_id = ?', whereArgs: [execution.tradeId],
        orderBy: 'executed_at ASC, id ASC',
      );
      final existing = existingRows.map(TradeExecution.fromMap).toList();
      TradeCalculator.calculate(
        trade: Trade.fromMap(tradeRows.first),
        executions: [...existing, execution],
      );
      await txn.insert(
        'trade_executions', execution.toMap(),
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    });
  }

  @override
  Future<List<TradeExecution>> getExecutions(String tradeId) async {
    final rows = await (await _db).query(
      'trade_executions',
      where: 'trade_id = ?', whereArgs: [tradeId],
      orderBy: 'executed_at ASC, id ASC',
    );
    return rows.map(TradeExecution.fromMap).toList(growable: false);
  }

  @override
  Future<void> deleteTrade(String id) async {
    final db = await _db;
    await db.transaction((txn) async {
      await txn.delete(
        'trade_executions', where: 'trade_id = ?', whereArgs: [id],
      );
      await txn.delete('trades', where: 'id = ?', whereArgs: [id]);
    });
  }
}
