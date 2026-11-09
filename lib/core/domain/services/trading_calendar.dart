import '../models/calendar_day_summary.dart';
import '../models/journal_entry.dart';
import '../models/performance_report.dart';
import '../models/psychology_entry.dart';
import '../models/trade_realization.dart';

class TradingCalendar {
  const TradingCalendar();

  List<CalendarDaySummary> buildMonth({
    required int year,
    required int month,
    required List<TradeOutcome> outcomes,
    required List<JournalEntry> journalEntries,
    required List<PsychologyEntry> psychologyEntries,
    List<TradeRealization> realizations = const [],
  }) {
    if (month < 1 || month > 12) {
      throw ArgumentError.value(month, 'month', 'Must be between 1 and 12.');
    }
    return buildRange(
      from: DateTime(year, month, 1),
      to: DateTime(year, month + 1, 0),
      outcomes: outcomes,
      journalEntries: journalEntries,
      psychologyEntries: psychologyEntries,
      realizations: realizations,
    );
  }

  List<CalendarDaySummary> buildRange({
    required DateTime from,
    required DateTime to,
    required List<TradeOutcome> outcomes,
    required List<JournalEntry> journalEntries,
    required List<PsychologyEntry> psychologyEntries,
    List<TradeRealization> realizations = const [],
  }) {
    final start = DateTime(from.year, from.month, from.day);
    final end = DateTime(to.year, to.month, to.day);
    if (end.isBefore(start)) {
      throw ArgumentError('Calendar end date must not precede start date.');
    }
    final summaries = <CalendarDaySummary>[];
    for (var date = start;
        !date.isAfter(end);
        date = date.add(const Duration(days: 1))) {
      final dayRealizations = realizations
          .where((item) => _sameDate(item.closedAt, date))
          .toList(growable: false);
      final dayOutcomes = dayRealizations.isEmpty
          ? outcomes.where((item) => _sameDate(item.closedAt, date)).toList()
          : _aggregateDay(dayRealizations);
      final journalCount = journalEntries
          .where((item) => _sameDate(item.entryDate, date)).length;
      final psychologyCount = psychologyEntries
          .where((item) => _sameDate(item.entryDate, date)).length;
      summaries.add(CalendarDaySummary(
        date: date,
        tradeCount: dayOutcomes.length,
        wins: dayOutcomes.where((item) => item.isWin).length,
        losses: dayOutcomes.where((item) => item.isLoss).length,
        netPnl: dayOutcomes.fold<double>(
          0, (sum, item) => sum + item.netPnl,
        ),
        journalEntryCount: journalCount,
        psychologyEntryCount: psychologyCount,
      ));
    }
    return List.unmodifiable(summaries);
  }

  static List<TradeOutcome> _aggregateDay(List<TradeRealization> values) {
    final grouped = <String, List<TradeRealization>>{};
    for (final item in values) {
      grouped.putIfAbsent(item.tradeId, () => []).add(item);
    }
    return grouped.values.map((items) {
      final last = items.last;
      return TradeOutcome(
        tradeId: last.tradeId,
        symbol: last.symbol,
        direction: last.direction,
        strategyId: last.strategyId,
        closedAt: last.closedAt,
        netPnl: items.fold<double>(0, (sum, item) => sum + item.netPnl),
        grossPnl: items.fold<double>(0, (sum, item) => sum + item.grossPnl),
        fees: items.fold<double>(0, (sum, item) => sum + item.fees),
        closedQuantity:
            items.fold<double>(0, (sum, item) => sum + item.closedQuantity),
        duration: last.duration,
      );
    }).toList(growable: false);
  }

  static bool _sameDate(DateTime first, DateTime second) =>
      first.year == second.year &&
      first.month == second.month &&
      first.day == second.day;
}
