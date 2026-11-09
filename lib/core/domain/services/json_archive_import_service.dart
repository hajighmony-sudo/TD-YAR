import 'dart:convert';

import 'package:sqflite/sqflite.dart';

import '../../data/local/app_database.dart';
import '../models/trade.dart';
import '../models/trade_execution.dart';
import 'trade_calculator.dart';

/// Imports a TD-YAR JSON archive without replacing existing user records.
///
/// Every row is inserted in one SQLite transaction. Any malformed row,
/// duplicate primary key, missing relationship, or SQLite constraint failure
/// rolls the entire import back. Existing IDs are never overwritten.
class JsonArchiveImportService {
  JsonArchiveImportService({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;

  Future<ArchiveImportSummary> importJson(String source) async {
    final decoded = jsonDecode(source);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Archive root must be a JSON object.');
    }
    if (decoded['format'] != 'td-yar-export' ||
        decoded['schema_version'] != 2) {
      throw const FormatException('Unsupported TD-YAR archive format/version.');
    }

    final accounts = _mapList(decoded, 'accounts');
    final strategies = _mapList(decoded, 'strategies');
    final playbooks = _mapList(decoded, 'playbooks');
    final attachments = _mapList(decoded, 'attachments');
    final trades = _mapList(decoded, 'trades');
    final journals = _mapList(decoded, 'journal_entries');
    final psychology = _mapList(decoded, 'psychology_entries');
    final riskProfiles = _mapList(decoded, 'risk_profiles');
    final tradeRisks = _mapList(decoded, 'trade_risk_records');
    final learning = _mapList(decoded, 'learning_resources');
    final sessions = _mapList(decoded, 'trading_sessions');
    final auditEvents = _mapList(decoded, 'audit_events');
    final settings = decoded['settings'];
    if (settings is! Map<String, dynamic>) {
      throw const FormatException('Archive settings must be a JSON object.');
    }

    final tradeRows = <Map<String, Object?>>[];
    final executionRows = <Map<String, Object?>>[];
    final tradeIds = <String>{};
    for (final item in trades) {
      final rawTrade = item['trade'];
      final rawExecutions = item['executions'];
      if (rawTrade is! Map<String, dynamic> ||
          rawExecutions is! List<dynamic>) {
        throw const FormatException('Each trade must contain trade and executions.');
      }
      final tradeMap = Map<String, Object?>.from(rawTrade);
      final trade = Trade.fromMap(tradeMap);
      if (trade.id.trim().isEmpty || !tradeIds.add(trade.id)) {
        throw FormatException('Duplicate or empty trade id: ${trade.id}');
      }
      final fills = rawExecutions.map((value) {
        if (value is! Map<String, dynamic>) {
          throw const FormatException('Execution rows must be JSON objects.');
        }
        return TradeExecution.fromMap(Map<String, Object?>.from(value));
      }).toList(growable: false);
      if (fills.any((fill) => fill.tradeId != trade.id)) {
        throw FormatException('Execution belongs to a different trade: ${trade.id}');
      }
      TradeCalculator.calculate(trade: trade, executions: fills);
      tradeRows.add(tradeMap);
      executionRows.addAll(fills.map((fill) => fill.toMap()));
    }

    final strategyIds = strategies.map((row) => _requiredString(row, 'id')).toSet();
    final accountIds = accounts.map((row) => _requiredString(row, 'id')).toSet();
    final db = await _database.database;

    return db.transaction((txn) async {
      // Insert referenced parent records before their dependants.
      await _insertAll(txn, 'trading_accounts', accounts);
      await _insertAll(txn, 'strategies', strategies);
      await _insertAll(txn, 'playbooks', playbooks);

      for (final row in tradeRows) {
        final accountId = row['account_id'] as String?;
        final strategyId = row['strategy_id'] as String?;
        if (accountId != null &&
            !accountIds.contains(accountId) &&
            !await _exists(txn, 'trading_accounts', accountId)) {
          throw StateError('Trading account not found in archive or database: $accountId');
        }
        if (strategyId != null &&
            !strategyIds.contains(strategyId) &&
            !await _exists(txn, 'strategies', strategyId)) {
          throw StateError('Strategy not found in archive or database: $strategyId');
        }
      }

      await _insertAll(txn, 'trades', tradeRows);
      await _insertAll(txn, 'trade_executions', executionRows);

      final knownTradeIds = <String>{...tradeIds};
      final knownStrategyIds = <String>{...strategyIds};
      for (final row in journals) {
        final linked = _decodeStringList(row['linked_trade_ids_json'], 'journal trade links');
        for (final id in linked) {
          if (!knownTradeIds.contains(id) && !await _exists(txn, 'trades', id)) {
            throw StateError('Journal references missing trade: $id');
          }
        }
      }
      await _insertAll(txn, 'journal_entries', journals);

      for (final row in psychology) {
        final tradeId = row['trade_id'] as String?;
        if (tradeId != null &&
            !knownTradeIds.contains(tradeId) &&
            !await _exists(txn, 'trades', tradeId)) {
          throw StateError('Psychology entry references missing trade: $tradeId');
        }
      }
      await _insertAll(txn, 'psychology_entries', psychology);
      await _insertAll(txn, 'risk_profiles', riskProfiles);

      for (final row in tradeRisks) {
        final tradeId = _requiredString(row, 'trade_id');
        if (!knownTradeIds.contains(tradeId) &&
            !await _exists(txn, 'trades', tradeId)) {
          throw StateError('Risk record references missing trade: $tradeId');
        }
      }
      await _insertAll(txn, 'trade_risk_records', tradeRisks);
      await _insertAll(txn, 'trading_sessions', sessions);

      for (final row in learning) {
        final rawIds = row['related_strategy_ids_json'];
        for (final id in _decodeStringList(rawIds, 'learning strategy links')) {
          if (!knownStrategyIds.contains(id) &&
              !await _exists(txn, 'strategies', id)) {
            throw StateError('Learning resource references missing strategy: $id');
          }
        }
      }
      await _insertAll(txn, 'learning_resources', learning);
      await _insertAll(txn, 'attachments', attachments);

      for (final entry in settings.entries) {
        if (entry.key.trim().isEmpty || entry.value is! String) {
          throw const FormatException('Setting keys and values must be strings.');
        }
        await txn.insert(
          'app_settings',
          {
            'key': entry.key,
            'value': entry.value,
            'updated_at': DateTime.now().toUtc().toIso8601String(),
          },
          conflictAlgorithm: ConflictAlgorithm.abort,
        );
      }
      await _insertAll(txn, 'audit_events', auditEvents);

      return ArchiveImportSummary(
        accounts: accounts.length,
        strategies: strategies.length,
        playbooks: playbooks.length,
        attachments: attachments.length,
        trades: tradeRows.length,
        executions: executionRows.length,
        journalEntries: journals.length,
        psychologyEntries: psychology.length,
        riskProfiles: riskProfiles.length,
        tradeRiskRecords: tradeRisks.length,
        learningResources: learning.length,
        tradingSessions: sessions.length,
        settings: settings.length,
        auditEvents: auditEvents.length,
      );
    });
  }

  static List<Map<String, Object?>> _mapList(
    Map<String, dynamic> archive,
    String key,
  ) {
    final value = archive[key] ?? const <dynamic>[];
    if (value is! List<dynamic>) {
      throw FormatException('Archive field "$key" must be a list.');
    }
    return value.map((item) {
      if (item is! Map<String, dynamic>) {
        throw FormatException('Archive field "$key" must contain objects.');
      }
      return Map<String, Object?>.from(item);
    }).toList(growable: false);
  }

  static String _requiredString(Map<String, Object?> row, String key) {
    final value = row[key];
    if (value is! String || value.trim().isEmpty) {
      throw FormatException('Archive row requires a non-empty "$key".');
    }
    return value;
  }

  static List<String> _decodeStringList(Object? value, String label) {
    if (value is! String) throw FormatException('$label must be JSON text.');
    final decoded = jsonDecode(value);
    if (decoded is! List<dynamic> || decoded.any((item) => item is! String)) {
      throw FormatException('$label must be a JSON string list.');
    }
    return decoded.cast<String>();
  }

  static Future<bool> _exists(
    Transaction txn,
    String table,
    String id,
  ) async {
    // Table names are internal constants at each call site, never user input.
    final rows = await txn.query(
      table,
      columns: ['id'],
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    return rows.isNotEmpty;
  }

  static Future<void> _insertAll(
    Transaction txn,
    String table,
    List<Map<String, Object?>> rows,
  ) async {
    for (final row in rows) {
      await txn.insert(
        table,
        row,
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    }
  }
}

class ArchiveImportSummary {
  const ArchiveImportSummary({
    required this.accounts,
    required this.strategies,
    required this.playbooks,
    required this.attachments,
    required this.trades,
    required this.executions,
    required this.journalEntries,
    required this.psychologyEntries,
    required this.riskProfiles,
    required this.tradeRiskRecords,
    required this.learningResources,
    required this.tradingSessions,
    required this.settings,
    required this.auditEvents,
  });

  final int accounts;
  final int strategies;
  final int playbooks;
  final int attachments;
  final int trades;
  final int executions;
  final int journalEntries;
  final int psychologyEntries;
  final int riskProfiles;
  final int tradeRiskRecords;
  final int learningResources;
  final int tradingSessions;
  final int settings;
  final int auditEvents;
}
