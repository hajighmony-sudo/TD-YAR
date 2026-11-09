import '../models/performance_report.dart';
import '../models/trade.dart';
import '../models/trade_execution.dart';
import 'trade_calculator.dart';

class PerformanceAnalytics {
  const PerformanceAnalytics();

  PerformanceReport analyze({
    required List<Trade> trades,
    required Map<String, List<TradeExecution>> executionsByTrade,
    DateTime? from,
    DateTime? to,
  }) {
    final outcomes = <TradeOutcome>[];
    for (final trade in trades) {
      final executions = executionsByTrade[trade.id] ?? const <TradeExecution>[];
      if (executions.isEmpty) continue;
      final metrics = TradeCalculator.calculate(
        trade: trade, executions: executions,
      );
      if (metrics.closedQuantity <= 0) continue;
      final exits = executions
          .where((item) => item.kind == ExecutionKind.exit)
          .toList()
        ..sort((a, b) => a.executedAt.compareTo(b.executedAt));
      if (exits.isEmpty) continue;
      final closedAt = exits.last.executedAt;
      if (from != null && closedAt.isBefore(from)) continue;
      if (to != null && closedAt.isAfter(to)) continue;
      final entries = executions
          .where((item) => item.kind == ExecutionKind.entry)
          .toList()
        ..sort((a, b) => a.executedAt.compareTo(b.executedAt));
      final openedAt = entries.isEmpty ? closedAt : entries.first.executedAt;
      outcomes.add(TradeOutcome(
        tradeId: trade.id,
        symbol: trade.symbol,
        direction: trade.direction,
        strategyId: trade.strategyId,
        closedAt: closedAt,
        netPnl: metrics.netRealizedPnl,
        grossPnl: metrics.grossRealizedPnl,
        fees: metrics.feesAllocatedToClosedQuantity,
        closedQuantity: metrics.closedQuantity,
        duration: closedAt.difference(openedAt),
      ));
    }
    outcomes.sort((a, b) => a.closedAt.compareTo(b.closedAt));

    final wins = outcomes.where((item) => item.isWin).toList();
    final losses = outcomes.where((item) => item.isLoss).toList();
    final net = _sum(outcomes.map((item) => item.netPnl));
    final gross = _sum(outcomes.map((item) => item.grossPnl));
    final fees = _sum(outcomes.map((item) => item.fees));
    final grossWins = _sum(wins.map((item) => item.netPnl));
    final grossLosses = _sum(losses.map((item) => item.netPnl.abs()));
    final maxDrawdown = _maxDrawdown(outcomes);
    final totalDuration = outcomes.fold<int>(
      0, (sum, item) => sum + item.duration.inSeconds,
    );

    return PerformanceReport(
      outcomes: List.unmodifiable(outcomes),
      totalTrades: outcomes.length,
      wins: wins.length,
      losses: losses.length,
      breakeven: outcomes.where((item) => item.isBreakeven).length,
      netPnl: net,
      grossPnl: gross,
      totalFees: fees,
      winRate: outcomes.isEmpty ? 0 : wins.length / outcomes.length * 100,
      averageWin: wins.isEmpty ? 0 : grossWins / wins.length,
      averageLoss: losses.isEmpty ? 0 : -grossLosses / losses.length,
      profitFactor: grossLosses == 0
          ? (grossWins == 0 ? null : double.infinity)
          : grossWins / grossLosses,
      expectancy: outcomes.isEmpty ? 0 : net / outcomes.length,
      maxDrawdown: maxDrawdown,
      averageDuration: outcomes.isEmpty
          ? Duration.zero
          : Duration(seconds: totalDuration ~/ outcomes.length),
      bySymbol: _breakdown(outcomes, (item) => item.symbol),
      byStrategy: _breakdown(outcomes, (item) => item.strategyId ?? 'بدون استراتژی'),
      byWeekday: _breakdown(outcomes, (item) => _weekday(item.closedAt.weekday)),
      byHour: _breakdown(outcomes, (item) => '${item.closedAt.hour.toString().padLeft(2, '0')}:00'),
    );
  }

  static double _sum(Iterable<double> values) =>
      values.fold<double>(0, (sum, value) => sum + value);

  static double _maxDrawdown(List<TradeOutcome> outcomes) {
    var equity = 0.0;
    var peak = 0.0;
    var maxDrawdown = 0.0;
    for (final outcome in outcomes) {
      equity += outcome.netPnl;
      if (equity > peak) peak = equity;
      final drawdown = peak - equity;
      if (drawdown > maxDrawdown) maxDrawdown = drawdown;
    }
    return maxDrawdown;
  }

  static List<PerformanceBreakdown> _breakdown(
    List<TradeOutcome> outcomes,
    String Function(TradeOutcome) keyOf,
  ) {
    final groups = <String, List<TradeOutcome>>{};
    for (final outcome in outcomes) {
      groups.putIfAbsent(keyOf(outcome), () => []).add(outcome);
    }
    final result = groups.entries.map((entry) {
      final values = entry.value;
      return PerformanceBreakdown(
        key: entry.key,
        tradeCount: values.length,
        netPnl: _sum(values.map((item) => item.netPnl)),
        winCount: values.where((item) => item.isWin).length,
        lossCount: values.where((item) => item.isLoss).length,
      );
    }).toList();
    result.sort((a, b) => b.netPnl.compareTo(a.netPnl));
    return List.unmodifiable(result);
  }

  static String _weekday(int day) => const [
        'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه',
        'جمعه', 'شنبه', 'یکشنبه',
      ][day - 1];
}
