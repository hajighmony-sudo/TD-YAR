import 'dart:io';

import 'package:sqflite/sqflite.dart';

import '../../data/local/app_database.dart';

/// Creates and restores local SQLite snapshots.
///
/// Backup/restore is an exclusive operation. Pause repository writes while it
/// runs; the singleton database is briefly closed during file replacement.
class DatabaseBackupService {
  DatabaseBackupService({AppDatabase? database})
      : _db = database ?? AppDatabase.instance;

  final AppDatabase _db;
  static const int _currentSchemaVersion = 8;

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
    await database.rawQuery('PRAGMA wal_checkpoint(TRUNCATE)');
    await _db.close();
    try {
      await target.parent.create(recursive: true);
      if (await target.exists()) await target.delete();
      final backup = await source.copy(target.path);
      await _validateBackup(backup);
      return backup;
    } finally {
      await _db.database;
    }
  }

  Future<void> restoreBackup(String backupPath) async {
    final source = File(backupPath);
    if (!await source.exists()) {
      throw FileSystemException('Backup file not found', backupPath);
    }
    if (await source.length() < 100) {
      throw const FormatException('Backup file is too small to be SQLite.');
    }
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
      await _db.database; // Runs supported versioned migrations.
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

      final required = <String>[
        'trades',
        'trade_executions',
        'journal_entries',
        if (version >= 2) ...[
          'strategies',
          'risk_profiles',
          'trade_risk_records',
          'psychology_entries',
        ],
        if (version >= 5) 'trading_sessions',
        if (version >= 6) ...[
          'learning_resources',
          'trading_accounts',
          'app_settings',
          'audit_events',
        ],
      ];
      final rows = await candidate.rawQuery(
        "SELECT name FROM sqlite_master WHERE type = 'table'",
      );
      final tables = rows.map((row) => row['name'] as String).toSet();
      final missing = required.where((name) => !tables.contains(name));
      if (version >= 8) {
        final riskColumns = await candidate.rawQuery('PRAGMA table_info(risk_profiles)');
        if (!riskColumns.any((column) => column['name'] == 'account_id')) {
          throw const FormatException('Schema v8 backup is missing risk_profiles.account_id.');
        }
      }
      if (version >= 7) {
        final tradeColumns = await candidate.rawQuery('PRAGMA table_info(trades)');
        if (!tradeColumns.any((column) => column['name'] == 'account_id')) {
          throw const FormatException('Schema v7 backup is missing trades.account_id.');
        }
      }
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
