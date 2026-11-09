import 'package:sqflite/sqflite.dart';

import '../../domain/models/risk_profile.dart';
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
    await (await _db).insert(
      'risk_profiles',
      profile.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
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
    _validateTradeRisk(record);
    await (await _db).insert(
      'trade_risk_records',
      record.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  void _validateTradeRisk(TradeRiskRecord record) {
    final values = [
      record.entryPrice, record.stopPrice, record.targetPrice, record.quantity,
      record.contractMultiplier, record.accountBalanceSnapshot,
      record.plannedRiskAmount, record.plannedRewardAmount,
      record.riskRewardRatio, record.riskPercent,
    ];
    if (record.id.trim().isEmpty || record.tradeId.trim().isEmpty ||
        values.any((value) => !value.isFinite || value <= 0)) {
      throw ArgumentError('Trade risk record contains invalid values.');
    }
    final risk = (record.entryPrice - record.stopPrice).abs() *
        record.quantity * record.contractMultiplier;
    final reward = (record.targetPrice - record.entryPrice).abs() *
        record.quantity * record.contractMultiplier;
    final tolerance = 1e-8;
    if ((risk - record.plannedRiskAmount).abs() >
            tolerance * (1 + risk) ||
        (reward - record.plannedRewardAmount).abs() >
            tolerance * (1 + reward) ||
        (reward / risk - record.riskRewardRatio).abs() >
            tolerance * (1 + record.riskRewardRatio) ||
        (risk / record.accountBalanceSnapshot * 100 - record.riskPercent).abs() >
            tolerance * (1 + record.riskPercent)) {
      throw ArgumentError('Trade risk record calculations are inconsistent.');
    }
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
