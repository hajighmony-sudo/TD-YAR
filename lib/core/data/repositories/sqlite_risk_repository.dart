import 'package:sqflite/sqflite.dart';

import '../../domain/models/risk_profile.dart';
import '../../domain/models/trade_risk_record.dart';
import '../local/app_database.dart';
import 'risk_repository.dart';

class SqliteRiskRepository implements RiskRepository {
  SqliteRiskRepository({AppDatabase? database})
      : _database = database ?? AppDatabase.instance;

  final AppDatabase _database;
  Future<Database> get _db => _database.database;

  @override
  Future<void> saveProfile(RiskProfile profile) async {
    if (profile.name.trim().isEmpty ||
        profile.accountBalance <= 0 ||
        profile.maxRiskPerTradePercent < 0 ||
        profile.maxDailyLossPercent < 0 ||
        profile.maxTradesPerDay < 0 ||
        profile.currency.trim().isEmpty) {
      throw ArgumentError('Risk profile contains invalid values.');
    }
    await (await _db).insert(
      'risk_profiles', profile.toMap(),
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
    await (await _db).delete('risk_profiles', where: 'id = ?', whereArgs: [id]);
  }

  @override
  Future<void> saveTradeRisk(TradeRiskRecord record) async {
    await (await _db).insert(
      'trade_risk_records', record.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
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
