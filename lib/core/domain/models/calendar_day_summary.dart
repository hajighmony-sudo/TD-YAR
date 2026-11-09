class CalendarDaySummary {
  const CalendarDaySummary({
    required this.date,
    required this.tradeCount,
    required this.wins,
    required this.losses,
    required this.netPnl,
    required this.journalEntryCount,
    required this.psychologyEntryCount,
    this.grossPnl = 0,
    this.fees = 0,
    this.bestTradePnl = 0,
    this.worstTradePnl = 0,
  });

  final DateTime date;
  final int tradeCount;
  final int wins;
  final int losses;
  final double netPnl;
  final double grossPnl;
  final double fees;
  final double bestTradePnl;
  final double worstTradePnl;
  final int journalEntryCount;
  final int psychologyEntryCount;

  int get breakeven => tradeCount - wins - losses;
  double get winRate => tradeCount == 0 ? 0 : wins / tradeCount * 100;
  double get averageTradePnl => tradeCount == 0 ? 0 : netPnl / tradeCount;
}
