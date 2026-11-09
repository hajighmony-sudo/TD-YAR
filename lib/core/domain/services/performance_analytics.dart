import '../models/performance_report.dart';
import '../models/trade.dart';
import '../models/trade_execution.dart';
import '../models/trade_realization.dart';
import 'trade_calculator.dart';

class PerformanceAnalytics {
  const PerformanceAnalytics();

  PerformanceReport analyze({
    required List<Trade> trades,
    required Map<String, List<TradeExecution>> executionsByTrade,
    DateTime? from,
    DateTime? to,
  }) {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('Report end must not precede report start.');
    }

    final allRealizations = <TradeRealization>[];
    final outcomes = <TradeOutcome>[];

    for (final trade in trades) {
      final executions = executionsByTrade[trade.id] ?? const <TradeExecution>[];
      if (executions.isEmpty) continue;

      // Validate the full execution history, including any exits outside range.
      TradeCalculator.calculate(trade: trade, executions: executions);
      final ordered = [...executions]..sort(_compareExecutions);
      final entries = ordered.where((e) => e.kind == ExecutionKind.entry).toList();
      if (entries.isEmpty) continue;
      final openedAt = entries.first.executedAt;

      var previousGross = 0.0;
      var previousFees = 0.0;
      var previousClosedQuantity = 0.0;
      final prefix = <TradeExecution>[];

      for (final execution in ordered) {
        prefix.add(execution);
        if (execution.kind != ExecutionKind.exit) continue;
        final cumulative = TradeCalculator.calculate(
          trade: trade,
          executions: prefix,
        );
        final gross = cumulative.grossRealizedPnl - previousGross;
        final fees =
            cumulative.feesAllocatedToClosedQuantity - previousFees;
        final quantity = cumulative.closedQuantity - previousClosedQuantity;
        final realization = TradeRealization(
          tradeId: trade.id,
          symbol: trade.symbol,
          direction: trade.direction,
          strategyId: trade.strategyId,
          closedAt: execution.executedAt,
          netPnl: gross - fees,
          grossPnl: gross,
          fees: fees,
          closedQuantity: quantity,
          duration: execution.executedAt.difference(openedAt),
        );
        allRealizations.add(realization);
        previousGross = cumulative.grossRealizedPnl;
        previousFees = cumulative.feesAllocatedToClosedQuantity;
        previousClosedQuantity = cumulative.closedQuantity;
      }
    }

    allRealizations.sort((a, b) => a.closedAt.compareTo(b.closedAt));
    final filtered = allRealizations.where((item) {
      if (from != null && item.closedAt.isBefore(from)) return false;
      if (to != null && item.closedAt.isAfter(to)) return false;
      return true;
    }).toList(growable: false);

    final grouped = <String, List<TradeRealization>>{};
    for (final realization in filtered) {
      grouped.putIfAbsent(realization.tradeId, () => []).add(realization);
    }
    for (final group in grouped.values) {
      final last = group.last;
      outcomes.add(TradeOutcome(
        tradeId: last.tradeId,
        symbol: last.symbol,
        direction: last.direction,
        strategyId: last.strategyId,
        closedAt: last.closedAt,
        netPnl: _sum(group.map((item) => item.netPnl)),
        grossPnl: _sum(group.map((item) => item.grossPnl)),
        fees: _sum(group.map((item) => item.fees)),
        closedQuantity: _sum(group.map((item) => item.closedQuantity)),
        duration: last.duration,
      ));
    }
    outcomes.sort((a, b) => a.closedAt.compareTo(b.closedAt));

    final wins = outcomes.where((item) => item.isWin).toList();
    final losses = outcomes.where((item) => item.isLoss).toList();
    final net = _sum(outcomes.map((item) => item.netPnl));
    final gross = _sum(outcomes.map((item) => item.grossPnl));
    final fees = _sum(outcomes.map((item) => item.fees));
    final positive = _sum(wins.map((item) => item.netPnl));
    final negative = _sum(losses.map((item) => item.netPnl.abs()));
    final durationSeconds = outcomes.fold<int>(
      0, (sum, item) => sum + item.duration.inSeconds,
    );

    return PerformanceReport(
      outcomes: List.unmodifiable(outcomes),
      realizations: List.unmodifiable(filtered),
      totalTrades: outcomes.length,
      wins: wins.length,
      losses: losses.length,
      breakeven: outcomes.where((item) => item.isBreakeven).length,
      netPnl: net,
      grossPnl: gross,
      totalFees: fees,
      winRate: outcomes.isEmpty ? 0 : wins.length / outcomes.length * 100,
      averageWin: wins.isEmpty ? 0 : positive / wins.length,
      averageLoss: losses.isEmpty ? 0 : -negative / losses.length,
      profitFactor: negative == 0 ? null : positive / negative,
      expectancy: outcomes.isEmpty ? 0 : net / outcomes.length,
      maxDrawdown: _maxDrawdown(outcomes),
      averageDuration: outcomes.isEmpty
          ? Duration.zero
          : Duration(seconds: durationSeconds ~/ outcomes.length),
      bySymbol: _breakdown(outcomes, (item) => item.symbol),
      byStrategy: _breakdown(
        outcomes, (item) => item.strategyId ?? 'بدون استراتژی',
      ),
      byWeekday: _breakdown(outcomes, (item) => _weekday(item.closedAt.weekday)),
      byHour: _breakdown(
        outcomes,
        (item) => '${item.closedAt.hour.toString().padLeft(2, '0')}:00',
      ),
    );
  }

  static int _compareExecutions(TradeExecution a, TradeExecution b) {
    final byTime = a.executedAt.compareTo(b.executedAt);
    if (byTime != 0) return byTime;
    if (a.kind != b.kind) {
      return a.kind == ExecutionKind.entry ? -1 : 1;
    }
    return a.id.compareTo(b.id);
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
