import 'trade.dart';

/// A single manually recorded exit fill matched to its FIFO entry lots.
class TradeRealization {
  const TradeRealization({
    required this.tradeId,
    required this.executionId,
    required this.symbol,
    required this.direction,
    required this.strategyId,
    required this.closedAt,
    this.accountLabel,
    required this.netPnl,
    required this.grossPnl,
    required this.fees,
    required this.closedQuantity,
    required this.duration,
  });

  final String tradeId;
  final String executionId;
  final String symbol;
  final TradeDirection direction;
  final String? strategyId;
  final String? accountLabel;
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
