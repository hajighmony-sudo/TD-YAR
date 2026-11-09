import '../../data/repositories/trade_repository.dart';
import '../models/performance_report.dart';
import '../models/trade_execution.dart';
import '../models/trade.dart';
import 'performance_analytics.dart';

class PerformanceAnalyticsService {
  PerformanceAnalyticsService({
    required TradeRepository tradeRepository,
    PerformanceAnalytics analytics = const PerformanceAnalytics(),
  })  : _tradeRepository = tradeRepository,
        _analytics = analytics;

  final TradeRepository _tradeRepository;
  final PerformanceAnalytics _analytics;

  Future<PerformanceReport> buildReport({
    DateTime? from,
    DateTime? to,
    String? symbol,
    String? accountId,
    String? strategyId,
    TradeDirection? direction,
  }) async {
    final trades = await _tradeRepository.getTrades(
      symbol: symbol,
      accountId: accountId,
      strategyId: strategyId,
      direction: direction,
    );
    final executions = await Future.wait(
      trades.map((trade) => _tradeRepository.getExecutions(trade.id)),
    );
    final byTrade = <String, List<TradeExecution>>{};
    for (var index = 0; index < trades.length; index++) {
      byTrade[trades[index].id] = executions[index];
    }
    return _analytics.analyze(
      trades: trades,
      executionsByTrade: byTrade,
      from: from,
      to: to,
    );
  }
}
