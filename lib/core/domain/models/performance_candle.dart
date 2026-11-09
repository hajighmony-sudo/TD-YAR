enum PerformanceCandleTimeframe { daily, weekly, yearly }

/// A realized-equity OHLC candle for one calendar period.
///
/// Values are cumulative realized net P&L within the requested report range,
/// not market prices or forecasts.
class PerformanceCandle {
  const PerformanceCandle({
    required this.periodStart,
    required this.open,
    required this.high,
    required this.low,
    required this.close,
    required this.netPnl,
    required this.realizationCount,
    required this.wins,
    required this.losses,
    required this.breakeven,
  });

  final DateTime periodStart;
  final double open;
  final double high;
  final double low;
  final double close;
  final double netPnl;
  final int realizationCount;
  final int wins;
  final int losses;
  final int breakeven;
}
