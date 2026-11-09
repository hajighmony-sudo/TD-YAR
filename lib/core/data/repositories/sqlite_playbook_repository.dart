import 'package:sqflite/sqflite.dart';

import '../../domain/models/playbook.dart';
import '../local/app_database.dart';
import 'playbook_repository.dart';

class SqlitePlaybookRepository implements PlaybookRepository {
  SqlitePlaybookRepository({AppDatabase? appDatabase})
      : _appDatabase = appDatabase ?? AppDatabase.instance;

  final AppDatabase _appDatabase;

  Future<Database> get _db => _appDatabase.database;

  @override
  Future<void> createPlaybook(Playbook playbook) async {
    await (await _db).insert(
      'playbooks',
      playbook.toMap(),
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<void> updatePlaybook(Playbook playbook) async {
    final changed = await (await _db).update(
      'playbooks',
      playbook.toMap(),
      where: 'id = ?',
      whereArgs: [playbook.id],
    );
    if (changed == 0) {
      throw StateError('Playbook not found: ${playbook.id}');
    }
  }

  @override
  Future<Playbook?> getPlaybook(String id) async {
    final rows = await (await _db).query(
      'playbooks',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return Playbook.fromMap(rows.first);
  }

  @override
  Future<List<Playbook>> getPlaybooks() async {
    final rows = await (await _db).query(
      'playbooks',
      orderBy: 'name COLLATE NOCASE ASC',
    );
    return rows.map(Playbook.fromMap).toList(growable: false);
  }

  @override
  Future<void> deletePlaybook(String id) async {
    await (await _db).delete(
      'playbooks',
      where: 'id = ?',
      whereArgs: [id],
    );
  }
}
