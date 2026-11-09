import '../../domain/models/risk_profile.dart';
import '../../domain/models/trade_risk_record.dart';

abstract interface class RiskRepository {
  Future<void> saveProfile(RiskProfile profile);
  Future<RiskProfile?> getProfile(String id);
  Future<List<RiskProfile>> getProfiles();
  Future<void> deleteProfile(String id);
  Future<void> saveTradeRisk(TradeRiskRecord record);
  Future<TradeRiskRecord?> getTradeRisk(String tradeId);
  Future<void> deleteTradeRisk(String tradeId);
}
