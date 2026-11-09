import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'package:td_yar/core/data/local/app_database.dart';
import 'package:td_yar/core/domain/services/json_archive_import_service.dart';

void main() {
  late Directory temporaryDirectory;

  setUpAll(() async {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
    temporaryDirectory = await Directory.systemTemp.createTemp('td_yar_db_test_');
    await databaseFactory.setDatabasesPath(temporaryDirectory.path);
  });

  tearDownAll(() async {
    await AppDatabase.instance.close();
    if (await temporaryDirectory.exists()) {
      await temporaryDirectory.delete(recursive: true);
    }
  });

  test('concurrent callers share one configured schema-v8 database', () async {
    final database = AppDatabase.instance;
    final opened = await Future.wait(
      List<Future<Database>>.generate(12, (_) => database.database),
    );

    expect(opened.every((candidate) => identical(candidate, opened.first)), isTrue);
    final db = opened.first;

    final foreignKeys = await db.rawQuery('PRAGMA foreign_keys');
    expect(foreignKeys.single.values.single, 1);

    final tables = await db.rawQuery(
      "SELECT name FROM sqlite_master WHERE type = 'table'",
    );
    final names = tables.map((row) => row['name']).toSet();
    expect(names, containsAll({
      'trades',
      'trade_executions',
      'journal_entries',
      'strategies',
      'risk_profiles',
      'trading_accounts',
      'learning_resources',
      'audit_events',
    }));

    final now = DateTime.utc(2026, 1, 1).toIso8601String();
    await db.insert('trading_accounts', {
      'id': 'account-1',
      'name': 'Primary',
      'currency': 'USD',
      'initial_balance': 10000,
      'is_archived': 0,
      'notes': '',
      'created_at': now,
      'updated_at': now,
    });
    await db.insert('trades', {
      'id': 'trade-1',
      'symbol': 'TEST',
      'direction': 'long',
      'account_label': 'Primary',
      'account_id': 'account-1',
      'notes': '',
      'tags_json': '[]',
      'contract_multiplier': 1,
      'strategy_id': null,
      'created_at': now,
      'updated_at': now,
    });

    final auditRows = await db.query(
      'audit_events',
      where: 'entity_type = ? AND entity_id = ?',
      whereArgs: ['trade', 'trade-1'],
    );
    expect(auditRows, hasLength(1));
    expect(auditRows.single['event_type'], 'insert');

    await expectLater(
      db.update(
        'audit_events',
        {'details_json': '{"tampered":true}'},
        where: 'id = ?',
        whereArgs: [auditRows.single['id']],
      ),
      throwsA(isA<DatabaseException>()),
    );

    await expectLater(
      db.insert('trades', {
        'id': 'trade-invalid-account',
        'symbol': 'TEST',
        'direction': 'long',
        'account_id': 'missing-account',
        'created_at': now,
        'updated_at': now,
      }),
      throwsA(isA<DatabaseException>()),
    );
  });

  test('database file path resolves inside the configured application folder', () async {
    final expected = path.join(temporaryDirectory.path, 'td_yar.db');
    expect(await AppDatabase.instance.databaseFile, expected);
  });
  test('JSON archive import rolls back all rows when a duplicate appears', () async {
    final importer = JsonArchiveImportService(database: AppDatabase.instance);
    final now = DateTime.utc(2026, 1, 1).toIso8601String();
    final archive = {
      'format': 'td-yar-export',
      'schema_version': 2,
      'accounts': [
        {
          'id': 'duplicate-account',
          'name': 'Duplicate A',
          'currency': 'USD',
          'initial_balance': 100,
          'is_archived': 0,
          'notes': '',
          'created_at': now,
          'updated_at': now,
        },
        {
          'id': 'duplicate-account',
          'name': 'Duplicate B',
          'currency': 'USD',
          'initial_balance': 200,
          'is_archived': 0,
          'notes': '',
          'created_at': now,
          'updated_at': now,
        },
      ],
      'strategies': [],
      'playbooks': [],
      'attachments': [],
      'trades': [],
      'journal_entries': [],
      'psychology_entries': [],
      'risk_profiles': [],
      'trade_risk_records': [],
      'learning_resources': [],
      'trading_sessions': [],
      'audit_events': [],
      'settings': <String, String>{},
    };

    await expectLater(
      importer.importJson(archive.toString().replaceAll("'", '"')),
      throwsA(isA<FormatException>()),
    );
  });

}
