import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/journal_entry.dart';
import 'package:td_yar/core/domain/services/daily_journal_analytics.dart';

void main() {
  final base = DateTime(2026, 7, 1);

  JournalEntry entry({
    required String id,
    required DateTime date,
    String plan = '',
    String review = '',
    List<String> tags = const [],
    List<String> trades = const [],
  }) => JournalEntry(
        id: id,
        entryDate: date,
        title: 'Daily review',
        content: 'Notes',
        preMarketPlan: plan,
        executionReview: review,
        tags: tags,
        linkedTradeIds: trades,
        createdAt: date,
        updatedAt: date,
      );

  test('daily journal insights calculate streaks and unique active days', () {
    final entries = [
      entry(
        id: 'j1', date: base, plan: 'Plan A',
        tags: const ['discipline', 'review'], trades: const ['t1'],
        
      ),
      entry(
        id: 'j2', date: base.add(const Duration(hours: 3)),
        review: 'Review A', tags: const ['discipline'], trades: const ['t1', 't2'],
      ),
      entry(
        id: 'j3', date: base.add(const Duration(days: 1)),
        plan: 'Plan B', review: 'Review B', tags: const ['focus'],
      ),
      entry(
        id: 'j4', date: base.add(const Duration(days: 3)),
        tags: const ['review'],
      ),
    ];

    final insights = const DailyJournalAnalytics().analyze(
      entries: entries,
      asOfDate: base.add(const Duration(days: 3)),
    );

    expect(insights.entryCount, 4);
    expect(insights.distinctDays, 3);
    expect(insights.currentStreak, 1);
    expect(insights.longestStreak, 2);
    expect(insights.daysWithPreMarketPlan, 2);
    expect(insights.daysWithExecutionReview, 2);
    expect(insights.linkedTradeCount, 2);
    expect(insights.tagCounts['discipline'], 2);
  });

  test('current streak continues through yesterday when today is empty', () {
    final insights = const DailyJournalAnalytics().analyze(
      entries: [
        entry(id: 'j1', date: base),
        entry(id: 'j2', date: base.add(const Duration(days: 1))),
      ],
      asOfDate: base.add(const Duration(days: 2)),
    );
    expect(insights.currentStreak, 2);
  });
}
