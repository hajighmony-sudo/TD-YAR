class DailyJournalInsights {
  const DailyJournalInsights({
    required this.entryCount,
    required this.distinctDays,
    required this.currentStreak,
    required this.longestStreak,
    required this.daysWithPreMarketPlan,
    required this.daysWithExecutionReview,
    required this.entriesWithGoals,
    required this.goalCount,
    required this.entriesWithMistakes,
    required this.entriesWithStrengths,
    required this.entriesWithLessons,
    required this.linkedTradeCount,
    required this.tagCounts,
  });

  final int entryCount;
  final int distinctDays;
  final int currentStreak;
  final int longestStreak;
  final int daysWithPreMarketPlan;
  final int daysWithExecutionReview;
  final int entriesWithGoals;
  final int goalCount;
  final int entriesWithMistakes;
  final int entriesWithStrengths;
  final int entriesWithLessons;
  final int linkedTradeCount;
  final Map<String, int> tagCounts;
}
