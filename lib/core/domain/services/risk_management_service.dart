import '../../data/repositories/risk_repository.dart';
import '../../data/repositories/trade_repository.dart';
import '../models/risk_profile.dart';
import '../models/trade_execution.dart';
import 'performance_analytics_service.dart';
import 'risk_calculator.dart';

/// Coordinates daily risk checks from the user's local journal records.
class RiskManagementService {
  const RiskManagementService({
    required RiskRepository riskRepository,
    required TradeRepository tradeRepository,
    required PerformanceAnalyticsService performanceService,
  })  : _riskRepository = riskRepository,
        _tradeRepository = tradeRepository,
        _performanceService = performanceService;

  final RiskRepository _riskRepository;
  final TradeRepository _tradeRepository;
  final PerformanceAnalyticsService _performanceService;

  Future<RiskLimitCheck> evaluateDay({
    required String riskProfileId,
    required DateTime date,
    double nextPlannedRiskAmount = 0,
  }) async {
    final RiskProfile? profile =
        await _riskRepository.getProfile(riskProfileId);
    if (profile == null) {
      throw StateError('Risk profile not found: $riskProfileId');
    }

    final start = DateTime(date.year, date.month, date.day);
    final end = DateTime(start.year, start.month, start.day + 1)
        .subtract(const Duration(microseconds: 1));
    final report = await _performanceService.buildReport(
      from: start,
      to: end,
      accountId: profile.accountId,
    );

    // Count unique trades with at least one entry fill on the selected day,
    // not just trades that happened to be created in the app on that day.
    final trades = await _tradeRepository.getTrades(accountId: profile.accountId);
    final executionLists = await Future.wait(
      trades.map((trade) => _tradeRepository.getExecutions(trade.id)),
    );
    var tradesTakenToday = 0;
    for (final executions in executionLists) {
      if (executions.any((execution) =>
          execution.kind == ExecutionKind.entry &&
          !execution.executedAt.isBefore(start) &&
          !execution.executedAt.isAfter(end))) {
        tradesTakenToday++;
      }
    }

    // Sum losing exit fills independently: wins do not erase daily losses.
    final realizedDailyLoss = report.realizations
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
