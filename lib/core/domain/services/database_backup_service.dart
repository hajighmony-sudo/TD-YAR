import 'dart:io';

import 'package:sqflite/sqflite.dart';

import '../../data/local/app_database.dart';

/// Creates and restores local SQLite snapshots.
///
/// Backup/restore is intentionally an exclusive operation: the singleton
/// database is closed while the file is copied or replaced. Callers should
/// pause repository writes for the duration of either operation.
class DatabaseBackupService {
  DatabaseBackupService({AppDatabase? database})
      : _db = database ?? AppDatabase.instance;

  final AppDatabase _db;

  static const int _currentSchemaVersion = 6;
  static const List<String> _requiredTables = [
    'trades',
    'trade_executions',
    'journal_entries',
    'strategies',
    'psychology_entries',
    'risk_profiles',
    'trade_risk_records',
    'trading_sessions',
    'learning_resources',
    'trading_accounts',
    'app_settings',
    'audit_events',
  ];

  Future<File> createBackup(String destinationPath) async {
    if (!destinationPath.toLowerCase().endsWith('.db')) {
      throw ArgumentError('Backup path must end with .db.');
    }
    final sourcePath = await _db.databaseFile;
    final source = File(sourcePath);
    final target = File(destinationPath).absolute;
    if (target.path == source.absolute.path) {
      throw ArgumentError('Backup destination cannot be the live database.');
    }

    final database = await _db.database;
    // Checkpoint WAL pages before copying the main database file.
    await database.rawQuery('PRAGMA wal_checkpoint(TRUNCATE)');
    await _db.close();

    await target.parent.create(recursive: true);
    if (await target.exists()) await target.delete();
    final backup = await source.copy(target.path);
    await _db.database; // Reopen for subsequent repository operations.
    await _validateBackup(backup);
    return backup;
  }

  Future<void> restoreBackup(String backupPath) async {
    final source = File(backupPath);
    if (!await source.exists()) {
      throw FileSystemException('Backup file not found', backupPath);
    }
    if (source.lengthSync() < 100) {
      throw const FormatException('Backup file is too small to be SQLite.');
    }

    // Validate the candidate before touching the current database.
    await _validateBackup(source);

    final livePath = await _db.databaseFile;
    final live = File(livePath);
    final staged = File('$livePath.restore.tmp');
    final rollback = File('$livePath.restore.previous');

    await _db.close();
    try {
      if (await staged.exists()) await staged.delete();
      if (await rollback.exists()) await rollback.delete();
      await source.copy(staged.path);
      if (await live.exists()) await live.rename(rollback.path);
      await staged.rename(live.path);

      // Opening runs the normal versioned migrations when needed.
      await _db.database;
      if (await rollback.exists()) await rollback.delete();
    } catch (_) {
      if (await staged.exists()) await staged.delete();
      if (await live.exists() && await rollback.exists()) {
        await live.delete();
      }
      if (await rollback.exists()) await rollback.rename(live.path);
      await _db.close();
      await _db.database;
      rethrow;
    }
  }

  Future<void> _validateBackup(File file) async {
    final candidate = await openDatabase(
      file.path,
      readOnly: true,
      singleInstance: false,
    );
    try {
      final integrity = await candidate.rawQuery('PRAGMA integrity_check');
      if (integrity.isEmpty || integrity.first.values.first != 'ok') {
        throw const FormatException('SQLite integrity check failed.');
      }

      final versionRows = await candidate.rawQuery('PRAGMA user_version');
      final version = (versionRows.first.values.first as num).toInt();
      if (version < 1 || version > _currentSchemaVersion) {
        throw FormatException('Unsupported database schema version: $version.');
      }

      final rows = await candidate.rawQuery(
        "SELECT name FROM sqlite_master WHERE type = 'table'",
      );
      final tables = rows.map((row) => row['name'] as String).toSet();
      final missing = _requiredTables.where((name) => !tables.contains(name));
      if (missing.isNotEmpty) {
        throw FormatException(
          'Backup is missing required tables: ${missing.join(', ')}.',
        );
      }
    } finally {
      await candidate.close();
    }
  }
}
