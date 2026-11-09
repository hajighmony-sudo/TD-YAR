class DailyJournalInsights {
  const DailyJournalInsights({
    required this.entryCount,
    required this.distinctDays,
    required this.currentStreak,
    required this.longestStreak,
    required this.daysWithPreMarketPlan,
    required this.daysWithExecutionReview,
    required this.linkedTradeCount,
    required this.tagCounts,
  });

  final int entryCount;
  final int distinctDays;
  final int currentStreak;
  final int longestStreak;
  final int daysWithPreMarketPlan;
  final int daysWithExecutionReview;
  final int linkedTradeCount;
  final Map<String, int> tagCounts;
}
