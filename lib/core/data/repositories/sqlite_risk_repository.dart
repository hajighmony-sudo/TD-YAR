import 'package:sqflite/sqflite.dart';

import '../../domain/models/risk_profile.dart';
import '../../domain/models/trade.dart';
import '../../domain/models/trade_risk_record.dart';
import '../../domain/services/risk_calculator.dart';
import '../local/app_database.dart';
import 'risk_repository.dart';

class SqliteRiskRepository implements RiskRepository {
  SqliteRiskRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  @override
  Future<void> saveProfile(RiskProfile profile) async {
    RiskCalculator.validateProfile(profile);
    final db = await _db;
    await db.transaction((txn) async {
      if (profile.accountId != null) {
        final accounts = await txn.query(
          'trading_accounts',
          columns: ['id'],
          where: 'id = ?',
          whereArgs: [profile.accountId],
          limit: 1,
        );
        if (accounts.isEmpty) {
          throw StateError('Risk profile account not found: ${profile.accountId}');
        }
      }
      await txn.insert(
        'risk_profiles', profile.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    });
  }

  @override
  Future<RiskProfile?> getProfile(String id) async {
    final rows = await (await _db).query(
      'risk_profiles', where: 'id = ?', whereArgs: [id], limit: 1,
    );
    return rows.isEmpty ? null : RiskProfile.fromMap(rows.first);
  }

  @override
  Future<List<RiskProfile>> getProfiles() async {
    final rows = await (await _db).query(
      'risk_profiles', orderBy: 'updated_at DESC',
    );
    return rows.map(RiskProfile.fromMap).toList(growable: false);
  }

  @override
  Future<void> deleteProfile(String id) async {
    await (await _db).delete(
      'risk_profiles', where: 'id = ?', whereArgs: [id],
    );
  }

  @override
  Future<void> saveTradeRisk(TradeRiskRecord record) async {
    final db = await _db;
    await db.transaction((txn) async {
      final rows = await txn.query(
        'trades', where: 'id = ?', whereArgs: [record.tradeId], limit: 1,
      );
      if (rows.isEmpty) throw StateError('Trade not found: ${record.tradeId}');
      final trade = Trade.fromMap(rows.first);
      final expected = RiskCalculator.createTradeRiskRecord(
        id: record.id,
        trade: trade,
        entryPrice: record.entryPrice,
        stopPrice: record.stopPrice,
        targetPrice: record.targetPrice,
        quantity: record.quantity,
        accountBalance: record.accountBalanceSnapshot,
        notes: record.notes,
        createdAt: record.createdAt,
      );
      const tolerance = 1e-8;
      if ((expected.plannedRiskAmount - record.plannedRiskAmount).abs() >
              tolerance * (1 + expected.plannedRiskAmount) ||
          (expected.plannedRewardAmount - record.plannedRewardAmount).abs() >
              tolerance * (1 + expected.plannedRewardAmount) ||
          (expected.riskRewardRatio - record.riskRewardRatio).abs() >
              tolerance * (1 + expected.riskRewardRatio) ||
          (expected.riskPercent - record.riskPercent).abs() >
              tolerance * (1 + expected.riskPercent) ||
          expected.contractMultiplier != record.contractMultiplier) {
        throw ArgumentError('Trade risk record calculations are inconsistent.');
      }
      await txn.insert(
        'trade_risk_records', record.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    });
  }

  @override
  Future<TradeRiskRecord?> getTradeRisk(String tradeId) async {
    final rows = await (await _db).query(
      'trade_risk_records',
      where: 'trade_id = ?', whereArgs: [tradeId], limit: 1,
    );
    return rows.isEmpty ? null : TradeRiskRecord.fromMap(rows.first);
  }

  @override
  Future<void> deleteTradeRisk(String tradeId) async {
    await (await _db).delete(
      'trade_risk_records', where: 'trade_id = ?', whereArgs: [tradeId],
    );
  }
}
