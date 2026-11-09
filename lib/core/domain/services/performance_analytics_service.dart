import '../../data/repositories/trade_repository.dart';
import '../models/performance_report.dart';
import '../models/performance_candle.dart';
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

  Future<List<PerformanceCandle>> buildCandles({
    required PerformanceCandleTimeframe timeframe,
    DateTime? from,
    DateTime? to,
    String? accountId,
    String? strategyId,
    TradeDirection? direction,
  }) async {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('Report end date must not precede start date.');
    }
    final report = await buildReport(
      from: from,
      to: to,
      accountId: accountId,
      strategyId: strategyId,
      direction: direction,
    );
    final realizations = [...report.realizations]
      ..sort((a, b) => a.closedAt.compareTo(b.closedAt));

    var equity = 0.0;
    final buckets = <DateTime, _CandleAccumulator>{};
    for (final realization in realizations) {
      final periodStart = _periodStart(realization.closedAt, timeframe);
      final bucket = buckets.putIfAbsent(
        periodStart,
        () => _CandleAccumulator(open: equity),
      );
      equity += realization.netPnl;
      bucket.add(realization.netPnl, equity);
    }

    final starts = buckets.keys.toList()..sort();
    return starts
        .map((start) => buckets[start]!.toCandle(start))
        .toList(growable: false);
  }

  static DateTime _periodStart(
    DateTime value,
    PerformanceCandleTimeframe timeframe,
  ) {
    switch (timeframe) {
      case PerformanceCandleTimeframe.daily:
        return DateTime(value.year, value.month, value.day);
      case PerformanceCandleTimeframe.weekly:
        final day = DateTime(value.year, value.month, value.day);
        return day.subtract(Duration(days: day.weekday - DateTime.monday));
      case PerformanceCandleTimeframe.yearly:
        return DateTime(value.year);
    }
  }

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

class _CandleAccumulator {
  _CandleAccumulator({required this.open})
      : high = open,
        low = open,
        close = open;

  final double open;
  double high;
  double low;
  double close;
  double netPnl = 0;
  int realizationCount = 0;
  int wins = 0;
  int losses = 0;
  int breakeven = 0;

  void add(double pnl, double equity) {
    netPnl += pnl;
    close = equity;
    if (equity > high) high = equity;
    if (equity < low) low = equity;
    realizationCount++;
    if (pnl > 0) {
      wins++;
    } else if (pnl < 0) {
      losses++;
    } else {
      breakeven++;
    }
  }

  PerformanceCandle toCandle(DateTime periodStart) => PerformanceCandle(
        periodStart: periodStart,
        open: open,
        high: high,
        low: low,
        close: close,
        netPnl: netPnl,
        realizationCount: realizationCount,
        wins: wins,
        losses: losses,
        breakeven: breakeven,
      );
}
