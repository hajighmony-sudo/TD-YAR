import '../../data/repositories/risk_repository.dart';
import '../models/performance_report.dart';
import '../models/risk_profile.dart';
import 'risk_calculator.dart';

class RiskManagementService {
  const RiskManagementService(this._repository);

  final RiskRepository _repository;

  Future<RiskLimitCheck> evaluateDay({
    required String riskProfileId,
    required PerformanceReport dailyPerformance,
    required int tradesTakenToday,
    double nextPlannedRiskAmount = 0,
  }) async {
    final RiskProfile? profile = await _repository.getProfile(riskProfileId);
    if (profile == null) {
      throw StateError('Risk profile not found: $riskProfileId');
    }

    // A profitable trade must not erase a separate losing trade when
    // evaluating a daily loss limit. Sum losing realizations independently.
    final realizedDailyLoss = dailyPerformance.realizations.isNotEmpty
        ? dailyPerformance.realizations
            .where((item) => item.netPnl < 0)
            .fold<double>(0, (sum, item) => sum + item.netPnl.abs())
        : dailyPerformance.outcomes
            .where((item) => item.netPnl < 0)
            .fold<double>(0, (sum, item) => sum + item.netPnl.abs());

    return RiskCalculator.checkLimits(
      profile: profile,
      plannedRiskAmount: nextPlannedRiskAmount,
      realizedDailyLoss: realizedDailyLoss,
      tradesTakenToday: tradesTakenToday,
    );
  }
}
