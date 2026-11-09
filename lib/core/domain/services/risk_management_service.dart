import '../../data/repositories/risk_repository.dart';
import '../models/performance_report.dart';
import '../models/risk_profile.dart';
import 'risk_calculator.dart';

class RiskManagementService {
  const RiskManagementService(this._repository);

  final RiskRepository _repository;

  Future<RiskLimitCheck> evaluateDay({
    required String riskProfileId,
    required DateTime date,
    required PerformanceReport dailyPerformance,
    required int tradesTakenToday,
    double nextPlannedRiskAmount = 0,
  }) async {
    final RiskProfile? profile = await _repository.getProfile(riskProfileId);
    if (profile == null) {
      throw StateError('Risk profile not found: $riskProfileId');
    }
    final realizedDailyLoss =
        dailyPerformance.netPnl < 0 ? dailyPerformance.netPnl.abs() : 0.0;
    return RiskCalculator.checkLimits(
      profile: profile,
      plannedRiskAmount: nextPlannedRiskAmount,
      realizedDailyLoss: realizedDailyLoss,
      tradesTakenToday: tradesTakenToday,
    );
  }
}
