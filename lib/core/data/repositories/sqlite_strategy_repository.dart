import 'package:sqflite/sqflite.dart';

import '../../domain/models/strategy.dart';
import '../local/app_database.dart';
import 'strategy_repository.dart';

class SqliteStrategyRepository implements StrategyRepository {
  SqliteStrategyRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  static void _validate(Strategy strategy) {
    if (strategy.id.trim().isEmpty || strategy.name.trim().isEmpty) {
      throw ArgumentError('Strategy id and name are required.');
    }
    final collections = [
      strategy.timeframes,
      strategy.setupConditions,
      strategy.entryRules,
      strategy.exitRules,
      strategy.riskRules,
      strategy.checklistItems,
      strategy.tags,
    ];
    if (collections.any((items) => items.any((item) => item.trim().isEmpty))) {
      throw ArgumentError('Strategy lists cannot contain empty values.');
    }
  }

  Future<void> _ensureUniqueName(
    DatabaseExecutor txn,
    String name, {
    String? excludingId,
  }) async {
    final rows = await txn.query(
      'strategies',
      columns: ['id', 'name'],
    );
    final normalized = name.trim().toLowerCase();
    final duplicate = rows.any((row) =>
        row['id'] != excludingId &&
        (row['name'] as String).trim().toLowerCase() == normalized);
    if (duplicate) {
      throw StateError('A strategy with this name already exists.');
    }
  }

  @override
  Future<void> create(Strategy strategy) async {
    _validate(strategy);
    final db = await _db;
    await db.transaction((txn) async {
      await _ensureUniqueName(txn, strategy.name);
      await txn.insert(
        'strategies',
        strategy.toMap(),
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    });
  }

  @override
  Future<void> update(Strategy strategy) async {
    _validate(strategy);
    final db = await _db;
    await db.transaction((txn) async {
      final current = await txn.query(
        'strategies',
        columns: ['id'],
        where: 'id = ?',
        whereArgs: [strategy.id],
        limit: 1,
      );
      if (current.isEmpty) throw StateError('Strategy not found: ${strategy.id}');
      await _ensureUniqueName(
        txn,
        strategy.name,
        excludingId: strategy.id,
      );
      await txn.update(
        'strategies',
        strategy.toMap(),
        where: 'id = ?',
        whereArgs: [strategy.id],
      );
    });
  }

  @override
  Future<Strategy?> getById(String id) async {
    final rows = await (await _db).query(
      'strategies',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    return rows.isEmpty ? null : Strategy.fromMap(rows.first);
  }

  @override
  Future<List<Strategy>> getAll({bool includeArchived = false}) async {
    final rows = await (await _db).query(
      'strategies',
      where: includeArchived ? null : 'is_archived = 0',
      orderBy: 'name COLLATE NOCASE ASC',
    );
    return rows.map(Strategy.fromMap).toList(growable: false);
  }

  @override
  Future<void> delete(String id) async {
    final db = await _db;
    await db.transaction((txn) async {
      await txn.update(
        'trades',
        {'strategy_id': null},
        where: 'strategy_id = ?',
        whereArgs: [id],
      );
      await txn.delete(
        'attachments',
        where: 'owner_type = ? AND owner_id = ?',
        whereArgs: ['strategy', id],
      );
      await txn.delete('strategies', where: 'id = ?', whereArgs: [id]);
    });
  }
}
