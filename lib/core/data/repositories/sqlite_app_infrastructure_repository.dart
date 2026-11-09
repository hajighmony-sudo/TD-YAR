import 'dart:convert';

import 'package:sqflite/sqflite.dart';

import '../local/app_database.dart';
import 'app_infrastructure_repository.dart';

class SqliteAppInfrastructureRepository implements AppInfrastructureRepository {
  SqliteAppInfrastructureRepository({AppDatabase? database})
      : _db = database ?? AppDatabase.instance;

  final AppDatabase _db;

  @override
  Future<void> saveAccount(TradingAccount account) async {
    if (account.id.trim().isEmpty ||
        account.name.trim().isEmpty ||
        account.currency.trim().isEmpty) {
      throw ArgumentError('Account id, name and currency are required.');
    }
    if (!account.initialBalance.isFinite || account.initialBalance < 0) {
      throw ArgumentError.value(account.initialBalance, 'initialBalance');
    }
    final accountValues = account.toMap()..['name'] = account.name.trim();
    final db = await _db.database;
    await db.transaction((txn) async {
      final duplicate = await txn.query(
        'trading_accounts',
        columns: ['id'],
        where: 'name = ? COLLATE NOCASE AND id != ?',
        whereArgs: [account.name.trim(), account.id],
        limit: 1,
      );
      if (duplicate.isNotEmpty) {
        throw StateError('An account with this name already exists.');
      }
      final existing = await txn.query(
        'trading_accounts',
        columns: ['id'],
        where: 'id = ?',
        whereArgs: [account.id],
        limit: 1,
      );
      if (existing.isEmpty) {
        await txn.insert(
          'trading_accounts',
          accountValues,
          conflictAlgorithm: ConflictAlgorithm.abort,
        );
      } else {
        await txn.update(
          'trading_accounts',
          accountValues,
          where: 'id = ?',
          whereArgs: [account.id],
        );
        // Keep the legacy display label synchronized; account_id remains the
        // canonical relationship and is never cleared by an upsert.
        await txn.update(
          'trades',
          {'account_label': account.name.trim()},
          where: 'account_id = ?',
          whereArgs: [account.id],
        );
      }
    });
  }

  @override
  Future<TradingAccount?> getAccount(String id) async {
    final rows = await (await _db.database).query(
      'trading_accounts', where: 'id = ?', whereArgs: [id], limit: 1,
    );
    return rows.isEmpty ? null : TradingAccount.fromMap(rows.first);
  }

  @override
  Future<List<TradingAccount>> listAccounts({
    bool includeArchived = false,
  }) async {
    final rows = await (await _db.database).query(
      'trading_accounts',
      where: includeArchived ? null : 'is_archived = 0',
      orderBy: 'name COLLATE NOCASE',
    );
    return rows.map(TradingAccount.fromMap).toList(growable: false);
  }

  @override
  Future<void> archiveAccount(String id, bool archived, DateTime at) async {
    final changed = await (await _db.database).update(
      'trading_accounts',
      {
        'is_archived': archived ? 1 : 0,
        'updated_at': at.toUtc().toIso8601String(),
      },
      where: 'id = ?',
      whereArgs: [id],
    );
    if (changed == 0) throw StateError('Account not found: $id');
  }

  @override
  Future<void> deleteAccount(String id) async {
    await (await _db.database).delete(
      'trading_accounts', where: 'id = ?', whereArgs: [id],
    );
  }

  @override
  Future<String?> getSetting(String key) async {
    final rows = await (await _db.database).query(
      'app_settings', columns: ['value'], where: 'key = ?', whereArgs: [key],
      limit: 1,
    );
    return rows.isEmpty ? null : rows.first['value'] as String;
  }

  @override
  Future<void> setSetting(String key, String value, DateTime at) async {
    if (key.trim().isEmpty) throw ArgumentError('Setting key is required.');
    await (await _db.database).insert(
      'app_settings',
      {
        'key': key.trim(),
        'value': value,
        'updated_at': at.toUtc().toIso8601String(),
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  @override
  Future<Map<String, String>> getSettings() async {
    final rows = await (await _db.database).query('app_settings');
    return {
      for (final row in rows)
        row['key'] as String: row['value'] as String,
    };
  }

  @override
  Future<void> deleteSetting(String key) async {
    await (await _db.database).delete(
      'app_settings', where: 'key = ?', whereArgs: [key],
    );
  }

  @override
  Future<void> appendAuditEvent(AuditLogEntry event) async {
    if (event.id.trim().isEmpty ||
        event.eventType.trim().isEmpty ||
        event.entityType.trim().isEmpty ||
        event.entityId.trim().isEmpty) {
      throw ArgumentError('Audit event identifiers and type are required.');
    }
    final decoded = jsonDecode(event.detailsJson);
    if (decoded is! Map<String, dynamic>) {
      throw ArgumentError('Audit event details must be a JSON object.');
    }
    await (await _db.database).insert(
      'audit_events',
      {
        'id': event.id,
        'event_type': event.eventType,
        'entity_type': event.entityType,
        'entity_id': event.entityId,
        'details_json': event.detailsJson,
        'occurred_at': event.occurredAt.toUtc().toIso8601String(),
      },
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }

  @override
  Future<List<AuditLogEntry>> getAuditEvents({
    DateTime? from,
    DateTime? to,
    String? entityType,
    int limit = 200,
  }) async {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('Audit end date must not precede start date.');
    }
    if (limit < 1 || limit > 5000) {
      throw ArgumentError.value(limit, 'limit');
    }
    final where = <String>[];
    final args = <Object?>[];
    if (from != null) {
      where.add('occurred_at >= ?');
      args.add(from.toUtc().toIso8601String());
    }
    if (to != null) {
      where.add('occurred_at <= ?');
      args.add(to.toUtc().toIso8601String());
    }
    if (entityType != null && entityType.trim().isNotEmpty) {
      where.add('entity_type = ?');
      args.add(entityType.trim());
    }
    final rows = await (await _db.database).query(
      'audit_events',
      where: where.isEmpty ? null : where.join(' AND '),
      whereArgs: args.isEmpty ? null : args,
      orderBy: 'occurred_at DESC',
      limit: limit,
    );
    return rows.map((row) => AuditLogEntry(
      id: row['id'] as String,
      eventType: row['event_type'] as String,
      entityType: row['entity_type'] as String,
      entityId: row['entity_id'] as String,
      detailsJson: row['details_json'] as String,
      occurredAt: DateTime.parse(row['occurred_at'] as String).toLocal(),
    )).toList(growable: false);
  }
}
