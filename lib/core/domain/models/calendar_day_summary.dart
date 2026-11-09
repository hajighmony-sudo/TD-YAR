class CalendarDaySummary {
  const CalendarDaySummary({
    required this.date,
    required this.tradeCount,
    required this.wins,
    required this.losses,
    required this.netPnl,
    required this.journalEntryCount,
    required this.psychologyEntryCount,
  });

  final DateTime date;
  final int tradeCount;
  final int wins;
  final int losses;
  final double netPnl;
  final int journalEntryCount;
  final int psychologyEntryCount;

  int get breakeven => tradeCount - wins - losses;
}
