import '../models/daily_journal_insights.dart';
import '../models/journal_entry.dart';

class DailyJournalAnalytics {
  const DailyJournalAnalytics();

  DailyJournalInsights analyze({
    required List<JournalEntry> entries,
    required DateTime asOfDate,
  }) {
    final dayKeys = <int>{};
    final tagCounts = <String, int>{};
    final linkedTrades = <String>{};
    var daysWithPlan = 0;
    var daysWithReview = 0;

    for (final entry in entries) {
      dayKeys.add(_dateKey(entry.entryDate));
      if (entry.preMarketPlan.trim().isNotEmpty) daysWithPlan++;
      if (entry.executionReview.trim().isNotEmpty) daysWithReview++;
      linkedTrades.addAll(entry.linkedTradeIds);
      for (final tag in entry.tags) {
        final normalized = tag.trim();
        if (normalized.isNotEmpty) {
          tagCounts.update(normalized, (count) => count + 1, ifAbsent: () => 1);
        }
      }
    }

    final sorted = dayKeys.toList()..sort();
    var longest = 0;
    var streak = 0;
    int? previous;
    for (final key in sorted) {
      final date = _fromDateKey(key);
      final current = DateTime.utc(date.year, date.month, date.day);
      if (previous != null) {
        final prior = _fromDateKey(previous);
        final expected = DateTime.utc(prior.year, prior.month, prior.day + 1);
        streak = current == expected ? streak + 1 : 1;
      } else {
        streak = 1;
      }
      if (streak > longest) longest = streak;
      previous = key;
    }

    final today = DateTime(asOfDate.year, asOfDate.month, asOfDate.day);
    final todayKey = _dateKey(today);
    final yesterday = today.subtract(const Duration(days: 1));
    final anchorKey = dayKeys.contains(todayKey)
        ? todayKey
        : _dateKey(yesterday);
    var currentStreak = 0;
    if (dayKeys.contains(anchorKey)) {
      var cursor = _fromDateKey(anchorKey);
      while (dayKeys.contains(_dateKey(cursor))) {
        currentStreak++;
        cursor = cursor.subtract(const Duration(days: 1));
      }
    }

    final orderedTags = tagCounts.entries.toList()
      ..sort((a, b) {
        final byCount = b.value.compareTo(a.value);
        return byCount != 0 ? byCount : a.key.compareTo(b.key);
      });

    return DailyJournalInsights(
      entryCount: entries.length,
      distinctDays: dayKeys.length,
      currentStreak: currentStreak,
      longestStreak: longest,
      daysWithPreMarketPlan: daysWithPlan,
      daysWithExecutionReview: daysWithReview,
      linkedTradeCount: linkedTrades.length,
      tagCounts: Map.unmodifiable({
        for (final entry in orderedTags) entry.key: entry.value,
      }),
    );
  }

  static int _dateKey(DateTime date) =>
      date.year * 10000 + date.month * 100 + date.day;

  static DateTime _fromDateKey(int key) => DateTime(
        key ~/ 10000,
        (key ~/ 100) % 100,
        key % 100,
      );
}
