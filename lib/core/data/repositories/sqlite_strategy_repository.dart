import 'package:sqflite/sqflite.dart';

import '../../domain/models/strategy.dart';
import '../local/app_database.dart';
import 'strategy_repository.dart';

class SqliteStrategyRepository implements StrategyRepository {
  SqliteStrategyRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  @override
  Future<void> create(Strategy strategy) async {
    if (strategy.name.trim().isEmpty) {
      throw ArgumentError.value(strategy.name, 'strategy.name', 'Required.');
    }
    await (await _db).insert(
      'strategies',
      strategy.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> update(Strategy strategy) async {
    final changed = await (await _db).update(
      'strategies',
      strategy.toMap(),
      where: 'id = ?',
      whereArgs: [strategy.id],
    );
    if (changed == 0) throw StateError('Strategy not found: ${strategy.id}');
  }

  @override
  Future<Strategy?> getById(String id) async {
    final rows = await (await _db).query(
      'strategies', where: 'id = ?', whereArgs: [id], limit: 1,
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
    await (await _db).delete('strategies', where: 'id = ?', whereArgs: [id]);
  }
}
