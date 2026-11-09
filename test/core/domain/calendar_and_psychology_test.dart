import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/journal_entry.dart';
import 'package:td_yar/core/domain/models/performance_report.dart';
import 'package:td_yar/core/domain/models/psychology_entry.dart';
import 'package:td_yar/core/domain/services/psychology_analytics.dart';
import 'package:td_yar/core/domain/services/trading_calendar.dart';
import 'package:td_yar/core/domain/models/trade.dart';

void main() {
  final day = DateTime(2026, 3, 10, 12);

  test('psychology analytics summarizes self-reported behavior', () {
    final entries = [
      PsychologyEntry(
        id: 'p1',
        entryDate: day,
        mood: 'calm',
        confidence: 4,
        focus: 5,
        stress: 2,
        followedPlan: true,
        isFomo: false,
        isRevengeTrade: false,
        createdAt: day,
        updatedAt: day,
        tradeId: 't1',
      ),
      PsychologyEntry(
        id: 'p2',
        entryDate: day,
        mood: 'anxious',
        confidence: 2,
        focus: 2,
        stress: 4,
        followedPlan: false,
        isFomo: true,
        isRevengeTrade: true,
        createdAt: day,
        updatedAt: day,
        tradeId: 't2',
      ),
    ];
    final outcomes = [
      TradeOutcome(
        tradeId: 't1',
        symbol: 'EURUSD',
        direction: TradeDirection.long,
        strategyId: null,
        closedAt: day,
        netPnl: 12,
        grossPnl: 12,
        fees: 0,
        closedQuantity: 1,
        duration: Duration(hours: 1),
      ),
      TradeOutcome(
        tradeId: 't2',
        symbol: 'GBPUSD',
        direction: TradeDirection.short,
        strategyId: null,
        closedAt: day,
        netPnl: -5,
        grossPnl: -5,
        fees: 0,
        closedQuantity: 1,
        duration: Duration(minutes: 30),
      ),
    ];

    final insights = const PsychologyAnalytics().analyze(
      entries: entries,
      outcomes: outcomes,
    );
    expect(insights.entryCount, 2);
    expect(insights.averageConfidence, 3);
    expect(insights.planAdherencePercent, 50);
    expect(insights.fomoCount, 1);
    expect(insights.revengeTradeCount, 1);
    expect(insights.linkedTradeEntryCount, 2);
    expect(insights.moodCounts['calm'], 1);
    expect(insights.averagePnlWhenFomo, -5);
    expect(insights.averagePnlWithoutFomo, 12);
    expect(insights.averagePnlWhenPlanFollowed, 12);
    expect(insights.averagePnlWhenPlanNotFollowed, -5);
  });

  test('calendar includes every date and aggregates trade and journal counts', () {
    final outcomes = [
      TradeOutcome(
        tradeId: 't1',
        symbol: 'EURUSD',
        direction: TradeDirection.long,
        strategyId: null,
        closedAt: DateTime(2026, 3, 10, 15),
        netPnl: 8,
        grossPnl: 9,
        fees: 1,
        closedQuantity: 1,
        duration: const Duration(minutes: 20),
      ),
    ];
    final journals = [
      JournalEntry(
        id: 'j1',
        entryDate: day,
        title: 'Review',
        content: 'Notes',
        createdAt: day,
        updatedAt: day,
      ),
    ];
    final days = const TradingCalendar().buildMonth(
      year: 2026,
      month: 3,
      outcomes: outcomes,
      journalEntries: journals,
      psychologyEntries: const [],
    );
    final selected = days.firstWhere((item) => item.date.day == 10);
    expect(days.length, 31);
    expect(selected.tradeCount, 1);
    expect(selected.wins, 1);
    expect(selected.netPnl, 8);
    expect(selected.journalEntryCount, 1);
  });
}
