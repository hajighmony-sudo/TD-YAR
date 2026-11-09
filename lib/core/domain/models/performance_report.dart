import 'trade.dart';
import 'trade_realization.dart';

class TradeOutcome {
  const TradeOutcome({
    required this.tradeId,
    required this.symbol,
    required this.direction,
    required this.strategyId,
    required this.closedAt,
    required this.netPnl,
    required this.grossPnl,
    required this.fees,
    required this.closedQuantity,
    required this.duration,
  });

  final String tradeId;
  final String symbol;
  final TradeDirection direction;
  final String? strategyId;
  final DateTime closedAt;
  final double netPnl;
  final double grossPnl;
  final double fees;
  final double closedQuantity;
  final Duration duration;

  bool get isWin => netPnl > 0;
  bool get isLoss => netPnl < 0;
  bool get isBreakeven => netPnl == 0;
}

class PerformanceBreakdown {
  const PerformanceBreakdown({
    required this.key,
    required this.tradeCount,
    required this.netPnl,
    required this.winCount,
    required this.lossCount,
  });

  final String key;
  final int tradeCount;
  final double netPnl;
  final int winCount;
  final int lossCount;

  double get winRate => tradeCount == 0 ? 0 : winCount / tradeCount * 100;
}

class PerformanceReport {
  const PerformanceReport({
    required this.outcomes,
    required this.realizations,
    required this.totalTrades,
    required this.wins,
    required this.losses,
    required this.breakeven,
    required this.netPnl,
    required this.grossPnl,
    required this.totalFees,
    required this.winRate,
    required this.averageWin,
    required this.averageLoss,
    required this.profitFactor,
    required this.expectancy,
    required this.maxDrawdown,
    required this.averageDuration,
    required this.bySymbol,
    required this.byStrategy,
    required this.byWeekday,
    required this.byHour,
  });

  /// One aggregate outcome per trade in the requested reporting window.
  final List<TradeOutcome> outcomes;

  /// One record per exit fill, preserving the date of each partial realization.
  final List<TradeRealization> realizations;

  final int totalTrades;
  final int wins;
  final int losses;
  final int breakeven;
  final double netPnl;
  final double grossPnl;
  final double totalFees;
  final double winRate;
  final double averageWin;
  final double averageLoss;
  final double? profitFactor;
  final double expectancy;
  final double maxDrawdown;
  final Duration averageDuration;
  final List<PerformanceBreakdown> bySymbol;
  final List<PerformanceBreakdown> byStrategy;
  final List<PerformanceBreakdown> byWeekday;
  final List<PerformanceBreakdown> byHour;

  double get averageTrade => totalTrades == 0 ? 0 : netPnl / totalTrades;
  double get grossProfit =>
      outcomes.where((item) => item.netPnl > 0).fold<double>(
        0, (sum, item) => sum + item.netPnl,
      );
  double get grossLoss =>
      outcomes.where((item) => item.netPnl < 0).fold<double>(
        0, (sum, item) => sum + item.netPnl.abs(),
      );
  double get recoveryFactor =>
      maxDrawdown == 0 ? (netPnl > 0 ? double.infinity : 0) : netPnl / maxDrawdown;
}
